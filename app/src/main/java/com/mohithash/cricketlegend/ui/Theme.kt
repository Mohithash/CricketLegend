package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PitchGreen = Color(0xFF1E7A4C)
val DeepNavy = Color(0xFF0B1220)
val CardNavy = Color(0xFF16233A)
val GoldAccent = Color(0xFFE8B54D)
val BallRed = Color(0xFFC0392B)
val TextPrimary = Color(0xFFEDF2F7)
val TextDim = Color(0xFF93A3B8)
val WinGreen = Color(0xFF3DDC84)
val LossRed = Color(0xFFFF6B6B)

// gradient accents for the enhanced UI
val HeroTop = Color(0xFF1B3A5C)
val HeroBottom = Color(0xFF0E1B2E)
val GoldDeep = Color(0xFFB8862E)
val TileBg = Color(0xFF1A2A44)
val Violet = Color(0xFF7C5CFF)

private val Scheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF1A1205),
    secondary = PitchGreen,
    onSecondary = TextPrimary,
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = CardNavy,
    onSurface = TextPrimary,
    surfaceVariant = Color(0xFF1D2C47),
    onSurfaceVariant = TextDim,
    error = LossRed
)

@Composable
fun CricketLegendTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
