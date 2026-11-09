package com.lucanicoletti.wristotp.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UnavailableWatchBridge(reason: String) : WatchBridge {
    override val state: StateFlow<WatchState> = MutableStateFlow(WatchState.Unavailable(reason))

    override suspend fun send(syncId: Int, messages: List<WatchMessage>): SyncResult = SyncResult.Failed("Watch connection is not available")
}
