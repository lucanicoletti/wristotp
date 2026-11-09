package com.lucanicoletti.wristotp.otp

internal fun String.percentDecoded(plusAsSpace: Boolean): String {
    val bytes = ArrayList<Byte>(length)
    var index = 0
    while (index < length) {
        val char = this[index]
        val hexValue = if (char == '%' && index + 2 <= lastIndex) substring(index + 1, index + 3).toIntOrNull(16) else null
        when {
            hexValue != null -> {
                bytes.add(hexValue.toByte())
                index += 3
            }
            char == '+' && plusAsSpace -> {
                bytes.add(' '.code.toByte())
                index++
            }
            else -> {
                bytes.addAll(char.toString().encodeToByteArray().toList())
                index++
            }
        }
    }
    return bytes.toByteArray().decodeToString()
}

internal fun String.queryParameters(): Map<String, String> =
    split('&')
        .filter { it.isNotEmpty() }
        .associate { pair ->
            val separator = pair.indexOf('=')
            if (separator < 0) {
                pair.percentDecoded(plusAsSpace = true).lowercase() to ""
            } else {
                pair.substring(0, separator).percentDecoded(plusAsSpace = true).lowercase() to
                    pair.substring(separator + 1).percentDecoded(plusAsSpace = true)
            }
        }
