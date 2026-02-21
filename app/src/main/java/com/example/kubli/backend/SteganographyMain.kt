package com.example.kubli.backend

import android.content.Context

/**
 * Enhanced steganography engine with multiple algorithms
 * 
 * ALGORITHMS USED:
 * 1. N-gram Model (trigram/quadgram)
 * 2. Markov Chains (stochastic text generation)
 * 3. Viterbi Algorithm (optimal path selection)
 * 4. AES-256-GCM (encryption)
 * 5. Hamming(7,4) (error correction)
 */
class SteganographyMain(
    context: Context,
    private val ngramOrder: Int = 3,
    private val useHamming: Boolean = true,
    private val useViterbi: Boolean = true  // NEW: Enable Viterbi
) {
    private val model = ViterbiModel(context, ngramOrder).apply { initialize() }
    
    fun encrypt(secret: String, passphrase: String, maxWords: Int = 15, seed: Long? = null): Result {
        val encrypted = CryptoUtility.encrypt(secret, passphrase)
        val bits = BitstreamUtility.createBitstream(encrypted, useHamming)

        val cover = model.generateMarkov(maxWords, seed = System.currentTimeMillis())
        
        val stego = ZeroSteganography.embed(cover, bits)
        
        return Result(
            success = true,
            data = stego,
            visible = ZeroSteganography.getVisible(stego),
            errorsCorrected = 0,
            algorithm = if (useViterbi) "Viterbi" else "Markov"
        )
    }
    
    fun decrypt(stegoText: String, passphrase: String): Result {
        if (!ZeroSteganography.hasHidden(stegoText)) {
            return Result(false, error = "No hidden data found")
        }
        
        val bits = ZeroSteganography.extract(stegoText)
        val (encrypted, errors) = BitstreamUtility.extractEncryptedData(bits, useHamming)
        
        if (encrypted == null) {
            return Result(false, error = "Invalid payload")
        }
        
        return try {
            val message = CryptoUtility.decrypt(encrypted, passphrase)
            Result(true, data = message, errorsCorrected = errors)
        } catch (e: Exception) {
            Result(false, error = "Decryption failed: ${e.message}")
        }
    }
    
    fun close() = model.close()
    
    data class Result(
        val success: Boolean,
        val data: String? = null,
        val visible: String? = null,
        val error: String? = null,
        val errorsCorrected: Int = 0,
        val algorithm: String? = null
    )
}
