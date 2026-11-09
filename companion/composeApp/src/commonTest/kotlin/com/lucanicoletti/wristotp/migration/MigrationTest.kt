package com.lucanicoletti.wristotp.migration

import com.lucanicoletti.wristotp.otp.ImportedAccount
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.OtpAlgorithm
import com.lucanicoletti.wristotp.otp.UnsupportedReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class MigrationTest {
    private val sha1Seed = "12345678901234567890".encodeToByteArray()
    private val sha256Seed = "12345678901234567890123456789012".encodeToByteArray()

    @Test
    fun decodesSingleExport() {
        val batch = MigrationUri.parse(SINGLE)
        assertEquals(
            MigrationBatch(
                batchId = 123456,
                batchIndex = 0,
                batchSize = 1,
                accounts = listOf(
                    ImportedAccount.Supported(OtpAccount("GitHub", "demo@example.com", sha1Seed)),
                    ImportedAccount.Supported(OtpAccount("Example", "8 digits", sha256Seed, OtpAlgorithm.SHA256, digits = 8)),
                    ImportedAccount.Unsupported("Bank", "counter", UnsupportedReason.HOTP),
                    ImportedAccount.Unsupported("OldCo", "legacy", UnsupportedReason.ALGORITHM),
                ),
            ),
            batch,
        )
    }

    @Test
    fun rejectsMalformedUris() {
        assertNull(MigrationUri.parse("otpauth://totp/X?secret=GEZDGNBV"))
        assertNull(MigrationUri.parse("otpauth-migration://offline?foo=bar"))
        assertNull(MigrationUri.parse("otpauth-migration://offline?data=%%%not-base64"))
    }

    @Test
    fun collectsSplitExportInAnyOrder() {
        val collector = MigrationCollector()
        assertEquals(MigrationCollector.Progress.Waiting(scanned = 1, total = 2), collector.add(MigrationUri.parse(BATCH_1)!!))
        assertEquals(MigrationCollector.Progress.Waiting(scanned = 1, total = 2), collector.add(MigrationUri.parse(BATCH_1)!!))
        val complete = assertIs<MigrationCollector.Progress.Complete>(collector.add(MigrationUri.parse(BATCH_0)!!))
        assertEquals(
            listOf(
                ImportedAccount.Supported(OtpAccount("Alpha", "first", sha1Seed)),
                ImportedAccount.Supported(OtpAccount("Beta", "second", sha256Seed, OtpAlgorithm.SHA256)),
            ),
            complete.accounts,
        )
    }

    @Test
    fun rejectsCodeFromDifferentExport() {
        val collector = MigrationCollector()
        collector.add(MigrationUri.parse(BATCH_0)!!)
        assertEquals(MigrationCollector.Progress.DifferentExport, collector.add(MigrationUri.parse(OTHER_EXPORT)!!))
        collector.reset()
        assertEquals(MigrationCollector.Progress.Waiting(scanned = 1, total = 2), collector.add(MigrationUri.parse(OTHER_EXPORT)!!))
    }

    @Test
    fun singleExportCompletesImmediately() {
        val progress = MigrationCollector().add(MigrationUri.parse(SINGLE)!!)
        assertEquals(4, assertIs<MigrationCollector.Progress.Complete>(progress).accounts.size)
    }

    private companion object {
        const val SINGLE = "otpauth-migration://offline?data=Cj0KFDEyMzQ1Njc4OTAxMjM0NTY3ODkwEhdHaXRIdWI6ZGVtb0BleGFtcGxlLmNvbRoGR2l0SHViIAEoATACCjsKIDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyEgg4IGRpZ2l0cxoHRXhhbXBsZSACKAIwAgorChQxMjM0NTY3ODkwMTIzNDU2Nzg5MBIHY291bnRlchoEQmFuayABKAEwAQorChQxMjM0NTY3ODkwMTIzNDU2Nzg5MBIGbGVnYWN5GgVPbGRDbyAEKAEwAhABGAEgACjAxAc%3D"
        const val BATCH_0 = "otpauth-migration://offline?data=CioKFDEyMzQ1Njc4OTAxMjM0NTY3ODkwEgVmaXJzdBoFQWxwaGEgASgBMAIQARgCIAAoiQY%3D"
        const val BATCH_1 = "otpauth-migration://offline?data=CjYKIDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyEgZzZWNvbmQaBEJldGEgAigBMAIQARgCIAEoiQY%3D"
        const val OTHER_EXPORT = "otpauth-migration://offline?data=CiYKFDEyMzQ1Njc4OTAxMjM0NTY3ODkwEgF4GgVPdGhlciABKAEwAhABGAIgACjnBw%3D%3D"
    }
}
