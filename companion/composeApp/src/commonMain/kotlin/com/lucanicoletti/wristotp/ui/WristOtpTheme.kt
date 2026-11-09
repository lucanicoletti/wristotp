package com.lucanicoletti.wristotp.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import wristotp.composeapp.generated.resources.Res
import wristotp.composeapp.generated.resources.inter_400
import wristotp.composeapp.generated.resources.inter_500
import wristotp.composeapp.generated.resources.inter_600
import wristotp.composeapp.generated.resources.jetbrains_mono_500
import wristotp.composeapp.generated.resources.space_grotesk_600
import wristotp.composeapp.generated.resources.space_grotesk_700

object WristOtpColors {
    val Background = Color(0xFF0B0F19)
    val Card = Color(0xFF131B2E)
    val Raised = Color(0xFF1E293B)
    val Well = Color(0xFF0E1422)
    val Hairline = Color(0xFF94A3B8).copy(alpha = 0.08f)
    val Outline = Color(0xFF94A3B8).copy(alpha = 0.2f)
    val Cyan = Color(0xFF00D2FF)
    val OnCyan = Color(0xFF0B0F19)
    val Emerald = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Red = Color(0xFFEF4444)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextMuted = Color(0xFF94A3B8)
    val TextSubdued = Color(0xFF475569)
    val RingTrack = Color(0xFF94A3B8).copy(alpha = 0.15f)
}

@Immutable
data class WristOtpType(
    val labelCaps: TextStyle,
    val labelSmall: TextStyle,
)

val LocalWristOtpType = staticCompositionLocalOf<WristOtpType> { error("WristOtpTheme is missing") }

private val colorScheme = darkColorScheme(
    primary = WristOtpColors.Cyan,
    onPrimary = WristOtpColors.OnCyan,
    primaryContainer = WristOtpColors.Cyan.copy(alpha = 0.12f),
    onPrimaryContainer = WristOtpColors.Cyan,
    secondary = WristOtpColors.Emerald,
    onSecondary = WristOtpColors.OnCyan,
    tertiary = WristOtpColors.Amber,
    background = WristOtpColors.Background,
    onBackground = WristOtpColors.TextPrimary,
    surface = WristOtpColors.Background,
    onSurface = WristOtpColors.TextPrimary,
    surfaceVariant = WristOtpColors.Card,
    onSurfaceVariant = WristOtpColors.TextMuted,
    surfaceContainer = WristOtpColors.Card,
    surfaceContainerHigh = WristOtpColors.Raised,
    surfaceContainerHighest = WristOtpColors.Raised,
    error = WristOtpColors.Red,
    outline = WristOtpColors.Outline,
    outlineVariant = WristOtpColors.Hairline,
)

@Composable
fun WristOtpTheme(content: @Composable () -> Unit) {
    val grotesk = FontFamily(
        Font(Res.font.space_grotesk_600, FontWeight.SemiBold),
        Font(Res.font.space_grotesk_700, FontWeight.Bold),
    )
    val inter = FontFamily(
        Font(Res.font.inter_400, FontWeight.Normal),
        Font(Res.font.inter_500, FontWeight.Medium),
        Font(Res.font.inter_600, FontWeight.SemiBold),
    )
    val mono = FontFamily(
        Font(Res.font.jetbrains_mono_500, FontWeight.Medium),
    )
    val typography = Typography(
        headlineLarge = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineMedium = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
        headlineSmall = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleLarge = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(fontFamily = inter, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
        bodyLarge = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = TextStyle(fontFamily = inter, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = TextStyle(fontFamily = grotesk, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp),
        labelMedium = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp),
        labelSmall = TextStyle(fontFamily = inter, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp),
    )
    val type = WristOtpType(
        labelCaps = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.06.em),
        labelSmall = TextStyle(fontFamily = mono, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 12.sp, letterSpacing = 0.04.em),
    )
    CompositionLocalProvider(LocalWristOtpType provides type) {
        MaterialTheme(colorScheme = colorScheme, typography = typography, content = content)
    }
}
