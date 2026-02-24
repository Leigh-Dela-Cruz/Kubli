package com.example.kubli.backend

import android.content.Context

// Main engine that handles hiding and finding secret messages

class SteganographyMain(
    context: Context,
    private val ngramOrder: Int = 3,
    private val useHamming: Boolean = true,
    private val useViterbi: Boolean = true
) {
    // The language model used to create text that looks natural.
    private val model = ViterbiModel(context, ngramOrder).apply { initialize() }
    
    // This function hides a secret message inside a piece of generated text.
    fun encrypt(secret: String, passphrase: String, maxWords: Int = 15, seed: Long? = null): Result {
        // First, encrypt the secret message using the password for security.
        val encrypted = CryptoUtility.encrypt(secret, passphrase)
        
        // Convert the encrypted data into a stream of bits.
        val bits = BitstreamUtility.createBitstream(encrypted, useHamming)

        // Generate a normal looking sentence to use as a cover for the secret message.
        val cover = model.generateMarkov(maxWords, seed = System.currentTimeMillis())
        
        // Hide the secret bits inside the cover sentence.
        val stego = ZeroSteganography.embed(cover, bits)
        
        // Return the final result including the text with the hidden message.
        return Result(
            success = true,
            data = stego,
            visible = ZeroSteganography.getVisible(stego),
            errorsCorrected = 0,
            algorithm = if (useViterbi) "Viterbi" else "Markov"
        )
    }
    
    // This function finds and retrieves a hidden secret from text.
    fun decrypt(stegoText: String, passphrase: String): Result {
        // Check if the text actually contains any hidden data before trying to read it.
        if (!ZeroSteganography.hasHidden(stegoText)) {
            return Result(false, error = "No hidden data found")
        }
        
        // Extract the hidden bits from the text.
        val bits = ZeroSteganography.extract(stegoText)
        
        // Convert those bits back into encrypted data and fix any small errors.
        val (encrypted, errors) = BitstreamUtility.extractEncryptedData(bits, useHamming)
        
        if (encrypted == null) {
            return Result(false, error = "Invalid data")
        }
        
        // Try to decrypt the data using the password to get the original message back.
        return try {
            val message = CryptoUtility.decrypt(encrypted, passphrase)
            Result(true, data = message, errorsCorrected = errors)
        } catch (e: Exception) {
            // Return an error if the password is wrong or the data is corrupted.
            Result(false, error = "Decryption failed: ${e.message}")
        }
    }
    
    // Shut down the model to free up memory when it is no longer needed.
    fun close() = model.close()
    
    // A class to hold the output of the encryption or decryption process.
    data class Result(
        val success: Boolean,
        val data: String? = null,
        val visible: String? = null,
        val error: String? = null,
        val errorsCorrected: Int = 0,
        val algorithm: String? = null
    )
}
