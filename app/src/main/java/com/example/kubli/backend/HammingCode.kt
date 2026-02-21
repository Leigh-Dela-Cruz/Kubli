package com.example.kubli.backend

//Hamming(7,4) error correcting code

object HammingCode {
    
    fun encodeBytes(data: ByteArray): String = buildString {
        data.forEach { byte ->
            val bits = (0..7).map { (byte.toInt() shr (7 - it)) and 1 == 1 }
            append(encode4(bits.take(4)))
            append(encode4(bits.drop(4)))
        }
    }
    
    fun decodeBytes(bits: String): Pair<ByteArray, Int> {
        require(bits.length % 14 == 0) { "Invalid Hamming bit length" }
        
        val bytes = ByteArray(bits.length / 14)
        var errors = 0
        
        bits.chunked(14).forEachIndexed { i, chunk ->
            val (first, e1) = decode7(chunk.take(7))
            val (second, e2) = decode7(chunk.drop(7))
            errors += if (e1 > 0) 1 else 0
            errors += if (e2 > 0) 1 else 0
            
            bytes[i] = ((0..3).sumOf { if (first[it]) 1 shl (7 - it) else 0 } +
                       (0..3).sumOf { if (second[it]) 1 shl (3 - it) else 0 }).toByte()
        }
        
        return Pair(bytes, errors)
    }
    
    private fun encode4(bits: List<Boolean>): String {
        val d = bits
        val p1 = d[0] xor d[1] xor d[3]
        val p2 = d[0] xor d[2] xor d[3]
        val p4 = d[1] xor d[2] xor d[3]
        return listOf(p1, p2, d[0], p4, d[1], d[2], d[3]).joinToString("") { if (it) "1" else "0" }
    }
    
    private fun decode7(bits: String): Pair<List<Boolean>, Int> {
        val b = bits.map { it == '1' }.toMutableList()
        val s1 = b[0] xor b[2] xor b[4] xor b[6]
        val s2 = b[1] xor b[2] xor b[5] xor b[6]
        val s4 = b[3] xor b[4] xor b[5] xor b[6]
        val error = (if (s4) 4 else 0) + (if (s2) 2 else 0) + (if (s1) 1 else 0)
        
        if (error > 0) b[error - 1] = !b[error - 1]
        
        return Pair(listOf(b[2], b[4], b[5], b[6]), error)
    }
}
