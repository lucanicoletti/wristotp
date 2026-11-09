package com.lucanicoletti.wristotp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lucanicoletti.wristotp.otp.ImportedAccount
import com.lucanicoletti.wristotp.otp.OtpAccount
import com.lucanicoletti.wristotp.otp.UnsupportedReason
import com.lucanicoletti.wristotp.scan.rememberImageQrImporter
import com.lucanicoletti.wristotp.sync.SyncResult
import com.lucanicoletti.wristotp.sync.SyncUiState
import com.lucanicoletti.wristotp.sync.SyncViewModel
import com.lucanicoletti.wristotp.sync.WatchEdition
import com.lucanicoletti.wristotp.sync.WatchState

private const val PRIVACY_URL = "https://lucanicoletti.github.io/wristotp-site/privacy"
private const val SUPPORT_URL = "https://lucanicoletti.github.io/wristotp-site/support"

@Composable
fun MainScreen(viewModel: SyncViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val watchState by viewModel.watchState.collectAsStateWithLifecycle()
    val importImage = rememberImageQrImporter(viewModel::onImageScanned)
    PlatformBackHandler(enabled = state.scanning, onBack = viewModel::stopScanning)

    Surface(color = WristOtpColors.Background, contentColor = WristOtpColors.TextPrimary, modifier = Modifier.fillMaxSize()) {
        if (state.scanning) {
            ScanScreen(viewModel = viewModel, state = state, onImportImage = importImage)
            return@Surface
        }
        val edition = (watchState as? WatchState.Ready)?.edition
        val toSend = state.accountsToSend(edition?.accountLimit)
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f).statusBarsPadding().padding(horizontal = 16.dp),
            ) {
                item { Header() }
                item { WatchCard(viewModel = viewModel, state = state, watchState = watchState) }
                if (state.accounts.isEmpty() && state.unsupported.isEmpty()) {
                    item { EmptyAccounts(onScan = viewModel::startScanning, onImportImage = importImage) }
                } else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CapsLabel("Accounts (${state.accounts.size})", modifier = Modifier.weight(1f))
                            TextButton(onClick = viewModel::clearAccounts) {
                                Text("CLEAR", style = type.labelCaps, color = WristOtpColors.TextMuted)
                            }
                        }
                    }
                    if (edition != null && state.accounts.size > toSend.size) {
                        item {
                            InfoBanner(
                                text = if (edition == WatchEdition.Free) {
                                    "WristOTP Free holds one account, so only the first is sent. WristOTP holds up to ${WatchEdition.Full.accountLimit}."
                                } else {
                                    "The watch holds up to ${edition.accountLimit} accounts, so only the first ${edition.accountLimit} are sent."
                                },
                                accent = WristOtpColors.Amber,
                            )
                        }
                    }
                    items(state.accounts) { account -> AccountRow(account, sent = edition == null || account in toSend) }
                    items(state.unsupported) { account -> UnsupportedRow(account) }
                }
                if (state.showDeleteImageHint) {
                    item {
                        InfoBanner(
                            text = "That image contains your 2FA secrets. Delete it from Photos once you've synced.",
                            action = "Got it",
                            onAction = viewModel::dismissDeleteImageHint,
                            accent = WristOtpColors.Amber,
                        )
                    }
                }
                item { Footer() }
            }
            if (state.accounts.isNotEmpty()) {
                SendBar(viewModel = viewModel, state = state, watchState = watchState, count = toSend.size)
            }
        }
    }
}

@Composable
private fun Header() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(Color.Black),
        ) {
            AppMark(28.dp)
        }
        Spacer(Modifier.width(12.dp))
        Text("WristOTP", style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun WatchCard(viewModel: SyncViewModel, state: SyncUiState, watchState: WatchState) {
    val status = watchState.status()
    Panel(highlighted = status.ready) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(status.color, pulsing = status.ready)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(watchState.deviceName() ?: "No watch", style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(status.detail, style = MaterialTheme.typography.bodySmall, color = WristOtpColors.TextMuted)
            }
            if (viewModel.canChooseDevice) {
                TextButton(onClick = viewModel::chooseDevice) {
                    Text(if (watchState == WatchState.NoDevice) "CHOOSE" else "CHANGE", style = type.labelCaps, color = WristOtpColors.Cyan)
                }
            }
        }
        HorizontalDivider(color = WristOtpColors.Hairline, modifier = Modifier.padding(vertical = 12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Require PIN on watch", style = MaterialTheme.typography.titleSmall)
                Text("Applies from the next send", style = MaterialTheme.typography.bodySmall, color = WristOtpColors.TextMuted)
            }
            Switch(
                checked = state.pinRequired,
                onCheckedChange = viewModel::setPinRequired,
                colors = SwitchDefaults.colors(checkedTrackColor = WristOtpColors.Emerald, checkedThumbColor = WristOtpColors.TextPrimary),
            )
        }
        val lastSync = state.lastSync
        if (lastSync != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Last sent ${lastSync.accountCount} ${if (lastSync.accountCount == 1) "account" else "accounts"} to ${lastSync.deviceName}",
                style = MaterialTheme.typography.bodySmall,
                color = WristOtpColors.TextMuted,
            )
        }
    }
}

@Composable
private fun AccountRow(account: OtpAccount, sent: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (sent) 1f else 0.45f)) {
        Monogram(account.issuer.ifEmpty { account.name }, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(account.issuer.ifEmpty { account.name }, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (account.issuer.isNotEmpty()) {
                Text(account.name, style = MaterialTheme.typography.bodySmall, color = WristOtpColors.TextMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (!sent) {
            CapsLabel("Not sent")
        }
    }
}

@Composable
private fun UnsupportedRow(account: ImportedAccount.Unsupported) {
    val reason = when (account.reason) {
        UnsupportedReason.HOTP -> "Counter-based (HOTP) codes aren't supported"
        UnsupportedReason.ALGORITHM -> "Uses an algorithm the watch can't compute"
        UnsupportedReason.DIGITS -> "Unsupported code length"
        UnsupportedReason.PERIOD -> "Unsupported time period"
        UnsupportedReason.INVALID_SECRET -> "The secret in this QR code is invalid"
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(0.6f)) {
        Monogram(account.issuer.ifEmpty { account.name }, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(account.issuer.ifEmpty { account.name }, style = MaterialTheme.typography.titleSmall)
            Text(reason, style = MaterialTheme.typography.bodySmall, color = WristOtpColors.Red)
        }
    }
}

@Composable
private fun EmptyAccounts(onScan: () -> Unit, onImportImage: () -> Unit) {
    Panel {
        Text("Add your accounts", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GuideStep(1, "In Google Authenticator, open Transfer accounts → Export accounts.")
            GuideStep(2, "Scan the QR code, or import a screenshot of it.")
            GuideStep(3, "Open WristOTP on your watch and tap Send.")
        }
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Scan QR code", onClick = onScan, icon = Icons.Outlined.QrCodeScanner, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        SecondaryButton("Import image", onClick = onImportImage, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun GuideStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).clip(CircleShape).background(WristOtpColors.Cyan.copy(alpha = 0.15f)),
        ) {
            Text("$number", style = type.labelCaps, color = WristOtpColors.Cyan)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun SendBar(viewModel: SyncViewModel, state: SyncUiState, watchState: WatchState, count: Int) {
    val status = watchState.status()
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(WristOtpColors.Background)
            .navigationBarsPadding()
            .padding(16.dp),
    ) {
        SyncResultText(state.lastResult)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Scan", onClick = viewModel::startScanning, icon = Icons.Outlined.QrCodeScanner)
            PrimaryButton(
                text = if (status.ready) "Send $count to ${watchState.deviceName()}" else status.label,
                onClick = viewModel::send,
                enabled = status.ready,
                loading = state.sending,
                icon = Icons.Outlined.Sync,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SyncResultText(result: SyncResult?) {
    val (color, text) = when (result) {
        null -> return
        SyncResult.Confirmed -> WristOtpColors.Emerald to "Synced. The watch confirmed every account."
        SyncResult.Unconfirmed -> WristOtpColors.Amber to "Sent, but the watch didn't confirm. Open WristOTP on the watch and try again."
        is SyncResult.Rejected -> WristOtpColors.Red to "The watch rejected the sync: ${result.reason}"
        is SyncResult.Failed -> WristOtpColors.Red to "Sending failed: ${result.reason}"
    }
    Text(text, color = color, style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun Footer() {
    val uriHandler = LocalUriHandler.current
    Column(Modifier.padding(top = 8.dp, bottom = 16.dp)) {
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { uriHandler.openUri(PRIVACY_URL) }) {
                Text("PRIVACY POLICY", style = type.labelCaps, color = WristOtpColors.Cyan)
            }
            TextButton(onClick = { uriHandler.openUri(SUPPORT_URL) }) {
                Text("HELP", style = type.labelCaps, color = WristOtpColors.Cyan)
            }
        }
        FooterNote("WristOTP isn't affiliated with Google or Garmin")
    }
}
