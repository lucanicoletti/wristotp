package com.lucanicoletti.wristotp.sync

import com.lucanicoletti.wristotp.otp.ImportedAccount
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.UnsupportedReason
import com.lucanicoletti.wristotp.settings.InMemoryPhoneSettings
import com.lucanicoletti.wristotp.settings.LastSync
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SyncViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val seed = "12345678901234567890".encodeToByteArray()
    private val bridge = FakeWatchBridge()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun singleOtpAuthCodeAddsAccountAndStopsScanning() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")

        val state = viewModel.state.value
        assertEquals(listOf(OtpAccount("GitHub", "demo", seed)), state.accounts)
        assertFalse(state.scanning)
    }

    @Test
    fun splitExportReportsProgressThenMerges() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onScanned(BATCH_0)
        assertEquals(ScanStatus.Collecting(scanned = 1, total = 2), viewModel.state.value.scanStatus)

        viewModel.onScanned(BATCH_1)
        val state = viewModel.state.value
        assertEquals(ScanStatus.Idle, state.scanStatus)
        assertEquals(listOf("Alpha", "Beta"), state.accounts.map { it.issuer })
    }

    @Test
    fun unsupportedAccountsAreListedSeparately() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onScanned("otpauth://hotp/Bank:demo?secret=GEZDGNBVGY3TQOJQ")
        assertEquals(listOf(ImportedAccount.Unsupported("Bank", "demo", UnsupportedReason.HOTP)), viewModel.state.value.unsupported)
        assertEquals(emptyList(), viewModel.state.value.accounts)
    }

    @Test
    fun unknownQrIsReported() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onScanned("https://example.com")
        assertEquals(ScanStatus.Unrecognised, viewModel.state.value.scanStatus)
    }

    @Test
    fun imageWithSeveralCodesImportsAllAndSuggestsDeletingIt() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onImageScanned(listOf(BATCH_0, BATCH_1))

        val state = viewModel.state.value
        assertEquals(listOf("Alpha", "Beta"), state.accounts.map { it.issuer })
        assertEquals(true, state.showDeleteImageHint)

        viewModel.dismissDeleteImageHint()
        assertFalse(viewModel.state.value.showDeleteImageHint)
    }

    @Test
    fun imageWithoutQrIsReported() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onImageScanned(emptyList())
        assertEquals(ScanStatus.NoQrInImage, viewModel.state.value.scanStatus)
        assertFalse(viewModel.state.value.showDeleteImageHint)
    }

    @Test
    fun sendPassesProtocolMessagesAndStoresResult() = runTest(dispatcher) {
        val viewModel = SyncViewModel(bridge, nextSyncId = { 99 })
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(99, bridge.sentSyncId)
        assertEquals(WatchProtocol.messages(listOf(OtpAccount("GitHub", "demo", seed)), 99, pinRequired = true), bridge.sentMessages)
        assertEquals(SyncResult.Confirmed, viewModel.state.value.lastResult)
        assertFalse(viewModel.state.value.sending)
    }

    @Test
    fun sendWithoutAccountsDoesNothing() = runTest(dispatcher) {
        val viewModel = SyncViewModel(bridge)
        viewModel.send()
        advanceUntilIdle()
        assertEquals(null, bridge.sentSyncId)
    }

    @Test
    fun pinIsRequiredByDefault() {
        assertEquals(true, SyncViewModel(bridge).state.value.pinRequired)
    }

    @Test
    fun pinSettingIsRememberedAndSent() = runTest(dispatcher) {
        val settings = InMemoryPhoneSettings()
        val viewModel = SyncViewModel(bridge, settings = settings, nextSyncId = { 7 })
        viewModel.setPinRequired(false)
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.send()
        advanceUntilIdle()

        assertEquals(false, settings.pinRequired)
        assertEquals(WatchMessage.Begin(syncId = 7, total = 1, pinRequired = false), bridge.sentMessages.first())
        assertEquals(false, SyncViewModel(bridge, settings = settings).state.value.pinRequired)
    }

    @Test
    fun freeWatchGetsOnlyTheFirstAccount() = runTest(dispatcher) {
        val freeBridge = FakeWatchBridge(WatchEdition.Free)
        val viewModel = SyncViewModel(freeBridge)
        viewModel.onImageScanned(listOf(BATCH_0, BATCH_1))
        viewModel.send()
        advanceUntilIdle()
        assertEquals(WatchMessage.Begin(syncId = freeBridge.sentSyncId!!, total = 1, pinRequired = true), freeBridge.sentMessages.first())
        assertEquals(listOf("Alpha"), (freeBridge.sentMessages[1] as WatchMessage.Chunk).accounts.map { it["i"] })
    }

    @Test
    fun fullWatchGetsAtMostFiftyAccounts() = runTest(dispatcher) {
        val viewModel = SyncViewModel(bridge)
        viewModel.onImageScanned((1..51).map { "otpauth://totp/Issuer$it:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ" })
        assertEquals(51, viewModel.state.value.accounts.size)

        viewModel.send()
        advanceUntilIdle()
        val sent = bridge.sentMessages.filterIsInstance<WatchMessage.Chunk>().flatMap { chunk -> chunk.accounts.map { it["i"] } }
        assertEquals((1..50).map { "Issuer$it" }, sent)
    }

    @Test
    fun confirmedSyncRecordsLastSync() = runTest(dispatcher) {
        val settings = InMemoryPhoneSettings()
        val viewModel = SyncViewModel(bridge, settings = settings, now = { 1_000L })
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.send()
        advanceUntilIdle()
        assertEquals(LastSync(1_000L, 1, "fēnix 8"), viewModel.state.value.lastSync)
        assertEquals(LastSync(1_000L, 1, "fēnix 8"), settings.lastSync)
    }

    @Test
    fun clearingAccountsKeepsSettings() {
        val viewModel = SyncViewModel(bridge, settings = InMemoryPhoneSettings(pinRequired = false))
        viewModel.onScanned("otpauth://totp/GitHub:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.clearAccounts()
        assertEquals(emptyList(), viewModel.state.value.accounts)
        assertEquals(false, viewModel.state.value.pinRequired)
    }

    @Test
    fun movedAccountIsSentFirst() = runTest(dispatcher) {
        val freeBridge = FakeWatchBridge(WatchEdition.Free)
        val viewModel = SyncViewModel(freeBridge)
        viewModel.onImageScanned(listOf(BATCH_0, BATCH_1))
        viewModel.moveAccount(1, 0)
        assertEquals(listOf("Beta", "Alpha"), viewModel.state.value.accounts.map { it.issuer })

        viewModel.send()
        advanceUntilIdle()
        assertEquals(listOf("Beta"), (freeBridge.sentMessages[1] as WatchMessage.Chunk).accounts.map { it["i"] })
    }

    @Test
    fun moveOutsideTheListIsIgnored() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onImageScanned(listOf(BATCH_0, BATCH_1))
        viewModel.moveAccount(0, 5)
        assertEquals(listOf("Alpha", "Beta"), viewModel.state.value.accounts.map { it.issuer })
    }

    @Test
    fun sortOrdersByIssuerThenNameIgnoringCase() {
        val viewModel = SyncViewModel(bridge)
        viewModel.onScanned("otpauth://totp/zeta:demo?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.onScanned("otpauth://totp/Acme:work?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.onScanned("otpauth://totp/acme:home?secret=GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ")
        viewModel.sortAccounts()
        assertEquals(listOf("home", "work", "demo"), viewModel.state.value.accounts.map { it.name })
    }

    private class FakeWatchBridge(edition: WatchEdition = WatchEdition.Full) : WatchBridge {
        override val state: StateFlow<WatchState> = MutableStateFlow(WatchState.Ready("fēnix 8", edition))
        var sentSyncId: Int? = null
        var sentMessages: List<WatchMessage> = emptyList()

        override suspend fun send(syncId: Int, messages: List<WatchMessage>): SyncResult {
            sentSyncId = syncId
            sentMessages = messages
            return SyncResult.Confirmed
        }
    }

    private companion object {
        const val BATCH_0 = "otpauth-migration://offline?data=CioKFDEyMzQ1Njc4OTAxMjM0NTY3ODkwEgVmaXJzdBoFQWxwaGEgASgBMAIQARgCIAAoiQY%3D"
        const val BATCH_1 = "otpauth-migration://offline?data=CjYKIDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDEyEgZzZWNvbmQaBEJldGEgAigBMAIQARgCIAEoiQY%3D"
    }
}
