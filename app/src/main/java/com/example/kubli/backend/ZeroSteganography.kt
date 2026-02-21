package com.example.kubli.backend


//Zero-width character steganography

object ZeroSteganography {
    
    private const val ZERO = '\u200b'
    private const val ONE = '\u200c'
    private val INJECT_AFTER = setOf(' ', '.', ',', '!', '?', ';', ':', '\n')
    
    fun embed(coverText: String, bits: String): String = buildString {
        var i = 0
        coverText.forEach { char ->
            append(char)
            if (char in INJECT_AFTER && i < bits.length) {
                append(if (bits[i++] == '1') ONE else ZERO)
            }
        }
        while (i < bits.length) {
            append(if (bits[i++] == '1') ONE else ZERO)
        }
    }
    
    fun extract(text: String): String = 
        text.filter { it == ZERO || it == ONE }
            .map { if (it == ONE) '1' else '0' }
            .joinToString("")
    
    fun getVisible(text: String): String = 
        text.filter { it != ZERO && it != ONE }
    
    fun hasHidden(text: String): Boolean = 
        text.contains(ZERO) || text.contains(ONE)
}
