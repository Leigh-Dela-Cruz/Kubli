package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.File
import kotlin.math.ln
import kotlin.random.Random

// Viterbi Model an advanced version of the Markov and N-gram model

class ViterbiModel(private val context: Context, private val order: Int = 3) {
    
    // This variable manages the connection to the database file.
    private var db: SQLiteDatabase? = null
    
    // This function sets up the database by copying it from the app assets.
    fun initialize() {
        val dbName = "filipino_${order}gram.db"
        val dbFile = copyFromAssets("ngram_database/$dbName", dbName)
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
    
    // This function generates text using the Viterbi algorithm.
    // It always picks the most likely next word instead of picking randomly.
    fun generateViterbi(maxWords: Int = 15): String {
        val words = mutableListOf<String>()
        var context = List(order - 1) { "<START>" }
        
        repeat(maxWords) {
            // Find the best next word based on probability.
            val next = viterbiNextWord(context) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context = (context + next).takeLast(order - 1)
        }

        return words.joinToString(" ")
            .replace(Regex("\\s+([.,!?;:])"), "$1")
    }
    
    // Helper function for Viterbi that selects the word with the highest probability.
    private fun viterbiNextWord(context: List<String>): String? {
        val candidates = getNextWordsWithProbs(context) ?: return null
        
        // Pick the word that has the maximum probability score.
        return candidates.maxByOrNull { it.second }?.first
    }
    
    // This function generates text using the Markov chain method.
    // It picks words randomly, but gives more common words a better chance.
    fun generateMarkov(maxWords: Int = 15, seed: Long? = null): String {
        val random = seed?.let { Random(it) } ?: Random.Default
        val context = MutableList(order - 1) { "<START>" }
        val words = mutableListOf<String>()
        
        repeat(maxWords) {
            // Pick a word using weighted random selection.
            val next = markovNextWord(context.takeLast(order - 1), random) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context.add(next)
        }

        return words.joinToString(" ")
            .replace(Regex("\\s+([.,!?;:])"), "$1")
    }
    
    // Helper function that runs a database query to find words following the current context.
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
            
            // Randomly select a word based on its frequency in the data.
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
    
    // This function calculates the probability of each possible next word.
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
            
            // Use math logarithms to calculate probabilities more accurately.
            val total = candidates.sumOf { it.second }.toDouble()
            return candidates.map { (word, freq) -> 
                Pair(word, ln(freq / total)) // Log probability
            }
        }
        
        return null
    }
    
    // This is an advanced search that keeps track of several "best" sentences at once.
    fun generateBeamSearch(maxWords: Int = 15, beamWidth: Int = 5): String {
        data class Beam(val words: List<String>, val logProb: Double)

        var beams = listOf(Beam(emptyList(), 0.0))

        for (step in 0 until maxWords) {
            val newBeams = mutableListOf<Beam>()

            for (beam in beams) {
                val context = (List(order - 1) { "<START>" } + beam.words)
                    .takeLast(order - 1)

                val candidates = getNextWordsWithProbs(context) ?: continue

                // Look at the top candidate words for each possible sentence path.
                for ((word, logProb) in candidates.take(beamWidth)) {
                    if (word == "<END>") continue
                    newBeams.add(
                        Beam(beam.words + word, beam.logProb + logProb)
                    )
                }
            }

            if (newBeams.isEmpty()) break

            // Only keep the best few paths to continue in the next step.
            beams = newBeams
                .sortedByDescending { it.logProb }
                .take(beamWidth)
        }

        // Return the best sentence found by the search.
        return beams.firstOrNull()?.words
            ?.joinToString(" ")
            ?.replace(Regex("\\s+([.,!?;:])"), "$1")
            ?: ""
    }
    
    // Returns basic statistics about the database and algorithms.
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
    
    // Properly closes the database to avoid memory leaks.
    fun close() {
        db?.close()
        db = null
    }

    // Helper to move the database from assets to a folder where the app can read/write.
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
