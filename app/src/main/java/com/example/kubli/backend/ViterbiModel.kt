package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import kotlin.math.ln
import kotlin.random.Random

/**
 * Enhanced N-gram model with Viterbi algorithm
 * 
 * Uses:
 * - N-gram statistical model (trigram/quadgram)
 * - Markov chains (text generation state transitions)
 * - Viterbi algorithm (optimal path selection)
 */
class ViterbiModel(private val context: Context, private val order: Int = 3) {
    
    private var db: SQLiteDatabase? = null
    
    fun initialize() {
        val dbName = "filipino_${order}gram.db"
        val dbFile = copyFromAssets("ngram_database/$dbName", dbName)
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
    
    /**
     * Generate text using VITERBI ALGORITHM
     * Finds the most likely sequence of words given the n-gram model
     */
    fun generateViterbi(maxWords: Int = 15): String {
        val words = mutableListOf<String>()
        var context = List(order - 1) { "<START>" }
        
        repeat(maxWords) {
            val next = viterbiNextWord(context) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context = (context + next).takeLast(order - 1)
        }
        
        return words.joinToString(" ")
    }
    
    /**
     * VITERBI: Select word with maximum probability (MAP - Maximum A Posteriori)
     */
    private fun viterbiNextWord(context: List<String>): String? {
        val candidates = getNextWordsWithProbs(context) ?: return null
        
        // Viterbi: Pick word with highest probability
        return candidates.maxByOrNull { it.second }?.first
    }
    
    /**
     * Generate text using MARKOV CHAIN with weighted random sampling
     * (Original implementation - frequency-based random selection)
     */
    fun generateMarkov(maxWords: Int = 15, seed: Long? = null): String {
        val random = seed?.let { Random(it) } ?: Random.Default
        val context = MutableList(order - 1) { "<START>" }
        val words = mutableListOf<String>()
        
        repeat(maxWords) {
            val next = markovNextWord(context.takeLast(order - 1), random) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context.add(next)
        }
        
        return words.joinToString(" ")
    }
    
    /**
     * MARKOV CHAIN: Select word using weighted random sampling based on frequency
     */
    private fun markovNextWord(context: List<String>, random: Random): String? {
        val query = when (order) {
            3 -> "SELECT word3, frequency FROM ngrams WHERE word1=? AND word2=?"
            4 -> "SELECT word4, frequency FROM ngrams WHERE word1=? AND word2=? AND word3=?"
            else -> return null
        }
        
        db?.rawQuery(query, context.toTypedArray())?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            
            val candidates = mutableListOf<Pair<String, Int>>()
            do {
                candidates.add(Pair(cursor.getString(0), cursor.getInt(1)))
            } while (cursor.moveToNext())
            
            // Weighted random selection (Markov chain stochastic sampling)
            val total = candidates.sumOf { it.second }
            var rand = random.nextInt(total)
            
            for ((word, freq) in candidates) {
                rand -= freq
                if (rand < 0) return word
            }
            
            return candidates.random(random).first
        }
        
        return null
    }
    
    /**
     * Get next words with probabilities (for Viterbi)
     */
    private fun getNextWordsWithProbs(context: List<String>): List<Pair<String, Double>>? {
        val query = when (order) {
            3 -> "SELECT word3, frequency FROM ngrams WHERE word1=? AND word2=?"
            4 -> "SELECT word4, frequency FROM ngrams WHERE word1=? AND word2=? AND word3=?"
            else -> return null
        }
        
        db?.rawQuery(query, context.toTypedArray())?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            
            val candidates = mutableListOf<Pair<String, Int>>()
            do {
                candidates.add(Pair(cursor.getString(0), cursor.getInt(1)))
            } while (cursor.moveToNext())
            
            // Calculate probabilities (log probabilities for numerical stability)
            val total = candidates.sumOf { it.second }.toDouble()
            return candidates.map { (word, freq) -> 
                Pair(word, ln(freq / total)) // Log probability
            }
        }
        
        return null
    }
    
    /**
     * Generate text with BEAM SEARCH (advanced Viterbi variant)
     * Maintains top-k candidate sequences
     */
    fun generateBeamSearch(maxWords: Int = 15, beamWidth: Int = 5): String {
        data class Beam(val words: List<String>, val logProb: Double)

        var beams = listOf(Beam(emptyList(), 0.0))

        for (step in 0 until maxWords) {
            val newBeams = mutableListOf<Beam>()

            for (beam in beams) {
                val context = (List(order - 1) { "<START>" } + beam.words)
                    .takeLast(order - 1)

                val candidates = getNextWordsWithProbs(context) ?: continue

                for ((word, logProb) in candidates.take(beamWidth)) {
                    if (word == "<END>") continue
                    newBeams.add(
                        Beam(beam.words + word, beam.logProb + logProb)
                    )
                }
            }

            if (newBeams.isEmpty()) break

            beams = newBeams
                .sortedByDescending { it.logProb }
                .take(beamWidth)
        }

        return beams.firstOrNull()?.words?.joinToString(" ") ?: ""
    }
    
    fun getStats(): Map<String, Any> {
        val count = db?.rawQuery("SELECT COUNT(*) FROM ngrams", null)?.use {
            it.moveToFirst()
            it.getInt(0)
        } ?: 0
        
        return mapOf(
            "ngramOrder" to order,
            "totalNgrams" to count,
            "storageType" to "SQLite",
            "algorithms" to listOf("N-gram", "Markov Chain", "Viterbi", "Beam Search")
        )
    }
    
    fun close() {
        db?.close()
        db = null
    }

    private fun copyFromAssets(assetPath: String, fileName: String): File {
        val outputFile = context.getDatabasePath(fileName)

        if (!outputFile.exists() || outputFile.length() == 0L) {
            outputFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Log.d("ViterbiModel", "Copied DB to: ${outputFile.absolutePath}, size: ${outputFile.length()}")
        } else {
            Log.d("ViterbiModel", "DB already exists: ${outputFile.absolutePath}, size: ${outputFile.length()}")
        }

        return outputFile
    }
}
