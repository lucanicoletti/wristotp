package com.lucanicoletti.wristotp.sync

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withTimeoutOrNull

internal suspend fun deliverWithAcknowledgement(
    syncId: Int,
    messages: List<WatchMessage>,
    replies: Flow<Map<*, *>>,
    timeoutMillis: Long,
    sendMessage: suspend (Map<String, Any>) -> String?,
): SyncResult = coroutineScope {
    val ack = async(start = CoroutineStart.UNDISPATCHED) {
        withTimeoutOrNull(timeoutMillis) {
            replies.firstOrNull { it["op"] == "ack" && (it["id"] as? Number)?.toInt() == syncId }
        }
    }
    for (message in messages) {
        val failure = sendMessage(message.toWire())
        if (failure != null) {
            ack.cancel()
            return@coroutineScope SyncResult.Failed(failure)
        }
    }
    val reply = ack.await() ?: return@coroutineScope SyncResult.Unconfirmed
    if (reply["ok"] == true) SyncResult.Confirmed else SyncResult.Rejected(reply["err"]?.toString() ?: "unknown error")
}
