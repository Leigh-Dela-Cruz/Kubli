package com.example.kubli.backend

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


// Steganography API

class SteganographyAPI(
    context: Context,
    useViterbi: Boolean = true
) {
    // Create the language model and load the database patterns.
    val model = ViterbiModel(context, 3).apply {
        initialize() // Prepare the word database.
    }

    // Set up the engine that does the main encryption and decryption work.
    private val engine = SteganographyMain(
        context = context,
        ngramOrder = 3,
        useHamming = true, // Use a method to fix small errors in the text automatically.
        useViterbi = useViterbi
    )

    // This function hides a secret message inside a generated sentence.
    suspend fun encrypt(secret: String, password: String): EncryptResult =
        // Run this in the background so the app screen doesn't freeze.
        withContext(Dispatchers.IO) {
            when {
                // Make sure the inputs are not empty and the password is long enough.
                secret.isBlank() -> EncryptResult(error = "Secret cannot be empty")
                password.length < 8 -> EncryptResult(error = "Password must be 8+ characters")
                else -> {
                    // Try to hide the message using the engine.
                    val result = engine.encrypt(secret, password)
                    if (result.success) {
                        // If it worked, return the hidden text and some details.
                        EncryptResult(
                            stegoText = result.data!!,
                            visibleText = result.visible!!,
                            algorithm = result.algorithm
                        )
                    } else {
                        // If it failed, return the error message.
                        EncryptResult(error = result.error ?: "Encryption failed")
                    }
                }
            }
        }

    // This function takes hidden text and tries to find the original secret message.
    suspend fun decrypt(stegoText: String, password: String): DecryptResult =
        withContext(Dispatchers.IO) {
            when {
                // Ensure the text and password are provided.
                stegoText.isBlank() -> DecryptResult(error = "Text cannot be empty")
                password.isBlank() -> DecryptResult(error = "Password cannot be empty")
                else -> {
                    // Ask the engine to recover the secret.
                    val result = engine.decrypt(stegoText, password)
                    if (result.success) {
                        // Return the secret message and any errors that were fixed.
                        DecryptResult(
                            message = result.data!!,
                            errorsCorrected = result.errorsCorrected
                        )
                    } else {
                        // Return an error if decryption was not possible.
                        DecryptResult(error = result.error ?: "Decryption failed")
                    }
                }
            }
        }

    // Close everything when the API is done to save memory.
    fun close() = engine.close()
}

// Result class for when a message is being hidden.
data class EncryptResult(
    val stegoText: String? = null,     // This is the final text with the hidden message.
    val visibleText: String? = null,   // This is a simple version of the text to look at.
    val algorithm: String? = null,     // The mathematical method used for the text.
    val error: String? = null          // Holds an error message if something went wrong.
) {
    // Check if the process was successful.
    val success: Boolean get() = error == null
}

// Result class for when a secret message is being recovered.
data class DecryptResult(
    val message: String? = null,       // This is the original secret message that was found.
    val errorsCorrected: Int = 0,      // Number of small mistakes that were fixed.
    val error: String? = null          // Holds an error message if something went wrong.
) {
    // Check if the process was successful.
    val success: Boolean get() = error == null
}
