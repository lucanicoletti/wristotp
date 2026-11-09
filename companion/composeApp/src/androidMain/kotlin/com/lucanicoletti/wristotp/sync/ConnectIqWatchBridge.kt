package com.lucanicoletti.wristotp.sync

import android.content.Context
import com.garmin.android.connectiq.ConnectIQ
import com.garmin.android.connectiq.IQApp
import com.garmin.android.connectiq.IQDevice
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class ConnectIqWatchBridge(
    private val context: Context,
    connectType: ConnectIQ.IQConnectType,
) : WatchBridge {
    private val connectIQ = ConnectIQ.getInstance(context, connectType)
    private val replies = MutableSharedFlow<Map<*, *>>(extraBufferCapacity = 16)
    private val _state = MutableStateFlow<WatchState>(WatchState.Initializing)
    private var device: IQDevice? = null
    private var app: IQApp? = null

    override val state: StateFlow<WatchState> = _state.asStateFlow()

    fun start() {
        connectIQ.initialize(context, true, object : ConnectIQ.ConnectIQListener {
            override fun onSdkReady() {
                refreshDevices()
            }

            override fun onInitializeError(status: ConnectIQ.IQSdkErrorStatus) {
                _state.value = WatchState.Unavailable(
                    when (status) {
                        ConnectIQ.IQSdkErrorStatus.GCM_NOT_INSTALLED -> "Garmin Connect is not installed"
                        ConnectIQ.IQSdkErrorStatus.GCM_UPGRADE_NEEDED -> "Garmin Connect needs an update"
                        ConnectIQ.IQSdkErrorStatus.SERVICE_ERROR -> "Garmin Connect service error"
                    },
                )
            }

            override fun onSdkShutDown() {
                _state.value = WatchState.Unavailable("Connect IQ shut down")
            }
        })
    }

    override suspend fun send(syncId: Int, messages: List<WatchMessage>): SyncResult {
        val targetDevice = device ?: return SyncResult.Failed("No watch connected")
        val targetApp = app ?: return SyncResult.Failed("WristOTP is not installed on the watch")
        return deliverWithAcknowledgement(syncId, messages, replies, ACK_TIMEOUT_MILLIS) { payload ->
            val status = sendMessage(targetDevice, targetApp, payload)
            if (status == ConnectIQ.IQMessageStatus.SUCCESS) null else status.name
        }
    }

    private fun refreshDevices() {
        val devices = runCatching { connectIQ.knownDevices.orEmpty() }.getOrElse {
            _state.value = WatchState.Unavailable(it.message ?: "Could not list watches")
            return
        }
        if (devices.isEmpty()) {
            _state.value = WatchState.NoDevice
            return
        }
        devices.forEach { known ->
            connectIQ.registerForDeviceEvents(known) { changed, status -> onDeviceStatusChanged(changed, status) }
        }
        val connected = devices.firstOrNull { connectIQ.getDeviceStatus(it) == IQDevice.IQDeviceStatus.CONNECTED }
        if (connected == null) {
            _state.value = WatchState.Disconnected(devices.first().friendlyName)
        } else {
            selectDevice(connected)
        }
    }

    private fun onDeviceStatusChanged(changed: IQDevice, status: IQDevice.IQDeviceStatus) {
        val current = device
        when {
            status == IQDevice.IQDeviceStatus.CONNECTED && (current == null || current == changed) -> selectDevice(changed)
            status != IQDevice.IQDeviceStatus.CONNECTED && current == changed -> {
                device = null
                app = null
                _state.value = WatchState.Disconnected(changed.friendlyName)
            }
        }
    }

    private fun selectDevice(selected: IQDevice) {
        device = selected
        _state.value = WatchState.Checking(selected.friendlyName)
        findApp(selected, WATCH_APP_IDS.keys.toList())
    }

    private fun findApp(selected: IQDevice, editions: List<WatchEdition>) {
        val edition = editions.first()
        connectIQ.getApplicationInfo(WATCH_APP_IDS.getValue(edition), selected, object : ConnectIQ.IQApplicationInfoListener {
            override fun onApplicationInfoReceived(info: IQApp) {
                app = info
                connectIQ.registerForAppEvents(selected, info) { _, _, payload, _ ->
                    payload.filterIsInstance<Map<*, *>>().forEach { replies.tryEmit(it) }
                }
                _state.value = WatchState.Ready(selected.friendlyName, edition)
            }

            override fun onApplicationNotInstalled(applicationId: String) {
                if (editions.size > 1) {
                    findApp(selected, editions.drop(1))
                    return
                }
                app = null
                _state.value = WatchState.AppNotInstalled(selected.friendlyName)
            }
        })
    }

    private suspend fun sendMessage(target: IQDevice, targetApp: IQApp, payload: Any): ConnectIQ.IQMessageStatus =
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    connectIQ.sendMessage(target, targetApp, payload) { _, _, status ->
                        if (continuation.isActive) continuation.resume(status)
                    }
                }.onFailure {
                    if (continuation.isActive) continuation.resume(ConnectIQ.IQMessageStatus.FAILURE_UNKNOWN)
                }
            }
        }

    private companion object {
        val WATCH_APP_IDS = mapOf(
            WatchEdition.Full to "5bdabd0de07e48e1a7c103ee7a3d006c",
            WatchEdition.Free to "c4cc401eebcc4caeab763d52f85af9af",
        )
        const val ACK_TIMEOUT_MILLIS = 15_000L
    }
}
