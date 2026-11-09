package com.lucanicoletti.wristotp.otp

object OtpAuthUri {
    private const val SCHEME = "otpauth://"

    fun parse(uri: String): ImportedAccount? {
        if (!uri.startsWith(SCHEME, ignoreCase = true)) return null
        val remainder = uri.substring(SCHEME.length)
        val typeEnd = remainder.indexOf('/')
        if (typeEnd < 0) return null
        val type = remainder.substring(0, typeEnd).lowercase()
        val queryStart = remainder.indexOf('?', typeEnd)
        val label = remainder.substring(typeEnd + 1, if (queryStart < 0) remainder.length else queryStart)
            .percentDecoded(plusAsSpace = false)
        val parameters = if (queryStart < 0) emptyMap() else remainder.substring(queryStart + 1).queryParameters()

        val labelSeparator = label.indexOf(':')
        val labelIssuer = if (labelSeparator < 0) "" else label.substring(0, labelSeparator).trim()
        val name = if (labelSeparator < 0) label.trim() else label.substring(labelSeparator + 1).trim()
        val issuer = parameters["issuer"]?.trim()?.takeIf { it.isNotEmpty() } ?: labelIssuer

        return when {
            type == "hotp" -> ImportedAccount.Unsupported(issuer, name, UnsupportedReason.HOTP)
            type != "totp" -> null
            else -> parseTotp(issuer, name, parameters)
        }
    }

    private fun parseTotp(issuer: String, name: String, parameters: Map<String, String>): ImportedAccount {
        val secret = parameters["secret"]?.let { Base32.decode(it) }
        if (secret == null || secret.isEmpty()) {
            return ImportedAccount.Unsupported(issuer, name, UnsupportedReason.INVALID_SECRET)
        }
        val algorithm = when (parameters["algorithm"]?.uppercase()) {
            null, "SHA1" -> OtpAlgorithm.SHA1
            "SHA256" -> OtpAlgorithm.SHA256
            else -> return ImportedAccount.Unsupported(issuer, name, UnsupportedReason.ALGORITHM)
        }
        val digits = parameters["digits"]?.toIntOrNull() ?: 6
        if (digits !in 6..8) {
            return ImportedAccount.Unsupported(issuer, name, UnsupportedReason.DIGITS)
        }
        val period = parameters["period"]?.toIntOrNull() ?: 30
        if (period !in 1..3600) {
            return ImportedAccount.Unsupported(issuer, name, UnsupportedReason.PERIOD)
        }
        return ImportedAccount.Supported(OtpAccount(issuer, name, secret, algorithm, digits, period))
    }
}
