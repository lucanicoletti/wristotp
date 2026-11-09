package com.lucanicoletti.wristotp.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lucanicoletti.wristotp.sync.WatchEdition
import com.lucanicoletti.wristotp.sync.WatchState

val type: WristOtpType
    @Composable get() = LocalWristOtpType.current

@Composable
fun Panel(
    modifier: Modifier = Modifier,
    raised: Boolean = false,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = if (raised) WristOtpColors.Raised else WristOtpColors.Card,
        contentColor = WristOtpColors.TextPrimary,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (highlighted) WristOtpColors.Cyan.copy(alpha = 0.2f) else WristOtpColors.Hairline),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = WristOtpColors.TextMuted) {
    Text(text.uppercase(), style = type.labelCaps, color = color, modifier = modifier, maxLines = 1, overflow = TextOverflow.Ellipsis)
}

@Composable
fun StatusDot(color: Color, pulsing: Boolean = false) {
    val alpha = if (pulsing) {
        val transition = rememberInfiniteTransition()
        transition.animateFloat(1f, 0.35f, infiniteRepeatable(tween(900), RepeatMode.Reverse)).value
    } else {
        1f
    }
    Box(Modifier.size(8.dp).alpha(alpha).clip(CircleShape).background(color))
}

data class WatchStatus(val color: Color, val label: String, val detail: String, val ready: Boolean)

fun WatchState.deviceName(): String? = when (this) {
    is WatchState.Disconnected -> deviceName
    is WatchState.Checking -> deviceName
    is WatchState.AppNotInstalled -> deviceName
    is WatchState.Ready -> deviceName
    else -> null
}

fun WatchState.status(): WatchStatus = when (this) {
    WatchState.Initializing -> WatchStatus(WristOtpColors.Amber, "Connecting", "Starting Connect IQ…", false)
    is WatchState.Unavailable -> WatchStatus(WristOtpColors.Red, "Unavailable", reason, false)
    WatchState.NoDevice -> WatchStatus(WristOtpColors.TextMuted, "No watch", "Choose your watch to get started", false)
    is WatchState.Disconnected -> WatchStatus(WristOtpColors.TextMuted, "Offline", "Not connected. Open Garmin Connect", false)
    is WatchState.Checking -> WatchStatus(WristOtpColors.Amber, "Searching", "Looking for WristOTP on the watch…", false)
    is WatchState.AppNotInstalled -> WatchStatus(WristOtpColors.Red, "App missing", "Install WristOTP from the Connect IQ Store", false)
    is WatchState.Ready -> WatchStatus(
        WristOtpColors.Emerald,
        "Ready",
        if (edition == WatchEdition.Free) "Connected · WristOTP Free" else "Connected · WristOTP",
        true,
    )
}

@Composable
fun AppMark(size: Dp) {
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.11f
        val radius = this.size.minDimension / 2 - stroke
        val center = Offset(this.size.width / 2, this.size.height / 2)
        val topLeft = Offset(center.x - radius, center.y - radius)
        val arc = Size(radius * 2, radius * 2)
        drawArc(Color(0xFF3A3F47), 0f, 360f, false, topLeft, arc, style = Stroke(stroke))
        drawArc(WristOtpColors.Cyan, -90f, 270f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
        val dot = radius * 0.11f
        listOf(-0.52f, -0.34f, -0.16f, 0.16f, 0.34f, 0.52f).forEach { x ->
            drawCircle(Color.White, dot, Offset(center.x + x * radius, center.y))
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WristOtpColors.Cyan,
            contentColor = WristOtpColors.OnCyan,
            disabledContainerColor = WristOtpColors.Raised,
            disabledContentColor = WristOtpColors.TextMuted,
        ),
        modifier = modifier.height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = WristOtpColors.TextMuted)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    contentColor: Color = WristOtpColors.TextPrimary,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (contentColor == WristOtpColors.TextPrimary) WristOtpColors.Outline else contentColor.copy(alpha = 0.4f)),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = WristOtpColors.Raised, contentColor = contentColor),
        modifier = modifier.height(52.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = contentColor)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

private val monogramColors = listOf(WristOtpColors.Cyan, WristOtpColors.Emerald, WristOtpColors.Amber, Color(0xFFA78BFA), Color(0xFFF472B6))

@Composable
fun Monogram(text: String, size: Dp = 44.dp) {
    val initial = text.trim().firstOrNull()?.uppercaseChar() ?: '?'
    val color = monogramColors[(text.hashCode() and Int.MAX_VALUE) % monogramColors.size]
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(WristOtpColors.Raised),
    ) {
        Text(initial.toString(), style = MaterialTheme.typography.titleLarge, color = color)
    }
}

@Composable
fun InfoBanner(
    text: String,
    action: String? = null,
    onAction: () -> Unit = {},
    accent: Color = WristOtpColors.Cyan,
) {
    Surface(
        color = WristOtpColors.Card,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)) {
            Box(Modifier.size(width = 3.dp, height = 32.dp).clip(RoundedCornerShape(2.dp)).background(accent))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = WristOtpColors.TextPrimary, modifier = Modifier.weight(1f))
            if (action != null) {
                TextButton(onClick = onAction) { Text(action, color = accent) }
            }
        }
    }
}

@Composable
fun FooterNote(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        style = type.labelSmall,
        color = WristOtpColors.TextSubdued,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}
