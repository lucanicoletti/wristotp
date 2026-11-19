package com.lucanicoletti.wristotp.sync

import kotlinx.coroutines.flow.StateFlow

interface WatchBridge {
    val state: StateFlow<WatchState>

    val canChooseDevice: Boolean
        get() = false

    fun chooseDevice() {}

    suspend fun send(syncId: Int, messages: List<WatchMessage>): SyncResult
}

sealed interface WatchState {
    data object Initializing : WatchState
    data class Unavailable(val reason: String) : WatchState
    data object NoDevice : WatchState
    data class Disconnected(val deviceName: String) : WatchState
    data class Checking(val deviceName: String) : WatchState
    data class AppNotInstalled(val deviceName: String) : WatchState
    data class Ready(val deviceName: String) : WatchState
}

const val WATCH_ACCOUNT_LIMIT = 50

sealed interface SyncResult {
    data object Confirmed : SyncResult
    data object Unconfirmed : SyncResult
    data class Rejected(val reason: String) : SyncResult
    data class Failed(val reason: String) : SyncResult
}
