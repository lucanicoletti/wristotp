package com.lucanicoletti.wristotp.sync

import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.OtpAlgorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class WatchProtocolTest {
    private val sha1Account = OtpAccount("GitHub", "demo", "12345678901234567890".encodeToByteArray())
    private val sha256Account = OtpAccount("Example", "8 digits", "12345678901234567890123456789012".encodeToByteArray(), OtpAlgorithm.SHA256, digits = 8)

    @Test
    fun wrapsChunksBetweenBeginAndCommit() {
        val accounts = List(12) { sha1Account }
        val messages = WatchProtocol.messages(accounts, syncId = 42, pinRequired = true, chunkSize = 5)

        assertEquals(WatchMessage.Begin(syncId = 42, total = 3, pinRequired = true), messages.first())
        val chunks = messages.drop(1).dropLast(1).map { assertIs<WatchMessage.Chunk>(it) }
        assertEquals(listOf(0, 1, 2), chunks.map { it.index })
        assertEquals(listOf(5, 5, 2), chunks.map { it.accounts.size })
        assertIs<WatchMessage.Commit>(messages.last())
    }

    @Test
    fun encodesRecordsWithWatchKeys() {
        val chunk = WatchProtocol.messages(listOf(sha256Account), syncId = 1, pinRequired = false)[1]
        assertEquals(
            mapOf(
                "op" to "chunk",
                "id" to 1,
                "idx" to 0,
                "accounts" to listOf(
                    mapOf(
                        "i" to "Example",
                        "n" to "8 digits",
                        "s" to "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA",
                        "a" to 2,
                        "d" to 8,
                        "p" to 30,
                    ),
                ),
            ),
            chunk.toWire(),
        )
    }

    @Test
    fun commitHashMatchesCanonicalForm() {
        val messages = WatchProtocol.messages(listOf(sha1Account, sha256Account), syncId = 7, pinRequired = false)
        assertEquals(
            WatchMessage.Commit(syncId = 7, hash = "d775e0cef4199700880e864acb242831133cd0bc0c0bcde360f5235bd7eb215d"),
            messages.last(),
        )
    }

    @Test
    fun emptySyncStillBeginsAndCommits() {
        val messages = WatchProtocol.messages(emptyList(), syncId = 3, pinRequired = false)
        assertEquals(
            listOf(
                WatchMessage.Begin(syncId = 3, total = 0, pinRequired = false),
                WatchMessage.Commit(syncId = 3, hash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
            ),
            messages,
        )
    }
}
