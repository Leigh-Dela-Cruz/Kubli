package com.example.kubli.backend

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


//AES-256-GCM encryption utilities

object CryptoUtility {
    
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA256"
    
    fun encrypt(plaintext: String, passphrase: String): EncryptedData {
        val salt = SecureRandom().generateSeed(16)
        val nonce = SecureRandom().generateSeed(12)
        val key = deriveKey(passphrase, salt)
        
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        val ciphertext = cipher.doFinal(plaintext.toByteArray())
        
        return EncryptedData(salt, nonce, ciphertext)
    }
    
    fun decrypt(data: EncryptedData, passphrase: String): String {
        val key = deriveKey(passphrase, data.salt)
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data.nonce))
        return String(cipher.doFinal(data.ciphertext))
    }
    
    private fun deriveKey(passphrase: String, salt: ByteArray) = 
        SecretKeySpec(
            SecretKeyFactory.getInstance(KEY_ALGORITHM)
                .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, 100000, 256))
                .encoded,
            "AES"
        )
}

data class EncryptedData(val salt: ByteArray, val nonce: ByteArray, val ciphertext: ByteArray)
