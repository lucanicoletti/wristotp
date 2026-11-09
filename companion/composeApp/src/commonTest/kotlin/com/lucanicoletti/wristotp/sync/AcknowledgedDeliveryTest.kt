package com.lucanicoletti.wristotp.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest

class AcknowledgedDeliveryTest {
    private val messages = WatchProtocol.messages(emptyList(), syncId = 5, pinRequired = false)

    @Test
    fun confirmedWhenWatchAcknowledgesThisSync() = runTest {
        val replies = MutableSharedFlow<Map<*, *>>(extraBufferCapacity = 4)
        val sent = mutableListOf<Map<String, Any>>()
        val result = deliverWithAcknowledgement(5, messages, replies, 1_000) { message ->
            sent += message
            if (message["op"] == "commit") {
                replies.tryEmit(mapOf("op" to "ack", "id" to 4, "ok" to true))
                replies.tryEmit(mapOf("op" to "ack", "id" to 5, "ok" to true))
            }
            null
        }
        assertEquals(SyncResult.Confirmed, result)
        assertEquals(listOf("begin", "commit"), sent.map { it["op"] })
    }

    @Test
    fun rejectedCarriesWatchError() = runTest {
        val replies = MutableSharedFlow<Map<*, *>>(extraBufferCapacity = 4)
        val result = deliverWithAcknowledgement(5, messages, replies, 1_000) { message ->
            if (message["op"] == "commit") replies.tryEmit(mapOf("op" to "ack", "id" to 5, "ok" to false, "err" to "hash mismatch"))
            null
        }
        assertEquals(SyncResult.Rejected("hash mismatch"), result)
    }

    @Test
    fun failedStopsAtFirstTransportError() = runTest {
        val sent = mutableListOf<Any?>()
        val result = deliverWithAcknowledgement(5, messages, emptyFlow(), 1_000) { message ->
            sent += message["op"]
            "DeviceNotAvailable"
        }
        assertEquals(SyncResult.Failed("DeviceNotAvailable"), result)
        assertEquals(listOf<Any?>("begin"), sent)
    }

    @Test
    fun unconfirmedWhenWatchStaysSilent() = runTest {
        val result = deliverWithAcknowledgement(5, messages, MutableSharedFlow(), 15_000) { null }
        assertEquals(SyncResult.Unconfirmed, result)
    }
}
