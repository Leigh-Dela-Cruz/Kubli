package com.example.kubli.backend

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


//AES-256-GCM encryption utilities

object CryptoUtility {

    // Encryption algorithm configuration
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"

    // Encrypts a plaintext message using a passphrase
    fun encrypt(plaintext: String, passphrase: String): EncryptedData {
        // Generate random salt for key derivation
        val salt = SecureRandom().generateSeed(16)
        // Generate random nonce (IV) for AES-GCM
        val nonce = SecureRandom().generateSeed(12)
        // Derive AES-256 key from passphrase and salt
        val key = deriveKey(passphrase, salt)

        // Initialize cipher for encryption
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))

        // Encrypt plaintext into ciphertext
        val ciphertext = cipher.doFinal(plaintext.toByteArray())

        // Return all values needed for decryption
        return EncryptedData(salt, nonce, ciphertext)
    }

    // Decrypts encrypted data using the same passphrase
    fun decrypt(data: EncryptedData, passphrase: String): String {

        // Recreate the AES key using stored salt
        val key = deriveKey(passphrase, data.salt)
        // Initialize cipher for decryption
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data.nonce))

        // Decrypt ciphertext back to plaintext
        return String(cipher.doFinal(data.ciphertext))
    }

    // Derives a 256-bit AES key from a passphrase using PBKDF2
    private fun deriveKey(passphrase: String, salt: ByteArray) = 
        SecretKeySpec(
            SecretKeyFactory.getInstance(KEY_ALGORITHM)
                .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, 100000, 256))
                .encoded,
            "AES"
        )
}

// Holds all values required for decryption
data class EncryptedData(val salt: ByteArray, val nonce: ByteArray, val ciphertext: ByteArray)
