package com.example.kubli.backend

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.Deflater
import java.util.zip.Inflater

class ImageSteganography {

    companion object {

        // ============================
        // MAIN FUNCTIONS
        // ============================

        fun encode(message: String, password: String, bitmap: Bitmap): Bitmap {

            // 1. Compress
            val compressed = compress(message.toByteArray(Charsets.UTF_8))

            // 2. Encrypt
            val encrypted = encrypt(compressed, password)

            // 3. Add length header
            val payload = addLengthHeader(encrypted)

            // 4. Embed into image
            return embedLSB(bitmap, payload)
        }

        fun decode(bitmap: Bitmap, password: String): String {

            // 1. Extract raw data
            val extracted = extractLSB(bitmap)

            // 2. Read length header
            val buffer = ByteBuffer.wrap(extracted)
            val length = buffer.int

            val encrypted = ByteArray(length)
            buffer.get(encrypted)

            // 3. Decrypt
            val decrypted = decrypt(encrypted, password)

            // 4. Decompress
            val decompressed = decompress(decrypted)

            return String(decompressed, Charsets.UTF_8)
        }

        // ============================
        // COMPRESSION (DEFLATE)
        // ============================

        fun compress(data: ByteArray): ByteArray {
            val deflater = Deflater()
            deflater.setInput(data)
            deflater.finish()

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)

            while (!deflater.finished()) {
                val count = deflater.deflate(buffer)
                output.write(buffer, 0, count)
            }

            return output.toByteArray()
        }

        fun decompress(data: ByteArray): ByteArray {
            val inflater = Inflater()
            inflater.setInput(data)

            val output = ByteArrayOutputStream()
            val buffer = ByteArray(1024)

            while (!inflater.finished()) {
                val count = inflater.inflate(buffer)
                output.write(buffer, 0, count)
            }

            return output.toByteArray()
        }

        // ============================
        // AES-256 (SHA-256 KEY)
        // ============================

        private fun getAESKey(password: String): SecretKeySpec {

            require(password.length <= 8) {
                "Password must be at most 8 characters."
            }

            val digest = MessageDigest.getInstance("SHA-256")
            val keyBytes = digest.digest(password.toByteArray(Charsets.UTF_8))

            return SecretKeySpec(keyBytes, "AES")
        }

        fun encrypt(data: ByteArray, password: String): ByteArray {

            val key = getAESKey(password)

            val iv = ByteArray(16)
            SecureRandom().nextBytes(iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, IvParameterSpec(iv))

            val encrypted = cipher.doFinal(data)

            // Store IV + ciphertext
            return iv + encrypted
        }

        fun decrypt(data: ByteArray, password: String): ByteArray {

            val iv = data.copyOfRange(0, 16)
            val ciphertext = data.copyOfRange(16, data.size)

            val key = getAESKey(password)

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, IvParameterSpec(iv))

            return cipher.doFinal(ciphertext)
        }

        // ============================
        // LSB EMBEDDING
        // ============================

        fun embedLSB(bitmap: Bitmap, data: ByteArray): Bitmap {

            val width = bitmap.width
            val height = bitmap.height

            val capacity = width * height * 3
            val bits = toBitArray(data)

            if (bits.size > capacity) {
                throw IllegalArgumentException("Message too large for this image.")
            }

            val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)

            var bitIndex = 0

            for (y in 0 until height) {
                for (x in 0 until width) {

                    if (bitIndex >= bits.size) return result

                    val pixel = result.getPixel(x, y)

                    var r = (pixel shr 16) and 0xFF
                    var g = (pixel shr 8) and 0xFF
                    var b = pixel and 0xFF

                    if (bitIndex < bits.size) {
                        r = (r and 0xFE) or bits[bitIndex++]
                    }
                    if (bitIndex < bits.size) {
                        g = (g and 0xFE) or bits[bitIndex++]
                    }
                    if (bitIndex < bits.size) {
                        b = (b and 0xFE) or bits[bitIndex++]
                    }

                    val newPixel = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                    result.setPixel(x, y, newPixel)
                }
            }

            return result
        }

        fun extractLSB(bitmap: Bitmap): ByteArray {

            val width = bitmap.width
            val height = bitmap.height

            val bits = ArrayList<Int>()

            for (y in 0 until height) {
                for (x in 0 until width) {

                    val pixel = bitmap.getPixel(x, y)

                    bits.add((pixel shr 16) and 1)
                    bits.add((pixel shr 8) and 1)
                    bits.add(pixel and 1)
                }
            }

            return bitsToByteArray(bits)
        }

        // ============================
        // HELPERS
        // ============================

        private fun addLengthHeader(data: ByteArray): ByteArray {
            val buffer = ByteBuffer.allocate(4 + data.size)
            buffer.putInt(data.size)
            buffer.put(data)
            return buffer.array()
        }

        private fun toBitArray(data: ByteArray): IntArray {
            val bits = IntArray(data.size * 8)
            var index = 0

            for (byte in data) {
                for (i in 7 downTo 0) {
                    bits[index++] = (byte.toInt() shr i) and 1
                }
            }

            return bits
        }

        private fun bitsToByteArray(bits: List<Int>): ByteArray {
            val byteCount = bits.size / 8
            val bytes = ByteArray(byteCount)

            for (i in 0 until byteCount) {
                var value = 0
                for (j in 0 until 8) {
                    value = (value shl 1) or bits[i * 8 + j]
                }
                bytes[i] = value.toByte()
            }

            return bytes
        }
    }
}