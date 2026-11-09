package com.lucanicoletti.wristotp.otp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OtpAuthUriTest {
    private val seed = "12345678901234567890".encodeToByteArray()

    @Test
    fun parsesFullUri() {
        val result = OtpAuthUri.parse("otpauth://totp/ACME%20Co:jane%40example.com?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=ACME%20Co&algorithm=SHA256&digits=8&period=60")
        assertEquals(
            ImportedAccount.Supported(OtpAccount("ACME Co", "jane@example.com", seed, OtpAlgorithm.SHA256, 8, 60)),
            result,
        )
    }

    @Test
    fun appliesDefaultsAndLabelIssuer() {
        val result = OtpAuthUri.parse("otpauth://totp/GitHub:demo?secret=gezdgnbvgy3tqojqgezdgnbvgy3tqojq")
        assertEquals(ImportedAccount.Supported(OtpAccount("GitHub", "demo", seed)), result)
    }

    @Test
    fun issuerParameterWinsOverLabel() {
        val result = OtpAuthUri.parse("otpauth://totp/Old:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ&issuer=New")
        assertEquals(ImportedAccount.Supported(OtpAccount("New", "demo", seed)), result)
    }

    @Test
    fun labelWithoutIssuer() {
        val result = OtpAuthUri.parse("otpauth://totp/demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        assertEquals(ImportedAccount.Supported(OtpAccount("", "demo", seed)), result)
    }

    @Test
    fun flagsUnsupportedAccounts() {
        assertEquals(
            ImportedAccount.Unsupported("Bank", "demo", UnsupportedReason.HOTP),
            OtpAuthUri.parse("otpauth://hotp/Bank:demo?secret=GEZDGNBVGY3TQOJQ&counter=1"),
        )
        assertEquals(
            ImportedAccount.Unsupported("X", "demo", UnsupportedReason.ALGORITHM),
            OtpAuthUri.parse("otpauth://totp/X:demo?secret=GEZDGNBVGY3TQOJQ&algorithm=SHA512"),
        )
        assertEquals(
            ImportedAccount.Unsupported("X", "demo", UnsupportedReason.DIGITS),
            OtpAuthUri.parse("otpauth://totp/X:demo?secret=GEZDGNBVGY3TQOJQ&digits=10"),
        )
        assertEquals(
            ImportedAccount.Unsupported("X", "demo", UnsupportedReason.INVALID_SECRET),
            OtpAuthUri.parse("otpauth://totp/X:demo?secret=not-base32!"),
        )
        assertEquals(
            ImportedAccount.Unsupported("X", "demo", UnsupportedReason.INVALID_SECRET),
            OtpAuthUri.parse("otpauth://totp/X:demo"),
        )
    }

    @Test
    fun rejectsOtherUris() {
        assertNull(OtpAuthUri.parse("https://example.com"))
        assertNull(OtpAuthUri.parse("otpauth://unknown/X:demo?secret=GEZDGNBVGY3TQOJQ"))
        assertNull(OtpAuthUri.parse("otpauth://totp"))
    }
}
