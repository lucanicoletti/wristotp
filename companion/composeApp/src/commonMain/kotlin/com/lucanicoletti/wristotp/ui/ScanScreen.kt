package com.lucanicoletti.wristotp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import com.lucanicoletti.wristotp.scan.QrScanner
import com.lucanicoletti.wristotp.sync.ScanStatus
import com.lucanicoletti.wristotp.sync.SyncUiState
import com.lucanicoletti.wristotp.sync.SyncViewModel

@Composable
fun ScanScreen(viewModel: SyncViewModel, state: SyncUiState, onImportImage: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        QrScanner(onScanned = viewModel::onScanned, modifier = Modifier.fillMaxSize())
        Reticle(Modifier.align(Alignment.Center).fillMaxWidth(0.7f).aspectRatio(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.statusBarsPadding().padding(8.dp),
        ) {
            IconButton(onClick = viewModel::stopScanning) {
                Icon(Icons.Outlined.Close, contentDescription = "Close", tint = WristOtpColors.TextPrimary)
            }
            Text("Scan the export QR code", style = MaterialTheme.typography.titleMedium)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(WristOtpColors.Background.copy(alpha = 0.9f))
                .navigationBarsPadding()
                .padding(16.dp),
        ) {
            val (color, text) = when (val status = state.scanStatus) {
                is ScanStatus.Collecting -> WristOtpColors.Amber to "Scanned ${status.scanned} of ${status.total}. Show the next QR code in Google Authenticator."
                ScanStatus.DifferentExport -> WristOtpColors.Red to "That QR code is from a different export."
                ScanStatus.Unrecognised -> WristOtpColors.Red to "That isn't an authenticator QR code."
                ScanStatus.NoQrInImage -> WristOtpColors.Red to "No QR code found in that image."
                ScanStatus.Idle -> WristOtpColors.TextMuted to "In Google Authenticator, open Transfer accounts → Export accounts. Everything is decoded on this phone."
            }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
            if (state.scanStatus == ScanStatus.DifferentExport) {
                SecondaryButton("Start over", onClick = viewModel::startOver, modifier = Modifier.fillMaxWidth())
            }
            SecondaryButton("Import image instead", onClick = onImportImage, icon = Icons.Outlined.Image, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Reticle(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = 3.dp.toPx()
        val arm = size.minDimension * 0.18f
        val corners = listOf(
            Offset(0f, 0f) to Offset(1f, 1f),
            Offset(size.width, 0f) to Offset(-1f, 1f),
            Offset(0f, size.height) to Offset(1f, -1f),
            Offset(size.width, size.height) to Offset(-1f, -1f),
        )
        corners.forEach { (corner, direction) ->
            drawLine(WristOtpColors.Cyan, corner, corner + Offset(arm * direction.x, 0f), stroke, StrokeCap.Round)
            drawLine(WristOtpColors.Cyan, corner, corner + Offset(0f, arm * direction.y), stroke, StrokeCap.Round)
        }
    }
}
