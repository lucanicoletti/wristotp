package com.lucanicoletti.wristotp.sync

import com.lucanicoletti.wristotp.otp.Base32
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.sha256

sealed interface WatchMessage {
    fun toWire(): Map<String, Any>

    data class Begin(val syncId: Int, val total: Int, val pinRequired: Boolean) : WatchMessage {
        override fun toWire(): Map<String, Any> = mapOf(
            "op" to "begin",
            "v" to WatchProtocol.VERSION,
            "id" to syncId,
            "total" to total,
            "pinRequired" to pinRequired,
        )
    }

    data class Chunk(val syncId: Int, val index: Int, val accounts: List<Map<String, Any>>) : WatchMessage {
        override fun toWire(): Map<String, Any> = mapOf(
            "op" to "chunk",
            "id" to syncId,
            "idx" to index,
            "accounts" to accounts,
        )
    }

    data class Commit(val syncId: Int, val hash: String) : WatchMessage {
        override fun toWire(): Map<String, Any> = mapOf(
            "op" to "commit",
            "id" to syncId,
            "hash" to hash,
        )
    }
}

object WatchProtocol {
    const val VERSION = 1
    const val DEFAULT_CHUNK_SIZE = 5

    fun messages(
        accounts: List<OtpAccount>,
        syncId: Int,
        pinRequired: Boolean,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
    ): List<WatchMessage> {
        val records = accounts.map { it.toRecord() }
        val chunks = records.chunked(chunkSize).mapIndexed { index, chunk -> WatchMessage.Chunk(syncId, index, chunk) }
        return buildList {
            add(WatchMessage.Begin(syncId, chunks.size, pinRequired))
            addAll(chunks)
            add(WatchMessage.Commit(syncId, hash(records)))
        }
    }

    fun hash(records: List<Map<String, Any>>): String {
        val canonical = records.joinToString(separator = "") { record ->
            "${record["s"]}|${record["a"]}|${record["d"]}|${record["p"]}\n"
        }
        return sha256(canonical.encodeToByteArray()).toHex()
    }

    private fun OtpAccount.toRecord(): Map<String, Any> = mapOf(
        "i" to issuer,
        "n" to name,
        "s" to Base32.encode(secret),
        "a" to algorithm.wireValue,
        "d" to digits,
        "p" to period,
    )

    private fun ByteArray.toHex(): String = joinToString(separator = "") { byte ->
        (byte.toInt() and 0xFF).toString(16).padStart(2, '0')
    }
}
