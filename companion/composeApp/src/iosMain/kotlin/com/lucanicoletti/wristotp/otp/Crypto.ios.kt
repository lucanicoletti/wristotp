package com.lucanicoletti.wristotp.otp

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(data: ByteArray): ByteArray {
    val output = ByteArray(CC_SHA256_DIGEST_LENGTH)
    val input = if (data.isEmpty()) ByteArray(1) else data
    input.usePinned { pinnedInput ->
        output.usePinned { pinnedOutput ->
            CC_SHA256(pinnedInput.addressOf(0), data.size.convert(), pinnedOutput.addressOf(0).reinterpret())
        }
    }
    return output
}
