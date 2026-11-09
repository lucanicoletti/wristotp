package com.lucanicoletti.wristotp.migration

import com.lucanicoletti.wristotp.otp.ImportedAccount
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.OtpAlgorithm
import com.lucanicoletti.wristotp.otp.UnsupportedReason
import com.lucanicoletti.wristotp.otp.queryParameters
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromByteArray
import kotlinx.serialization.protobuf.ProtoBuf

data class MigrationBatch(
    val batchId: Int,
    val batchIndex: Int,
    val batchSize: Int,
    val accounts: List<ImportedAccount>,
)

object MigrationUri {
    private const val PREFIX = "otpauth-migration://offline?"
    private const val ALGORITHM_SHA256 = 2
    private const val DIGITS_EIGHT = 2
    private const val TYPE_HOTP = 1

    @OptIn(ExperimentalEncodingApi::class, ExperimentalSerializationApi::class)
    fun parse(uri: String): MigrationBatch? {
        if (!uri.startsWith(PREFIX, ignoreCase = true)) return null
        val data = uri.substring(PREFIX.length).queryParameters()["data"] ?: return null
        val payload = runCatching {
            ProtoBuf.decodeFromByteArray<MigrationPayload>(Base64.Default.withPadding(Base64.PaddingOption.PRESENT_OPTIONAL).decode(data.replace(' ', '+')))
        }.getOrNull() ?: return null
        return MigrationBatch(
            batchId = payload.batchId,
            batchIndex = payload.batchIndex,
            batchSize = payload.batchSize.coerceAtLeast(1),
            accounts = payload.otpParameters.map { it.toImportedAccount() },
        )
    }

    private fun OtpParameters.toImportedAccount(): ImportedAccount {
        val displayName = name.removePrefix("$issuer:").trim()
        val algorithm = when (algorithm) {
            0, 1 -> OtpAlgorithm.SHA1
            ALGORITHM_SHA256 -> OtpAlgorithm.SHA256
            else -> null
        }
        return when {
            type == TYPE_HOTP -> ImportedAccount.Unsupported(issuer, displayName, UnsupportedReason.HOTP)
            algorithm == null -> ImportedAccount.Unsupported(issuer, displayName, UnsupportedReason.ALGORITHM)
            secret.isEmpty() -> ImportedAccount.Unsupported(issuer, displayName, UnsupportedReason.INVALID_SECRET)
            else -> ImportedAccount.Supported(
                OtpAccount(
                    issuer = issuer,
                    name = displayName,
                    secret = secret,
                    algorithm = algorithm,
                    digits = if (digits == DIGITS_EIGHT) 8 else 6,
                ),
            )
        }
    }
}
