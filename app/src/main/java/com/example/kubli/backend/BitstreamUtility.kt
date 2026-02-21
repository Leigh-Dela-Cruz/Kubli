package com.example.kubli.backend

//Bitstream conversion utilities

object BitstreamUtility {
    
    fun createBitstream(data: EncryptedData, useHamming: Boolean): String {
        val payload = data.salt + data.nonce + data.ciphertext
        
        return if (useHamming) {
            val header = intToBytes(payload.size)
            HammingCode.encodeBytes(header + payload)
        } else {
            payload.joinToString("") { it.toInt().and(0xFF).toString(2).padStart(8, '0') }
        }
    }
    
    fun extractEncryptedData(bits: String, useHamming: Boolean): Pair<EncryptedData?, Int> {
        return if (useHamming) {
            try {
                val (bytes, errors) = HammingCode.decodeBytes(bits)
                val length = bytesToInt(bytes.sliceArray(0..3))
                val payload = bytes.sliceArray(4 until bytes.size)
                
                if (payload.size >= 28) {
                    Pair(EncryptedData(
                        payload.sliceArray(0..15),
                        payload.sliceArray(16..27),
                        payload.sliceArray(28 until payload.size)
                    ), errors)
                } else {
                    Pair(null, errors)
                }
            } catch (e: Exception) {
                Pair(null, 0)
            }
        } else {
            try {
                val bytes = bits.chunked(8).map { it.toInt(2).toByte() }.toByteArray()
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
    
    private fun intToBytes(value: Int) = ByteArray(4) { i -> 
        (value shr (24 - i * 8)).toByte() 
    }
    
    private fun bytesToInt(bytes: ByteArray) = 
        bytes.take(4).foldIndexed(0) { i, acc, b -> 
            acc or ((b.toInt() and 0xFF) shl (24 - i * 8)) 
        }
}
