package com.lucanicoletti.wristotp.sync

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lucanicoletti.wristotp.migration.MigrationCollector
import com.lucanicoletti.wristotp.migration.MigrationUri
import com.lucanicoletti.wristotp.otp.ImportedAccount
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.OtpAuthUri
import com.lucanicoletti.wristotp.settings.InMemoryPhoneSettings
import com.lucanicoletti.wristotp.settings.LastSync
import com.lucanicoletti.wristotp.settings.PhoneSettings
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ScanStatus {
    data object Idle : ScanStatus
    data class Collecting(val scanned: Int, val total: Int) : ScanStatus
    data object DifferentExport : ScanStatus
    data object Unrecognised : ScanStatus
    data object NoQrInImage : ScanStatus
}

data class SyncUiState(
    val scanning: Boolean = false,
    val scanStatus: ScanStatus = ScanStatus.Idle,
    val accounts: List<OtpAccount> = emptyList(),
    val unsupported: List<ImportedAccount.Unsupported> = emptyList(),
    val sending: Boolean = false,
    val lastResult: SyncResult? = null,
    val showDeleteImageHint: Boolean = false,
    val pinRequired: Boolean = true,
    val lastSync: LastSync? = null,
) {
    fun accountsToSend(limit: Int?): List<OtpAccount> = if (limit == null) accounts else accounts.take(limit)
}

@OptIn(ExperimentalTime::class)
class SyncViewModel(
    private val watchBridge: WatchBridge,
    private val settings: PhoneSettings = InMemoryPhoneSettings(),
    private val nextSyncId: () -> Int = { Random.nextInt(1, Int.MAX_VALUE) },
    private val now: () -> Long = { Clock.System.now().epochSeconds },
) : ViewModel() {
    private val collector = MigrationCollector()
    private val _state = MutableStateFlow(SyncUiState(pinRequired = settings.pinRequired, lastSync = settings.lastSync))

    val state: StateFlow<SyncUiState> = _state.asStateFlow()
    val watchState: StateFlow<WatchState> = watchBridge.state
    val canChooseDevice: Boolean = watchBridge.canChooseDevice

    fun onScanned(text: String) {
        val content = text.trim()
        val batch = MigrationUri.parse(content)
        if (batch != null) {
            when (val progress = collector.add(batch)) {
                is MigrationCollector.Progress.Waiting -> _state.update {
                    it.copy(scanStatus = ScanStatus.Collecting(progress.scanned, progress.total))
                }
                is MigrationCollector.Progress.Complete -> {
                    collector.reset()
                    merge(progress.accounts)
                }
                MigrationCollector.Progress.DifferentExport -> _state.update { it.copy(scanStatus = ScanStatus.DifferentExport) }
            }
            return
        }
        val account = OtpAuthUri.parse(content)
        if (account != null) {
            merge(listOf(account))
            return
        }
        _state.update { it.copy(scanStatus = ScanStatus.Unrecognised) }
    }

    fun onImageScanned(values: List<String>) {
        if (values.isEmpty()) {
            _state.update { it.copy(scanStatus = ScanStatus.NoQrInImage) }
            return
        }
        values.forEach(::onScanned)
        _state.update { it.copy(showDeleteImageHint = true) }
    }

    fun dismissDeleteImageHint() {
        _state.update { it.copy(showDeleteImageHint = false) }
    }

    fun chooseDevice() {
        watchBridge.chooseDevice()
    }

    fun startScanning() {
        _state.update { it.copy(scanning = true, scanStatus = it.scanStatus as? ScanStatus.Collecting ?: ScanStatus.Idle) }
    }

    fun stopScanning() {
        collector.reset()
        _state.update { it.copy(scanning = false, scanStatus = ScanStatus.Idle) }
    }

    fun startOver() {
        collector.reset()
        _state.update { it.copy(scanStatus = ScanStatus.Idle) }
    }

    fun clearAccounts() {
        collector.reset()
        _state.update { SyncUiState(pinRequired = it.pinRequired, lastSync = it.lastSync) }
    }

    fun setPinRequired(required: Boolean) {
        settings.pinRequired = required
        _state.update { it.copy(pinRequired = required, lastResult = null) }
    }

    fun send() {
        val current = _state.value
        if (current.sending || current.accounts.isEmpty()) return
        val syncId = nextSyncId()
        val ready = watchBridge.state.value as? WatchState.Ready
        val toSend = current.accountsToSend(ready?.edition?.accountLimit)
        val messages = WatchProtocol.messages(toSend, syncId, pinRequired = current.pinRequired)
        _state.update { it.copy(sending = true, lastResult = null) }
        viewModelScope.launch {
            val result = watchBridge.send(syncId, messages)
            if (result != SyncResult.Confirmed) {
                _state.update { it.copy(sending = false, lastResult = result) }
                return@launch
            }
            val lastSync = LastSync(now(), toSend.size, ready?.deviceName.orEmpty())
            settings.lastSync = lastSync
            _state.update { it.copy(sending = false, lastResult = result, lastSync = lastSync) }
        }
    }

    private fun merge(imported: List<ImportedAccount>) {
        _state.update { state ->
            val added = imported.filterIsInstance<ImportedAccount.Supported>().map { it.account }.filter { it !in state.accounts }.distinct()
            state.copy(
                scanning = false,
                scanStatus = ScanStatus.Idle,
                accounts = state.accounts + added,
                unsupported = (state.unsupported + imported.filterIsInstance<ImportedAccount.Unsupported>()).distinct(),
                lastResult = null,
            )
        }
    }
}
