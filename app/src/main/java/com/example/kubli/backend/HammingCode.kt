package com.example.kubli.backend

//Hamming(7,4) error correcting code

object HammingCode {

    // Encodes a byte array into a Hamming protected bit string
    fun encodeBytes(data: ByteArray): String = buildString {
        data.forEach { byte ->
            // Convert byte into 8 individual bits
            val bits = (0..7).map { (byte.toInt() shr (7 - it)) and 1 == 1 }
            // Encode first 4 bits and second 4 bits separately
            append(encode4(bits.take(4)))
            append(encode4(bits.drop(4)))
        }
    }

    // Decodes a Hamming encoded bit string back into bytes
    fun decodeBytes(bits: String): Pair<ByteArray, Int> {
        // Each byte is encoded as 14 bits
        require(bits.length % 14 == 0) { "Invalid Hamming bit length" }

        val bytes = ByteArray(bits.length / 14)
        var errors = 0

        // Process each 14-bit block
        bits.chunked(14).forEachIndexed { i, chunk ->
            // Decode the two 7-bit Hamming blocks
            val (first, e1) = decode7(chunk.take(7))
            val (second, e2) = decode7(chunk.drop(7))
            // Count corrected errors
            errors += if (e1 > 0) 1 else 0
            errors += if (e2 > 0) 1 else 0

            // Reconstruct original byte from decoded bits
            bytes[i] = ((0..3).sumOf { if (first[it]) 1 shl (7 - it) else 0 } +
                       (0..3).sumOf { if (second[it]) 1 shl (3 - it) else 0 }).toByte()
        }
        
        return Pair(bytes, errors)
    }

    // Encodes 4 data bits into a 7-bit Hamming block
    private fun encode4(bits: List<Boolean>): String {
        // Data bits
        val d = bits
        // Calculate parity bits
        val p1 = d[0] xor d[1] xor d[3]
        val p2 = d[0] xor d[2] xor d[3]
        val p4 = d[1] xor d[2] xor d[3]
        // Return 7-bit Hamming code
        return listOf(p1, p2, d[0], p4, d[1], d[2], d[3]).joinToString("") { if (it) "1" else "0" }
    }

    // Decodes a 7-bit Hamming block and corrects one error if present
    private fun decode7(bits: String): Pair<List<Boolean>, Int> {
        // Convert bit string to boolean list
        val b = bits.map { it == '1' }.toMutableList()
        // Compute syndrome bits
        val s1 = b[0] xor b[2] xor b[4] xor b[6]
        val s2 = b[1] xor b[2] xor b[5] xor b[6]
        val s4 = b[3] xor b[4] xor b[5] xor b[6]
        // Determine error position
        val error = (if (s4) 4 else 0) + (if (s2) 2 else 0) + (if (s1) 1 else 0)

        // Correct the bit if an error is detected
        if (error > 0) b[error - 1] = !b[error - 1]

        // Return corrected data bits and error indicator
        return Pair(listOf(b[2], b[4], b[5], b[6]), error)
    }
}
