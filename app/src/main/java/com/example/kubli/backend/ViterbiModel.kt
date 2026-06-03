package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.math.abs
import kotlin.math.ln
import kotlin.random.Random

// This class is responsible for making the visible cover sentence.
// It uses an n-gram database to guess the next possible words, then uses a
// Viterbi-style beam search to keep the best sentence choices.
// After generating, it checks the sentence again so the output will not start
// or end in an awkward way.
class ViterbiModel(private val context: Context, private val order: Int = 3) {

    // These are the databases used by the model.
    // trigramDb checks two previous words.
    // quadgramDb checks three previous words.
    // posDb contains the pos tags, like NOUN, VERB, and PRON.
    private var trigramDb: SQLiteDatabase? = null
    private var quadgramDb: SQLiteDatabase? = null
    private var posDb: SQLiteDatabase? = null

    // These values control the general sentence generation limits.
    // beamWidth means how many possible sentence paths are kept at each step.
    // minWords and maxWords are soft limits for sentence length.
    // endBoost gives lets the model find a possible ending.
    // contextSize is kept at 3 so the model can use quadgram first,
    // even when the rest of the app still creates ViterbiModel(context, 3).
    private val contextSize = maxOf(order - 1, 3)
    private val beamWidth = 10
    private val minWords = 5
    private val maxWords = 16
    private val endBoost = 1.35

    // This keeps the full sentences already made, so the app will not repeat them.
    private val generatedSentences = mutableSetOf<String>()

    // This keeps short word patterns from recent sentences.
    // It helps the model avoid making sentences that sound too similar.
    private val ngramFingerprints = mutableMapOf<String, Int>()

    // This counts how many sentences were already generated in this session.
    private var generationCount = 0

    // These values control how random the word choices can be.
    private val temperaturePool = listOf(0.90, 1.00, 1.10, 1.20, 1.35, 1.50)
    private var temperatureIndex = 0

    // This is how many times the model will try again if the sentence is not good.
    private val maxRetries = 8

    // These are simple sentence shapes used as a reference for the kind of output wanted.
    // PRON means pronoun, VERB means action word, and NOUN means object/person/place.
    private val sentencePatterns = listOf(
        listOf("PRON", "VERB", "NOUN"),
        listOf("NOUN", "VERB", "NOUN"),
        listOf("PRON", "VERB"),
        listOf("NOUN", "VERB"),
        listOf("VERB", "NOUN")
    )

    // These lists help the model judge if a word is good for a certain position.
    // connectors are words that connect ideas.
    // sentenceEnders are words that usually sound okay at the end of a sentence.
    // weakStarters are words that usually sound strange as the first word.
    private val connectors = setOf("at", "kaya", "dahil", "para", "ngunit", "habang", "kung")
    private val sentenceEnders = setOf("ako", "siya", "kami", "sila", "ito", "iyan", "natin", "nila", "kanila")
    private val weakStarters = setOf("ng", "sa", "na", "pa", "at", "ay", "para", "dahil", "kaya", "ngunit")

    // These are words that are usually okay to use at the start of a sentence.
    private val naturalStarters = setOf(
        "ako", "ikaw", "siya", "kami", "tayo", "sila", "ang", "si", "sina", "mga",
        "ito", "iyan", "may", "noong", "ngayon", "kapag", "kung", "habang", "pero"
    )

    // These words make a sentence feel unfinished if they appear at the end.
    // For example, ending with "ng" or "para" sounds cut off.
    private val danglingEndings = setOf(
        "ang", "ng", "sa", "na", "pa", "po", "opo", "at", "ay", "para", "dahil",
        "kaya", "ngunit", "habang", "kung", "yung"
    )

    // These words are blocked because they caused awkward or unrelated sentences before.
    // This is a practical filter to avoid outputs that look unnatural.
    private val noisyWords = setOf(
        "inear", "monitors", "hangtag", "kimstore", "pup", "atrocities", "martial",
        "wy", "barker", "mapipintasan", "magkakapitbahay", "bise", "presidente",
        "sandata", "demokrasya"
    )

    // These are exact sentence patterns that should not be generated again.
    private val blockedSentences = setOf(
        "ako yung inear monitors ko kinain ng bise presidente",
        "ako yung hangtag kaya pala",
        "kapag may laban ang kimstore rito",
        "pup na talagang mapipintasan ka",
        "kabataan sa atrocities ng martial law",
        "puso upang palaging mapagusapan ang magkakapitbahay",
        "pero kung malambot ito wy lang",
        "lalo yung barker bukas pag nakita mo ko",
        "lola kamakailan bumalik ang demokrasya",
        "tao noon tapos nagdala ng sandata"
    )

    // A slot is the role that the next word should try to fill.
    // This gives the sentence a basic structure instead of choosing words blindly.
    private enum class Slot {
        SUBJECT, VERB, OBJECT, MODIFIER, END
    }

    // A Beam is one possible sentence path.
    // words are the words chosen so far.
    // score tells how good this path is.
    // context is the recent words used to search for the next word in the database.
    data class Beam(
        val words: List<String>,
        val score: Double,
        val context: List<String>
    )

    // This gives a penalty when the same word appears too often.
    // It helps avoid repeated words or phrases.

    private fun repetitionPenalty(word: String, beam: Beam): Double {
        val cleanWord = normalize(word)
        val recent = beam.words.takeLast(6).map { normalize(it) }
        val count = recent.count { it == cleanWord }
        return when {
            count >= 2 -> -2.0
            count == 1 -> -0.55
            else -> 0.0
        }
    }

    // This makes two word and three word patterns from a sentence.
    // The model uses these patterns to notice if new sentences are too similar.

    private fun extractNgrams(words: List<String>): Set<String> {
        val result = mutableSetOf<String>()
        for (i in words.indices) {
            if (i + 1 < words.size) result.add("${words[i]}_${words[i+1]}")          // two-word pattern
            if (i + 2 < words.size) result.add("${words[i]}_${words[i+1]}_${words[i+2]}") // three-word pattern
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
        // Lower the old pattern scores so newer sentences matter more.
        val decayKeys = ngramFingerprints.keys.toList()
        for (key in decayKeys) {
            val newVal = (ngramFingerprints[key] ?: 0) - 1
            if (newVal <= 0) ngramFingerprints.remove(key)
            else ngramFingerprints[key] = newVal
        }

        // Add the new sentence patterns to the recent pattern list.
        val ngrams = extractNgrams(words)
        for (ngram in ngrams) {
            ngramFingerprints[ngram] = (ngramFingerprints[ngram] ?: 0) + 3
        }
    }

    // This gives a lower score if the sentence is too similar to recent ones.
    // A lower score means the beam search is less likely to choose it.

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

    // This chooses the starting context for the sentence.

    private fun buildStartContext(random: Random): List<String> {
        // Most of the time, begin from the normal start marker.
        if (random.nextFloat() < 0.65f || generationCount < 3) {
            return List(contextSize) { "<START>" }
        }
        // Sometimes start with a common opening word to create more variation.
        val warmStarters = listOf("<START>", "ang", "si", "mga", "ako", "kami", "siya")
        return List(contextSize) { warmStarters.random(random) }
    }

    // These helper functions clean words and check their part of speech.

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
        val clean = normalize(word)
        return when (slot) {
            Slot.SUBJECT  -> if (pos == "PRON" || pos == "NOUN") 0.45 else -0.15
            Slot.VERB     -> if (pos == "VERB") 0.65 else -0.25
            Slot.OBJECT   -> if (pos == "NOUN") 0.50 else -0.15
            Slot.MODIFIER -> if (clean in connectors) 0.18 else 0.05
            Slot.END      -> if (clean in sentenceEnders) 0.25 else 0.0
        }
    }

    private fun slotFilter(slot: Slot, candidates: List<Pair<String, Double>>): List<Pair<String, Double>> {
        val cleanCandidates = candidates.filterNot { isNoisyCandidate(it.first) }
            .ifEmpty { candidates }

        val filtered = when (slot) {
            Slot.SUBJECT  -> cleanCandidates.filter { getPos(it.first) in setOf("PRON", "NOUN") }
            Slot.VERB     -> cleanCandidates.filter { getPos(it.first) == "VERB" }
            Slot.OBJECT   -> cleanCandidates.filter { getPos(it.first) == "NOUN" }
            Slot.MODIFIER -> cleanCandidates
            Slot.END      -> cleanCandidates
        }
        return if (filtered.isNotEmpty()) filtered else cleanCandidates
    }

    // This must be called before generating text.
    // It copies the database files from the app assets if needed, then opens them.

    fun initialize() {
        trigramDb = openNgramDatabase("filipino_3gram.db")
        quadgramDb = openNgramDatabase("filipino_4gram.db", required = false)

        val posFile = copyFromAssets("ngram_database/filipino_pos.db", "filipino_pos.db")
        posDb = SQLiteDatabase.openDatabase(
            posFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY
        )
    }

    // This is the main function used by the rest of the app.
    // It tries to generate a sentence, checks if it is acceptable, and retries if needed.
    // If all strict tries fail, it returns the best almost-acceptable sentence it found.

    fun generateViterbi(seed: Long? = null): String {
        val baseSeed = seed ?: System.currentTimeMillis()
        var bestCandidate = ""
        var bestCandidateScore = Double.NEGATIVE_INFINITY

        repeat(maxRetries) { attempt ->
            // Change the seed each try so the generated sentence will be different.
            val trySeed = baseSeed + attempt * 1_000_003L

            val candidate = generateOnce(trySeed, temperatureOverrideIndex = attempt)

            if (candidate.isEmpty()) return@repeat

            val wordKey = stripToWordKey(candidate)

            if (wordKey.isEmpty()) return@repeat

            if (isBlockedSentence(wordKey)) return@repeat

            if (wordKey in generatedSentences) return@repeat

            val words = wordKey.split(" ")
            val candidateScore = finalQualityScore(words)

            // Save this as a backup if it is not perfect but still usable.
            if (candidateScore > bestCandidateScore && isNearAcceptableSentence(words)) {
                bestCandidate = candidate
                bestCandidateScore = candidateScore
            }

            if (!isAcceptableSentence(words)) return@repeat

            val overlap = ngramOverlapScore(words)
            if (overlap > 0.75 && generatedSentences.size > 5) return@repeat

            // If the sentence passed the checks, save it and return it.
            temperatureIndex++
            generatedSentences.add(wordKey)
            recordSentence(words)
            generationCount++
            return candidate
        }

        if (bestCandidate.isNotBlank()) {
            // Use the best backup sentence if no sentence passed all strict checks.
            val wordKey = stripToWordKey(bestCandidate)
            val words = wordKey.split(" ").filter { it.isNotBlank() }
            generatedSentences.add(wordKey)
            recordSentence(words)
            generationCount++
            temperatureIndex += 2
            return bestCandidate
        }

        temperatureIndex += 4

        // Last fallback. This is only used if the model cannot find any good backup.
        val fallback = generateOnce(baseSeed + maxRetries * 7_777_777L)
        val wordKey = stripToWordKey(fallback)
        val words = wordKey.split(" ").filter { it.isNotBlank() }
        if (wordKey.isNotEmpty()) {
            generatedSentences.add(wordKey)
            recordSentence(words)
        }
        generationCount++
        return fallback
    }

    private fun stripToWordKey(sentence: String): String {
        // This turns a sentence into a simple lowercase key.
        // It removes punctuation so comparison is easier.
        return sentence
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    // This makes one possible sentence using beam search.
    // Beam search means the model keeps several good sentence paths at once,
    // instead of committing to only one word choice right away.

    private fun generateOnce(seed: Long, temperatureOverrideIndex: Int = -1): String {
        val random = Random(seed)

        val temperature = if (temperatureOverrideIndex >= 0) {
            temperaturePool[(temperatureIndex + temperatureOverrideIndex) % temperaturePool.size]
        } else {
            temperaturePool[temperatureIndex % temperaturePool.size]
        }

        val targetWords = random.nextInt(minWords + 1, maxWords + 1)

        // Start with one empty sentence path.
        var beams = listOf(
            Beam(emptyList(), 0.0, buildStartContext(random))
        )

        // Finished beams are sentence paths that already reached <END>.
        val finished = mutableListOf<Beam>()

        // The slots guide the first few words toward a simple sentence structure.
        val slots = listOf(
            Slot.SUBJECT, Slot.VERB, Slot.OBJECT, Slot.MODIFIER, Slot.END
        )

        // Let the model consider more words as more sentences are generated.
        val poolSize = (16 + (generationCount / 5).coerceAtMost(18))
        val pickSize = (6 + (generationCount / 10).coerceAtMost(10))

        repeat(maxWords) {

            val newBeams = mutableListOf<Beam>()

            for (beam in beams) {

                // Get possible next words based on the recent words in this beam.
                val candidates = getCandidates(beam.context, temperature) ?: continue
                val slot = slots.getOrElse(beam.words.size) { Slot.END }

                // Filter bad words, shuffle for variation, then keep only a small group.
                val filtered = slotFilter(slot, candidates)
                    .take(poolSize)
                    .shuffled(random)
                    .take(pickSize)

                val prevWord = beam.words.lastOrNull()
                val prevPos  = prevWord?.let { getPos(it) }

                for ((word, logProb) in filtered) {

                    // Skip words that are known to make strange sentences.
                    if (word != "<END>" && isNoisyCandidate(word)) continue

                    // Start the score with the probability from the n-gram database.
                    var score = beam.score + logProb

                    val curPos = getPos(word)

                    // Add extra points or penalties based on grammar, meaning, and repetition.
                    score += posScore(prevPos, curPos)
                    score += semanticBoost(word, slot)
                    score += repetitionPenalty(word, beam)

                    // Lower the score if this partial sentence is becoming repetitive.
                    val partialWords = if (word == "<END>") beam.words else beam.words + word
                    score += diversityPenalty(partialWords)
                    score += partialQualityScore(partialWords)
                    score -= abs(partialWords.size - targetWords) * 0.04

                    if (word == "<END>") {
                        // Ending is only rewarded if the sentence looks complete.
                        score += endBoost + endingScore(beam.words)
                        score -= abs(beam.words.size - targetWords) * 0.18
                    }

                    val newWords =
                        if (word == "<END>") beam.words else beam.words + word

                    val newContext =
                        (beam.context + word).takeLast(contextSize)

                    val newBeam = Beam(newWords, score, newContext)

                    if (word == "<END>") {
                        // Only accept an ending if the sentence already has enough words.
                        if (beam.words.size >= minWords) {
                            finished.add(newBeam)
                        }
                    } else {
                        newBeams.add(newBeam)
                    }
                }
            }

            beams = if (newBeams.isNotEmpty()) {
                // Keep only the best paths so the search does not become too large.
                newBeams
                    .sortedByDescending { it.score }
                    .distinctBy { it.words.takeLast(3) }
                    .take(beamWidth)
            } else {
                beams
            }
        }

        // Prefer sentences that actually reached <END> and passed the quality checks.
        val acceptableFinished = finished.filter { isAcceptableWordList(cleanEnding(it.words)) }
        val acceptableAny = (finished + beams).filter { isAcceptableWordList(cleanEnding(it.words)) }
        val selectionPool = when {
            acceptableFinished.isNotEmpty() -> acceptableFinished
            acceptableAny.isNotEmpty() -> acceptableAny
            finished.isNotEmpty() -> finished
            else -> beams
        }

        // Pick the best remaining sentence path.
        val best = selectionPool
            .maxByOrNull { it.score + finalQualityScore(it.words) }
            ?.words
            ?: beams.firstOrNull()?.words
            ?: emptyList()

        return format(cleanEnding(best))
    }

    // This gets possible next words for the current context.
    // It also turns frequency counts into scores that beam search can compare.

    private fun getCandidates(context: List<String>, temperature: Double): List<Pair<String, Double>>? {
        val results = query(context) ?: return null
        val total = results.sumOf { it.second }.toDouble()
        if (total <= 0.0) return null

        return results.map { (word, freq) ->
            val score = ln((freq / total) + 1e-10) / temperature
            word to score
        }.sortedByDescending { it.second }
    }

    // Backup version kept in case another part of the code needs it later.
    private fun getCandidates(context: List<String>): List<Pair<String, Double>>? =
        getCandidates(context, 1.15)

    // This reads matching word patterns from the n-gram databases.
    // It tries quadgram first because it has more context.
    // It also uses trigram as a backup so the model still has choices.

    private fun query(context: List<String>): List<Pair<String, Int>>? {
        val combined = mutableMapOf<String, Int>()
        val lastThree = context.takeLast(3)
        val lastTwo = context.takeLast(2)

        if (lastThree.size == 3) {
            queryDatabase(
                db = quadgramDb,
                sql = "SELECT word4, frequency FROM ngrams WHERE word1=? AND word2=? AND word3=?",
                args = lastThree,
                weight = 3,
                out = combined
            )
        }

        if (lastTwo.size == 2) {
            queryDatabase(
                db = trigramDb,
                sql = "SELECT word3, frequency FROM ngrams WHERE word1=? AND word2=?",
                args = lastTwo,
                weight = 1,
                out = combined
            )
        }

        return combined
            .map { (word, weightedFrequency) -> word to weightedFrequency }
            .sortedByDescending { it.second }
            .ifEmpty { null }
    }

    private fun queryDatabase(
        db: SQLiteDatabase?,
        sql: String,
        args: List<String>,
        weight: Int,
        out: MutableMap<String, Int>
    ) {
        try {
            db?.rawQuery(sql, args.toTypedArray())?.use { c ->
                if (!c.moveToFirst()) return
                do {
                    val word = c.getString(0)
                    val frequency = c.getInt(1) * weight
                    out[word] = (out[word] ?: 0) + frequency
                } while (c.moveToNext())
            }
        } catch (e: Exception) {
            // If one database lookup fails, just continue with the other one.
        }
    }

    // These functions check if a sentence looks readable enough.
    // They do not make the sentence perfect, but they remove the most obvious bad outputs.

    private fun isNoisyCandidate(word: String): Boolean {
        // A noisy word is a word that usually makes the output look strange.
        val clean = normalize(word)
        if (word.any { it == '`' || it == '"' || it == '\'' }) return true
        return clean in noisyWords
    }

    private fun isBlockedSentence(wordKey: String): Boolean =
        // This checks if the generated sentence matches one of the known bad examples.
        wordKey in blockedSentences || blockedSentences.any { blocked ->
            wordKey.contains(blocked)
        }

    private fun isAcceptableSentence(words: List<String>): Boolean {
        // This is the strict final check before a sentence can be returned.
        val cleanWords = words.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleanWords.size < 4) return false
        if (!hasGoodStart(cleanWords)) return false
        if (!hasCompleteEnding(cleanWords)) return false
        if (cleanWords.any { it in noisyWords }) return false
        if (hasAdjacentRepeat(cleanWords)) return false
        if (hasRepeatedNgram(cleanWords, 2)) return false

        val posTags = cleanWords.mapNotNull { getPos(it) }
        if (posTags.isNotEmpty()) {
            // A readable sentence should usually have a noun/pronoun and a verb.
            val hasSubjectOrObject = posTags.any { it == "PRON" || it == "NOUN" }
            val hasAction = posTags.any { it == "VERB" }
            if (!hasSubjectOrObject || !hasAction) return false
        }

        return true
    }

    private fun isNearAcceptableSentence(words: List<String>): Boolean {
        // This is a lighter check used for backup sentences.
        // It is still careful about bad starts and bad endings.
        val cleanWords = words.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleanWords.size < 4) return false
        if (cleanWords.any { it in noisyWords }) return false
        if (!hasGoodStart(cleanWords)) return false
        if (!hasCompleteEnding(cleanWords)) return false
        if (hasAdjacentRepeat(cleanWords)) return false
        return true
    }

    private fun isAcceptableWordList(words: List<String>): Boolean =
        isAcceptableSentence(words.map { normalize(it) }.filter { it.isNotBlank() })

    private fun hasGoodStart(words: List<String>): Boolean {
        // This checks if the first word is a normal way to begin a sentence.
        val first = words.firstOrNull() ?: return false
        if (first in weakStarters || first in danglingEndings || first in noisyWords) return false
        if (first in naturalStarters) return true

        // If the word is not in the manual list, the POS database can still approve it.
        val firstPos = getPos(first)
        return firstPos == "PRON" || firstPos == "NOUN"
    }

    private fun hasCompleteEnding(words: List<String>): Boolean {
        // This checks if the sentence ends on a word that sounds complete.
        val last = words.lastOrNull() ?: return false
        if (last in danglingEndings || last in connectors || last in weakStarters || last in noisyWords) {
            return false
        }

        val lastPos = getPos(last)

        // Very short sentences ending with a verb can sound unfinished.
        if (lastPos == "VERB" && words.size < 7) return false
        return lastPos == null || lastPos == "NOUN" || lastPos == "PRON" || last in sentenceEnders
    }

    private fun partialQualityScore(words: List<String>): Double {
        // This scores an unfinished sentence while it is still being built.
        // Bad signs are given negative points early so they are less likely to continue.
        if (words.isEmpty()) return 0.0

        val cleanWords = words.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleanWords.isEmpty()) return 0.0

        var score = 0.0
        if (cleanWords.size == 1 && cleanWords.first() in weakStarters) score -= 2.0
        if (cleanWords.any { it in noisyWords }) score -= 5.0
        if (cleanWords.size > 2 && cleanWords.last() in danglingEndings) score -= 0.35
        if (hasAdjacentRepeat(cleanWords)) score -= 1.8
        if (hasRepeatedNgram(cleanWords, 2)) score -= 0.9
        if (hasRepeatedNgram(cleanWords, 3)) score -= 1.3

        return score
    }

    private fun endingScore(words: List<String>): Double {
        // This scores a possible ending.
        // A complete ending gets a reward, while an unfinished ending gets a penalty.
        val cleanWords = words.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleanWords.isEmpty()) return -2.0

        val last = cleanWords.last()
        return when {
            cleanWords.size < minWords -> -2.5
            !hasCompleteEnding(cleanWords) -> -4.0
            last in sentenceEnders -> 0.8
            cleanWords.size in 7..13 -> 0.35
            else -> 0.0
        }
    }

    private fun finalQualityScore(words: List<String>): Double {
        // This gives the final sentence an extra readability score.
        // It checks length, variety, start quality, end quality, and simple POS structure.
        val cleanWords = words.map { normalize(it) }.filter { it.isNotBlank() }
        if (cleanWords.isEmpty()) return Double.NEGATIVE_INFINITY

        var score = 0.0
        val idealLength = 9
        val uniqueRatio = cleanWords.toSet().size.toDouble() / cleanWords.size
        val posTags = cleanWords.mapNotNull { getPos(it) }

        score += 4.5 - (abs(cleanWords.size - idealLength) * 0.25)
        if (cleanWords.size in 5..16) score += 1.0
        if (cleanWords.size in 7..12) score += 0.6

        score += uniqueRatio * 1.8
        if (!hasGoodStart(cleanWords)) score -= 3.0
        if (!hasCompleteEnding(cleanWords)) score -= 4.0
        if (cleanWords.last() in sentenceEnders) score += 0.7
        if (cleanWords.any { it in connectors }) score += 0.25
        if (cleanWords.any { it in noisyWords }) score -= 7.0
        if (hasAdjacentRepeat(cleanWords)) score -= 2.4
        if (hasRepeatedNgram(cleanWords, 2)) score -= 1.2
        if (hasRepeatedNgram(cleanWords, 3)) score -= 1.8

        if (posTags.isNotEmpty()) {
            if (posTags.any { it == "VERB" }) score += 0.8 else score -= 1.8
            if (posTags.any { it == "PRON" || it == "NOUN" }) score += 0.5 else score -= 1.2
        }

        return score
    }

    private fun hasAdjacentRepeat(words: List<String>): Boolean =
        // This checks any repeating side by side words
        words.zipWithNext().any { (left, right) -> left == right }

    private fun hasRepeatedNgram(words: List<String>, size: Int): Boolean {
        // This checks if the same two word or three word pattern appears again.
        if (words.size < size * 2) return false

        val seen = mutableSetOf<String>()
        for (index in 0..words.size - size) {
            val ngram = words.subList(index, index + size).joinToString(" ")
            if (!seen.add(ngram)) return true
        }
        return false
    }

    // This cleans the final sentence before showing it to the user.
    // It removes start/end markers, repeated words, weak first words, and bad endings.

    private fun cleanEnding(words: List<String>): List<String> {
        val cleaned = words
            .filter { it != "<START>" && it != "<END>" }
            .toMutableList()

        var index = 1
        while (index < cleaned.size) {
            if (normalize(cleaned[index]) == normalize(cleaned[index - 1])) {
                cleaned.removeAt(index)
            } else {
                index++
            }
        }

        while (cleaned.size > 3 && normalize(cleaned.first()) in weakStarters) {
            cleaned.removeAt(0)
        }

        while (cleaned.size > 4 && normalize(cleaned.last()) in danglingEndings) {
            cleaned.removeAt(cleaned.lastIndex)
        }
        return cleaned
    }

    private fun format(words: List<String>): String {
        // This turns the final word list into normal readable text.
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

        if (result.isNotEmpty()) {
            result += chooseTerminal(result)
        }

        // Make the first letter uppercase.
        return result.replaceFirstChar { it.uppercaseChar() }
    }

    private fun chooseTerminal(sentence: String): String {
        // Use a question mark only when the sentence starts like a question.
        // Otherwise, use a period so the sentence does not look too dramatic.
        val first = normalize(sentence.split(" ").firstOrNull().orEmpty())
        return if (first in setOf("sino", "ano", "bakit", "paano", "kailan", "saan")) "?" else "."
    }

    private fun openNgramDatabase(fileName: String, required: Boolean = true): SQLiteDatabase? {
        // This opens one n-gram database.
        // The trigram database is required, while the quadgram database is optional.
        return try {
            val file = copyFromAssets("ngram_database/$fileName", fileName)
            SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        } catch (e: Exception) {
            if (required) throw e else null
        }
    }

    private fun copyFromAssets(assetPath: String, fileName: String): File {
        // The database starts inside the app assets.
        // Android needs it copied to app storage before SQLite can open it.
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
        // Close the databases when the model is no longer needed.
        trigramDb?.close()
        quadgramDb?.close()
        posDb?.close()
    }

    // This clears the saved sentence history.
    // It is useful when starting a fresh generation session.

    fun resetSession() {
        generatedSentences.clear()
        ngramFingerprints.clear()
        generationCount = 0
        temperatureIndex = 0
    }
}
