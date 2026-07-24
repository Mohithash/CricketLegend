package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Broadcast Night palette ──────────────────────────────────────────────
val PitchGreen = Color(0xFF16915A)
val DeepNavy = Color(0xFF070B14)      // app background base (near-black blue)
val CardNavy = Color(0xFF121A2B)      // card surface
val GoldAccent = Color(0xFFF2C24B)    // primary accent (trophy gold)
val BallRed = Color(0xFFD1493E)
val TextPrimary = Color(0xFFF3F6FB)
val TextDim = Color(0xFF8A99B0)
val WinGreen = Color(0xFF35E08A)
val LossRed = Color(0xFFFF6B6B)

// gradient + structural tokens
val HeroTop = Color(0xFF14324F)
val HeroBottom = Color(0xFF0A1424)
val GoldDeep = Color(0xFFB8862E)
val TileBg = Color(0xFF16223A)
val Violet = Color(0xFF8B7BFF)
val Teal = Color(0xFF25D6C4)          // electric secondary accent (broadcast)
val CardBorder = Color(0xFF223047)    // subtle luminous card edge
val BgTop = Color(0xFF0C1626)         // background gradient top
val BgBottom = Color(0xFF05080F)      // background gradient bottom

private val Scheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = Color(0xFF17130A),
    secondary = Teal,
    onSecondary = Color(0xFF04140F),
    background = DeepNavy,
    onBackground = TextPrimary,
    surface = CardNavy,
    onSurface = TextPrimary,
    surfaceVariant = TileBg,
    onSurfaceVariant = TextDim,
    error = LossRed
)

/** A full-screen stadium-night gradient background. */
@Composable
fun ScreenBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(BgTop, BgBottom)))) {
        content()
    }
}

@Composable
fun CricketLegendTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
