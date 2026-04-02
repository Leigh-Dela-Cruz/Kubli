package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.random.Random


//SQLite-based N-gram model for text generation

class NgramModel(private val context: Context, private val order: Int = 3) {
    
    // Variable to store the database connection.
    private var db: SQLiteDatabase? = null
    
    // Function to set up the database.
    fun initialize() {
        // Find the correct database file name based on the N-gram order.
        val dbName = "filipino_${order}gram.db"
        // Copy the file from assets to the phone's storage and open it.
        val dbFile = copyFromAssets("ngram_database/$dbName", dbName)
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
    
    // Function to create a sentence of a certain length.
    fun generate(maxWords: Int = 15, seed: Long? = null): String {
        val random = seed?.let { Random(it) } ?: Random.Default
        // Start with markers to show the beginning of a sentence.
        val context = MutableList(order - 1) { "<START>" }
        val words = mutableListOf<String>()
        
        // Keep picking words until the sentence is long enough or reaches an end marker.
        repeat(maxWords) {
            // Get the next word based on the recent context.
            val next = pickNext(context.takeLast(order - 1), random) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context.add(next)
        }
        
        // Join all picked words into a single string with spaces.
        return words.joinToString(" ")
    }
    
    // Function to find possible next words in the database.
    private fun pickNext(context: List<String>, random: Random): String? {
        // Create a SQL query to search for words that follow the current ones.
        val query = when (order) {
            3 -> "SELECT word3, frequency FROM ngrams WHERE word1=? AND word2=?"
            4 -> "SELECT word4, frequency FROM ngrams WHERE word1=? AND word2=? AND word3=?"
            else -> return null
        }
        
        // Search the database and read the results.
        db?.rawQuery(query, context.toTypedArray())?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            
            // List all candidate words and how many times they appear in the data.
            val candidates = mutableListOf<Pair<String, Int>>()
            do {
                candidates.add(Pair(cursor.getString(0), cursor.getInt(1)))
            } while (cursor.moveToNext())
            
            // Pick a word randomly, but give higher chances to words that appear more often.
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
    
    // Function to get information about the database content.
    fun getStats(): Map<String, Any> {
        // Count how many word patterns are stored in the table.
        val count = db?.rawQuery("SELECT COUNT(*) FROM ngrams", null)?.use {
            it.moveToFirst()
            it.getInt(0)
        } ?: 0
        
        return mapOf(
            "ngramOrder" to order,
            "totalNgrams" to count,
            "storageType" to "SQLite"
        )
    }
    
    // Function to close the database when it is no longer needed to save memory.
    fun close() {
        db?.close()
        db = null
    }
    
    // Helper function to move the database file from assets to the application's data folder.
    private fun copyFromAssets(assetPath: String, fileName: String): File {
        val outputFile = context.getDatabasePath(fileName)
        
        // Check if the file is already there before copying it over.
        if (!outputFile.exists() || outputFile.length() == 0L) {
            outputFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                outputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        
        return outputFile
    }
}
