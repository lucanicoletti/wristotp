package com.lucanicoletti.wristotp.otp

enum class OtpAlgorithm(val wireValue: Int) {
    SHA1(1),
    SHA256(2),
}

class OtpAccount(
    val issuer: String,
    val name: String,
    val secret: ByteArray,
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: Int = 6,
    val period: Int = 30,
) {
    override fun equals(other: Any?): Boolean =
        other is OtpAccount &&
            issuer == other.issuer &&
            name == other.name &&
            secret.contentEquals(other.secret) &&
            algorithm == other.algorithm &&
            digits == other.digits &&
            period == other.period

    override fun hashCode(): Int {
        var result = issuer.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + secret.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + digits
        result = 31 * result + period
        return result
    }

    override fun toString(): String = "OtpAccount(issuer=$issuer, name=$name, algorithm=$algorithm, digits=$digits, period=$period)"
}

enum class UnsupportedReason {
    HOTP,
    ALGORITHM,
    DIGITS,
    PERIOD,
    INVALID_SECRET,
}

sealed interface ImportedAccount {
    data class Supported(val account: OtpAccount) : ImportedAccount
    data class Unsupported(val issuer: String, val name: String, val reason: UnsupportedReason) : ImportedAccount
}
