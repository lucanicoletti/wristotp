package com.lucanicoletti.wristotp.sync

import kotlin.coroutines.resume
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSMutableArray
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSNumber
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.numberWithBool
import platform.Foundation.numberWithInt

class IosWatchBridge(private val transport: IosWatchTransport) : WatchBridge, IosWatchTransportListener {
    private val replies = MutableSharedFlow<Map<*, *>>(extraBufferCapacity = 16)
    private val _state = MutableStateFlow<WatchState>(WatchState.Initializing)

    override val state: StateFlow<WatchState> = _state.asStateFlow()
    override val canChooseDevice: Boolean = true

    init {
        transport.start(this)
    }

    override fun chooseDevice() {
        transport.chooseDevice()
    }

    override suspend fun send(syncId: Int, messages: List<WatchMessage>): SyncResult {
        if (_state.value !is WatchState.Ready) return SyncResult.Failed("No watch ready")
        return deliverWithAcknowledgement(syncId, messages, replies, ACK_TIMEOUT_MILLIS) { payload ->
            suspendCancellableCoroutine { continuation ->
                transport.send(payload.toFoundation()) { failure ->
                    if (continuation.isActive) continuation.resume(failure)
                }
            }
        }
    }

    override fun onUnavailable(reason: String) {
        _state.value = WatchState.Unavailable(reason)
    }

    override fun onNoDevice() {
        _state.value = WatchState.NoDevice
    }

    override fun onDisconnected(deviceName: String) {
        _state.value = WatchState.Disconnected(deviceName)
    }

    override fun onChecking(deviceName: String) {
        _state.value = WatchState.Checking(deviceName)
    }

    override fun onAppNotInstalled(deviceName: String) {
        _state.value = WatchState.AppNotInstalled(deviceName)
    }

    override fun onReady(deviceName: String) {
        _state.value = WatchState.Ready(deviceName)
    }

    override fun onMessage(payload: Any?) {
        val messages = if (payload is List<*>) payload else listOf(payload)
        messages.filterIsInstance<Map<*, *>>().forEach { replies.tryEmit(it.normalized()) }
    }

    private fun Map<*, *>.normalized(): Map<String, Any?> = mapOf(
        "op" to this["op"]?.toString(),
        "id" to ((this["id"] as? NSNumber)?.intValue ?: (this["id"] as? Number)?.toInt()),
        "ok" to ((this["ok"] as? NSNumber)?.boolValue ?: (this["ok"] == true)),
        "err" to this["err"]?.toString(),
    )

    private fun Any.toFoundation(): Any = when (this) {
        is Boolean -> NSNumber.numberWithBool(this)
        is Int -> NSNumber.numberWithInt(this)
        is Map<*, *> -> NSMutableDictionary().also { dictionary ->
            forEach { (key, value) -> if (key != null && value != null) dictionary.setObject(value.toFoundation(), forKey = NSString.create(string = key.toString())) }
        }
        is List<*> -> NSMutableArray().also { array -> filterNotNull().forEach { array.addObject(it.toFoundation()) } }
        else -> this
    }

    private companion object {
        const val ACK_TIMEOUT_MILLIS = 15_000L
    }
}
