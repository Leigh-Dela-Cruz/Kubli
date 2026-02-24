package com.example.kubli.backend

// Bitstream conversion utilities

// Handles bitstream conversion of encrypted data
object BitstreamUtility {

    // Converts encrypted data into a bitstream
    fun createBitstream(data: EncryptedData, useHamming: Boolean): String {
        // Combine salt, nonce, and ciphertext
        val bitsData = data.salt + data.nonce + data.ciphertext
        
        return if (useHamming) {
            // Add length header and apply Hamming encoding
            val header = intToBytes(bitsData.size)
            HammingCode.encodeBytes(header + bitsData)
        } else {
            // Convert each byte into 8-bit binary
            bitsData.joinToString("") { it.toInt().and(0xFF).toString(2).padStart(8, '0') }
        }
    }

    // Extracts encrypted data from a bitstream
    fun extractEncryptedData(bits: String, useHamming: Boolean): Pair<EncryptedData?, Int> {
        return if (useHamming) {
            try {
                // Decode bitstream and fix errors
                val (bytes, errors) = HammingCode.decodeBytes(bits)
                // Read data length
                val length = bytesToInt(bytes.sliceArray(0..3))
                // Get encrypted data
                val bitsData = bytes.sliceArray(4 until bytes.size)

                // Check minimum required size
                if (bitsData.size >= 28) {
                    Pair(EncryptedData(
                        bitsData.sliceArray(0..15),
                        bitsData.sliceArray(16..27),
                        bitsData.sliceArray(28 until bitsData.size)
                    ), errors)
                } else {
                    Pair(null, errors)
                }
            } catch (e: Exception) {
                Pair(null, 0)
            }
        } else {
            try {
                // Convert bits back to bytes
                val bytes = bits.chunked(8).map { it.toInt(2).toByte() }.toByteArray()

                // Validate data size
                if (bytes.size >= 28) {
                    Pair(EncryptedData(
                        bytes.sliceArray(0..15),
                        bytes.sliceArray(16..27),
                        bytes.sliceArray(28 until bytes.size)
                    ), 0)
                } else {
                    Pair(null, 0)
                }
            } catch (e: Exception) {
                Pair(null, 0)
            }
        }
    }

    // Converts an integer to 4 bytes
    private fun intToBytes(value: Int) = ByteArray(4) { i -> 
        (value shr (24 - i * 8)).toByte() 
    }

    // Converts 4 bytes back to an integer
    private fun bytesToInt(bytes: ByteArray) = 
        bytes.take(4).foldIndexed(0) { i, acc, b -> 
            acc or ((b.toInt() and 0xFF) shl (24 - i * 8)) 
        }
}
