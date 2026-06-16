package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.math.ln
import kotlin.random.Random

class ViterbiModel(private val context: Context, private val order: Int = 3) {

    private var db: SQLiteDatabase? = null
    private var posDb: SQLiteDatabase? = null

    private val beamWidth = 8
    private val minWords = 6
    private val maxWords = 12
    private val endBoost = 2.0

    // Stores every complete sentence generated this session (exact match guard)
    private val generatedSentences = mutableSetOf<String>()

    // Stores bigram + trigram fingerprints from recent sentences
    // Key = ngram string, Value = how many recent sentences contained it
    private val ngramFingerprints = mutableMapOf<String, Int>()

    // How many sentences have been generated so far (used to widen candidate pool)
    private var generationCount = 0

    // Rotating temperature — changes each call to alter randomness profile
    private val temperaturePool = listOf(0.90, 1.00, 1.10, 1.20, 1.35, 1.50)
    private var temperatureIndex = 0

    // Max retries when a duplicate or near-duplicate is produced
    private val maxRetries = 6

    private val sentencePatterns = listOf(
        listOf("PRON", "VERB", "NOUN"),
        listOf("NOUN", "VERB", "NOUN"),
        listOf("PRON", "VERB"),
        listOf("NOUN", "VERB"),
        listOf("VERB", "NOUN")
    )

    private val connectors = setOf("at", "kaya", "dahil", "para", "ngunit")
    private val sentenceEnders = setOf("ako", "siya", "kami", "sila", "ito", "iyan")

    private enum class Slot {
        SUBJECT, VERB, OBJECT, MODIFIER, END
    }

    data class Beam(
        val words: List<String>,
        val score: Double,
        val context: List<String>
    )

    // REPETITION CONTROL

    private fun repetitionPenalty(word: String, beam: Beam): Double {
        val recent = beam.words.takeLast(5)
        val count = recent.count { it == word }
        return when {
            count >= 2 -> -1.2
            count == 1 -> -0.25
            else -> 0.0
        }
    }

    // SESSION-LEVEL NGRAM FINGERPRINT

    private fun extractNgrams(words: List<String>): Set<String> {
        val result = mutableSetOf<String>()
        for (i in words.indices) {
            if (i + 1 < words.size) result.add("${words[i]}_${words[i+1]}")          // bigram
            if (i + 2 < words.size) result.add("${words[i]}_${words[i+1]}_${words[i+2]}") // trigram
        }
        return result
    }

    private fun ngramOverlapScore(candidateWords: List<String>): Double {
        if (ngramFingerprints.isEmpty() || candidateWords.isEmpty()) return 0.0
        val candidateNgrams = extractNgrams(candidateWords)
        if (candidateNgrams.isEmpty()) return 0.0

        val overlapCount = candidateNgrams.count { ngram ->
            (ngramFingerprints[ngram] ?: 0) > 0
        }
        return overlapCount.toDouble() / candidateNgrams.size
    }

    private fun recordSentence(words: List<String>) {
        // Decay existing fingerprints slightly
        val decayKeys = ngramFingerprints.keys.toList()
        for (key in decayKeys) {
            val newVal = (ngramFingerprints[key] ?: 0) - 1
            if (newVal <= 0) ngramFingerprints.remove(key)
            else ngramFingerprints[key] = newVal
        }

        // Add new fingerprints with weight 3
        val ngrams = extractNgrams(words)
        for (ngram in ngrams) {
            ngramFingerprints[ngram] = (ngramFingerprints[ngram] ?: 0) + 3
        }
    }

    // DIVERSITY PENALTY

    private fun diversityPenalty(candidateWords: List<String>): Double {
        val overlap = ngramOverlapScore(candidateWords)
        return when {
            overlap > 0.70 -> -2.5
            overlap > 0.50 -> -1.2
            overlap > 0.35 -> -0.5
            overlap > 0.20 -> -0.2
            else -> 0.0
        }
    }

    // CONTEXT SHIFT

    private fun buildStartContext(random: Random): List<String> {
        // Most of the time use the standard <START> context
        if (random.nextFloat() < 0.65f || generationCount < 3) {
            return List(order - 1) { "<START>" }
        }
        // Otherwise pick a random connector or subject word as a "warm start"
        val warmStarters = listOf("<START>", "ang", "si", "mga", "sa", "ng")
        return List(order - 1) { warmStarters.random(random) }
    }

    // DYNAMIC TEMPERATURE
    // POS HELPERS

    private fun normalize(word: String): String {
        return word.lowercase().replace(Regex("[^\\p{L}\\p{N}]"), "")
    }

    private fun getPos(word: String): String? {
        val clean = normalize(word)
        if (clean.isEmpty()) return null
        return try {
            posDb?.rawQuery(
                "SELECT pos FROM word_pos WHERE word=? LIMIT 1",
                arrayOf(clean)
            )?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun posScore(prev: String?, cur: String?): Double {
        if (prev == null || cur == null) return 0.0
        return when (prev) {
            "VERB" -> if (cur == "NOUN") 0.1 else -0.03
            "NOUN" -> if (cur == "VERB") 0.08 else 0.0
            else -> 0.0
        }
    }

    private fun semanticBoost(word: String, slot: Slot): Double {
        val pos = getPos(word)
        return when (slot) {
            Slot.SUBJECT  -> if (pos == "PRON" || pos == "NOUN") 0.3 else 0.05
            Slot.VERB     -> if (pos == "VERB") 0.5 else -0.15
            Slot.OBJECT   -> if (pos == "NOUN") 0.4 else -0.1
            Slot.MODIFIER -> 0.05
            Slot.END      -> 0.0
        }
    }

    private fun slotFilter(slot: Slot, candidates: List<Pair<String, Double>>): List<Pair<String, Double>> {
        val filtered = when (slot) {
            Slot.SUBJECT  -> candidates.filter { getPos(it.first) in setOf("PRON", "NOUN") }
            Slot.VERB     -> candidates.filter { getPos(it.first) == "VERB" }
            Slot.OBJECT   -> candidates.filter { getPos(it.first) == "NOUN" }
            Slot.MODIFIER -> candidates
            Slot.END      -> candidates
        }
        return if (filtered.isNotEmpty()) filtered else candidates
    }

    // INITIALIZE

    fun initialize() {
        val dbName = "filipino_${order}gram.db"
        val dbFile = copyFromAssets("ngram_database/$dbName", dbName)
        db = SQLiteDatabase.openDatabase(
            dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )
        val posFile = copyFromAssets("ngram_database/filipino_pos.db", "filipino_pos.db")
        posDb = SQLiteDatabase.openDatabase(
            posFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )
    }

    // PUBLIC ENTRY POINT

    fun generateViterbi(seed: Long? = null): String {
        val baseSeed = seed ?: System.currentTimeMillis()

        repeat(maxRetries) { attempt ->
            // Vary the seed each retry so we get genuinely different beam paths
            val trySeed = baseSeed + attempt * 1_000_003L

            val candidate = generateOnce(trySeed, temperatureOverrideIndex = attempt)

            if (candidate.isEmpty()) return@repeat

            val wordKey = stripToWordKey(candidate)

            if (wordKey.isEmpty()) return@repeat

            if (wordKey in generatedSentences) return@repeat

            val words = wordKey.split(" ")
            val overlap = ngramOverlapScore(words)
            if (overlap > 0.75 && generatedSentences.size > 5) return@repeat

            // Passed all checks advance temperature, record, return
            temperatureIndex++
            generatedSentences.add(wordKey)
            recordSentence(words)
            generationCount++
            return candidate
        }

        temperatureIndex += 4
        val fallback = generateOnce(baseSeed + maxRetries * 7_777_777L)
        val wordKey = stripToWordKey(fallback)
        val words = wordKey.split(" ").filter { it.isNotBlank() }
        generatedSentences.add(wordKey)
        recordSentence(words)
        generationCount++
        return fallback
    }

    private fun stripToWordKey(sentence: String): String {
        return sentence
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // SINGLE GENERATION PASS

    private fun generateOnce(seed: Long, temperatureOverrideIndex: Int = -1): String {
        val random = Random(seed)

        val temperature = if (temperatureOverrideIndex >= 0) {
            temperaturePool[(temperatureIndex + temperatureOverrideIndex) % temperaturePool.size]
        } else {
            temperaturePool[temperatureIndex % temperaturePool.size]
        }

        var beams = listOf(
            Beam(emptyList(), 0.0, buildStartContext(random))
        )

        val finished = mutableListOf<Beam>()

        val slots = listOf(
            Slot.SUBJECT, Slot.VERB, Slot.OBJECT, Slot.MODIFIER, Slot.END
        )

        // Dynamic candidate pool size — widens as session matures
        val poolSize = (10 + (generationCount / 5).coerceAtMost(20))
        val pickSize = (5 + (generationCount / 10).coerceAtMost(10))

        repeat(maxWords) {

            val newBeams = mutableListOf<Beam>()

            for (beam in beams) {

                val candidates = getCandidates(beam.context, temperature) ?: continue
                val slot = slots.getOrElse(beam.words.size) { Slot.END }

                val filtered = slotFilter(slot, candidates)
                    .take(poolSize)
                    .shuffled(random)
                    .take(pickSize)

                val prevWord = beam.words.lastOrNull()
                val prevPos  = prevWord?.let { getPos(it) }

                for ((word, logProb) in filtered) {

                    var score = beam.score + logProb

                    val curPos = getPos(word)

                    score += posScore(prevPos, curPos)
                    score += semanticBoost(word, slot)
                    score += repetitionPenalty(word, beam)

                    // Cross-sentence diversity penalty (applied to partial beam)
                    val partialWords = if (word == "<END>") beam.words else beam.words + word
                    score += diversityPenalty(partialWords)

                    if (word == "<END>") score += endBoost

                    val newWords =
                        if (word == "<END>") beam.words else beam.words + word

                    val newContext =
                        (beam.context + word).takeLast(order - 1)

                    val newBeam = Beam(newWords, score, newContext)

                    if (word == "<END>") {
                        if (beam.words.size >= minWords) {
                            finished.add(newBeam)
                        }
                    } else {
                        newBeams.add(newBeam)
                    }
                }
            }

            beams = if (newBeams.isNotEmpty()) {
                newBeams
                    .sortedByDescending { it.score }
                    .distinctBy { it.words.takeLast(3) }
                    .take(beamWidth)
            } else {
                beams
            }
        }

        val best = (finished + beams)
            .maxByOrNull { it.score }
            ?.words
            ?: beams.firstOrNull()?.words
            ?: emptyList()

        return format(cleanEnding(best))
    }

    // CANDIDATES

    private fun getCandidates(context: List<String>, temperature: Double): List<Pair<String, Double>>? {
        val results = query(context) ?: return null
        val total = results.sumOf { it.second }.toDouble()
        if (total <= 0.0) return null

        return results.map { (word, freq) ->
            val score = ln((freq / total) + 1e-10) / temperature
            word to score
        }.sortedByDescending { it.second }
    }

    // Legacy overload used by nothing now, kept for safety
    private fun getCandidates(context: List<String>): List<Pair<String, Double>>? =
        getCandidates(context, 1.15)

    // QUERY

    private fun query(context: List<String>): List<Pair<String, Int>>? {
        val q = when (context.size) {
            3 -> "SELECT word4, frequency FROM ngrams WHERE word1=? AND word2=? AND word3=?"
            2 -> "SELECT word3, frequency FROM ngrams WHERE word1=? AND word2=?"
            1 -> "SELECT word2, frequency FROM ngrams WHERE word1=?"
            else -> return null
        }
        return try {
            db?.rawQuery(q, context.toTypedArray())?.use { c ->
                if (!c.moveToFirst()) return null
                val out = mutableListOf<Pair<String, Int>>()
                do { out.add(c.getString(0) to c.getInt(1)) } while (c.moveToNext())
                out.ifEmpty { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    // CLEAN OUTPUT

    private fun cleanEnding(words: List<String>): List<String> {
        val badEndings = setOf("na", "ng", "sa", "at", "ang", "pa")
        val cleaned = words.toMutableList()
        while (cleaned.size > 3 && normalize(cleaned.last()) in badEndings) {
            cleaned.removeAt(cleaned.lastIndex)
        }
        return cleaned
    }

    private fun format(words: List<String>): String {
        val raw = words
            .filter { it != "<START>" && it != "<END>" }
            .joinToString(" ")
            .trim()

        if (raw.isEmpty()) return ""

        val cleaned = StringBuilder()
        var i = 0
        while (i < raw.length) {
            val c = raw[i]
            when (c) {
                '.', '!', '?' -> {
                    while (cleaned.isNotEmpty() && cleaned.last() == ' ') {
                        cleaned.setLength(cleaned.length - 1)
                    }
                    if (cleaned.isNotEmpty() && cleaned.last() !in ".!?") {
                        cleaned.append(c)
                    }
                }
                ',' -> {
                    if (i != raw.lastIndex) cleaned.append(',')
                }
                else -> cleaned.append(c)
            }
            i++
        }

        var result = cleaned.toString()
        result = result.replace(Regex("\\s+"), " ")
        result = result.replace(Regex("\\s+([,.!?])"), "$1")
        result = result.trim().trimEnd(',', '.', '!', '?')

        val mode = Random.nextFloat()
        if (result.isNotEmpty()) {
            result = when {
                mode < 0.70 -> "$result."
                mode < 0.85 -> "$result!"
                mode < 0.95 -> "$result?"
                else        -> result
            }
        }

        // Capitalize first character
        return result.replaceFirstChar { it.uppercaseChar() }
    }

    private fun copyFromAssets(assetPath: String, fileName: String): File {
        val file = context.getDatabasePath(fileName)
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return file
    }

    fun close() {
        db?.close()
        posDb?.close()
    }

    // RESET SESSION

    fun resetSession() {
        generatedSentences.clear()
        ngramFingerprints.clear()
        generationCount = 0
        temperatureIndex = 0
    }
}