package io.github.vlukyanets.lemonflow.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Design-canvas tokens that have no Material 3 slot. Values come from the "LemonFlow" canvas. */
@Immutable
data class LemonFlowColors(
    val surface2: Color,
    val line: Color,
    val muted: Color,
    val blueSoft: Color,
    val ring: Color,
    val runBg: Color,
    val warn: Color,
    val warnSoft: Color,
    val danger: Color,
    val dangerSoft: Color,
    val dangerLine: Color,
    val inverse: Color,
    val onInverse: Color,
    val inverseMuted: Color,
    val inverseTrack: Color,
    val trackOff: Color,
    val scrim: Color
)

internal val LightTokens = LemonFlowColors(
    surface2 = Color(color = 0xFFECECE7),
    line = Color(color = 0xFFE2E2DC),
    muted = Color(color = 0xFF5C606B),
    blueSoft = Color(color = 0xFFE5EAFE),
    ring = Color(color = 0xFFC9CAC3),
    runBg = Color(color = 0xFFF3F5FF),
    warn = Color(color = 0xFFA8470A),
    warnSoft = Color(color = 0xFFFCEBDD),
    danger = Color(color = 0xFFB3261E),
    dangerSoft = Color(color = 0xFFFCEEEC),
    dangerLine = Color(color = 0xFFF1C9C4),
    inverse = Color(color = 0xFF15161A),
    onInverse = Color(color = 0xFFFFFFFF),
    inverseMuted = Color(color = 0xFFC9CBD2),
    inverseTrack = Color(color = 0xFF3A3C44),
    trackOff = Color(color = 0xFFD3D3CC),
    scrim = Color(color = 0x7A15161A)
)

internal val DarkTokens = LemonFlowColors(
    surface2 = Color(color = 0xFF222429),
    line = Color(color = 0xFF2A2C33),
    muted = Color(color = 0xFF9A9EAA),
    blueSoft = Color(color = 0xFF1D2550),
    ring = Color(color = 0xFF4A4D57),
    runBg = Color(color = 0xFF151A33),
    warn = Color(color = 0xFFFF9F5A),
    warnSoft = Color(color = 0xFF3B2516),
    danger = Color(color = 0xFFFF8A80),
    dangerSoft = Color(color = 0xFF2E1715),
    dangerLine = Color(color = 0xFF5A2622),
    inverse = Color(color = 0xFFEDEEF1),
    onInverse = Color(color = 0xFF0D0E11),
    inverseMuted = Color(color = 0xFF45484F),
    inverseTrack = Color(color = 0xFFC2C4CB),
    trackOff = Color(color = 0xFF3A3C44),
    scrim = Color(color = 0x9E000000)
)

private val LightBlue = Color(color = 0xFF2448E8)
private val DarkBlue = Color(color = 0xFF7D93FF)
private val LightInk = Color(color = 0xFF15161A)
private val DarkInk = Color(color = 0xFFEDEEF1)
private val LightSurface = Color(color = 0xFFFFFFFF)
private val DarkSurface = Color(color = 0xFF17181C)

internal val LightScheme: ColorScheme = lightColorScheme(
    primary = LightBlue,
    onPrimary = LightSurface,
    primaryContainer = LightTokens.blueSoft,
    onPrimaryContainer = LightBlue,
    background = Color(color = 0xFFF4F4F1),
    onBackground = LightInk,
    surface = LightSurface,
    onSurface = LightInk,
    surfaceVariant = LightTokens.surface2,
    onSurfaceVariant = LightTokens.muted,
    surfaceContainer = LightSurface,
    surfaceContainerHigh = LightSurface,
    outline = LightTokens.line,
    outlineVariant = LightTokens.line,
    error = LightTokens.danger,
    onError = LightSurface
)

internal val DarkScheme: ColorScheme = darkColorScheme(
    primary = DarkBlue,
    onPrimary = DarkTokens.onInverse,
    primaryContainer = DarkTokens.blueSoft,
    onPrimaryContainer = DarkBlue,
    background = Color(color = 0xFF0D0E11),
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkTokens.surface2,
    onSurfaceVariant = DarkTokens.muted,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    outline = DarkTokens.line,
    outlineVariant = DarkTokens.line,
    error = DarkTokens.danger,
    onError = DarkTokens.onInverse
)

val LocalLemonFlowColors = staticCompositionLocalOf { LightTokens }
