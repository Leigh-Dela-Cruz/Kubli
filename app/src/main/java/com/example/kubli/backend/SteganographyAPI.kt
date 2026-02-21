package com.example.kubli.backend

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Steganography API

class SteganographyAPI(
    context: Context,
    useViterbi: Boolean = true
) {
    // Step 1: Initialize the model first
    val model = ViterbiModel(context, 3).apply {
        initialize() // load the database here
    }

    // Step 2: Create the engine after DB is ready
    private val engine = SteganographyMain(
        context = context,
        ngramOrder = 3,
        useHamming = true,
        useViterbi = useViterbi
    )

    /**
     * ENCRYPT MESSAGE
     *
     * Uses:
     * - N-gram model for text generation
     * - Viterbi OR Markov for word selection
     * - AES-256-GCM for encryption
     * - Hamming(7,4) for error correction
     */
    suspend fun encrypt(secret: String, password: String): EncryptResult =
        withContext(Dispatchers.IO) {
            when {
                secret.isBlank() -> EncryptResult(error = "Secret cannot be empty")
                password.length < 8 -> EncryptResult(error = "Password must be 8+ characters")
                else -> {
                    val result = engine.encrypt(secret, password)
                    if (result.success) {
                        EncryptResult(
                            stegoText = result.data!!,
                            visibleText = result.visible!!,
                            algorithm = result.algorithm
                        )
                    } else {
                        EncryptResult(error = result.error ?: "Encryption failed")
                    }
                }
            }
        }

    /**
     * DECRYPT MESSAGE
     *
     * Uses:
     * - Hamming(7,4) for error correction
     * - AES-256-GCM for decryption
     */
    suspend fun decrypt(stegoText: String, password: String): DecryptResult =
        withContext(Dispatchers.IO) {
            when {
                stegoText.isBlank() -> DecryptResult(error = "Text cannot be empty")
                password.isBlank() -> DecryptResult(error = "Password cannot be empty")
                else -> {
                    val result = engine.decrypt(stegoText, password)
                    if (result.success) {
                        DecryptResult(
                            message = result.data!!,
                            errorsCorrected = result.errorsCorrected
                        )
                    } else {
                        DecryptResult(error = result.error ?: "Decryption failed")
                    }
                }
            }
        }

    fun close() = engine.close()
}

//Encryption
data class EncryptResult(
    val stegoText: String? = null,     // Share/save this
    val visibleText: String? = null,   // Preview this
    val algorithm: String? = null,     // Algorithm used (Viterbi/Markov)
    val error: String? = null
) {
    val success: Boolean get() = error == null
}

//Decryption
data class DecryptResult(
    val message: String? = null,       // The secret message
    val errorsCorrected: Int = 0,      // Errors fixed by Hamming
    val error: String? = null
) {
    val success: Boolean get() = error == null
}