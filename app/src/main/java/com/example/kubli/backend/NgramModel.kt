package com.example.kubli.backend

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import kotlin.random.Random


//SQLite-based N-gram model for text generation

class NgramModel(private val context: Context, private val order: Int = 3) {
    
    private var db: SQLiteDatabase? = null
    
    fun initialize() {
        val dbName = "filipino_${order}gram.db"
        val dbFile = copyFromAssets("ngram_database/$dbName", dbName)
        db = SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
    }
    
    fun generate(maxWords: Int = 15, seed: Long? = null): String {
        val random = seed?.let { Random(it) } ?: Random.Default
        val context = MutableList(order - 1) { "<START>" }
        val words = mutableListOf<String>()
        
        repeat(maxWords) {
            val next = pickNext(context.takeLast(order - 1), random) ?: return@repeat
            if (next == "<END>") return@repeat
            words.add(next)
            context.add(next)
        }
        
        return words.joinToString(" ")
    }
    
    private fun pickNext(context: List<String>, random: Random): String? {
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
    
    fun getStats(): Map<String, Any> {
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
        }
        
        return outputFile
    }
}
