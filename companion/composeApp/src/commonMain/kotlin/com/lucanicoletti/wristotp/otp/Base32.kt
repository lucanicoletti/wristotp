package com.lucanicoletti.wristotp.otp

object Base32 {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"

    fun decode(text: String): ByteArray? {
        val output = ArrayList<Byte>(text.length * 5 / 8)
        var buffer = 0
        var bits = 0
        for (char in text.uppercase()) {
            if (char == ' ' || char == '-' || char == '=') continue
            val value = ALPHABET.indexOf(char)
            if (value < 0) return null
            buffer = (buffer shl 5) or value
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output.add(((buffer shr bits) and 0xFF).toByte())
                buffer = buffer and ((1 shl bits) - 1)
            }
        }
        return output.toByteArray()
    }

    fun encode(bytes: ByteArray): String {
        val output = StringBuilder((bytes.size * 8 + 4) / 5)
        var buffer = 0
        var bits = 0
        for (byte in bytes) {
            buffer = (buffer shl 8) or (byte.toInt() and 0xFF)
            bits += 8
            while (bits >= 5) {
                bits -= 5
                output.append(ALPHABET[(buffer shr bits) and 0x1F])
            }
            buffer = buffer and ((1 shl bits) - 1)
        }
        if (bits > 0) {
            output.append(ALPHABET[(buffer shl (5 - bits)) and 0x1F])
        }
        return output.toString()
    }
}
