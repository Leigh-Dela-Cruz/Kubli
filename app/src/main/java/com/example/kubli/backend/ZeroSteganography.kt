package com.example.kubli.backend


// This class hides secret data inside text using invisible characters.
// These characters take up no space on the screen, so the text looks normal.

object ZeroSteganography {
    
    // These are special Unicode characters that are completely invisible.
    private const val ZERO = '\u200b'
    private const val ONE = '\u200c'
    
    // This list defines where the invisible characters can be inserted.
    private val INJECT_AFTER = setOf(' ', '.', ',', '!', '?', ';', ':', '\n')
    
    // This function hides a string of bits inside a cover sentence.
    fun embed(coverText: String, bits: String): String = buildString {
        var i = 0
        coverText.forEach { char ->
            append(char) // Keep the original character.
            // If the character is a symbol or space, insert one hidden bit after it.
            if (char in INJECT_AFTER && i < bits.length) {
                append(if (bits[i++] == '1') ONE else ZERO)
            }
        }
        // If there are still bits left over, add them to the very end of the text.
        while (i < bits.length) {
            append(if (bits[i++] == '1') ONE else ZERO)
        }
    }
    
    // This function looks for invisible characters and turns them back into 0 and 1.
    fun extract(text: String): String = 
        text.filter { it == ZERO || it == ONE }
            .map { if (it == ONE) '1' else '0' }
            .joinToString("")
    
    // This function removes all hidden bits to show only the readable text.
    fun getVisible(text: String): String = 
        text.filter { it != ZERO && it != ONE }
    
    // This function checks if a piece of text contains any hidden data.
    fun hasHidden(text: String): Boolean = 
        text.contains(ZERO) || text.contains(ONE)
}
