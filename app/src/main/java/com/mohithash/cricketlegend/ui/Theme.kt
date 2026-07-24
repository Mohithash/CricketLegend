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

// ── "Matchday" light palette — a clean modern sports-app identity ────────
// (Constant names kept from the old dark theme so every screen restyles at once.)
val PitchGreen = Color(0xFF0E8A5F)    // primary action — fresh turf green
val DeepNavy = Color(0xFF101826)      // ink (dark text on accents; deep surfaces)
val CardNavy = Color(0xFFEDF1F6)      // secondary chip / unselected button (light slate)
val GoldAccent = Color(0xFFDB8A00)    // trophy amber, darkened for light backgrounds
val BallRed = Color(0xFFC4453B)
val TextPrimary = Color(0xFF1A2433)   // ink body text
val TextDim = Color(0xFF64748B)       // slate secondary text
val WinGreen = Color(0xFF0B9D61)
val LossRed = Color(0xFFD64545)

// gradient + structural tokens
val HeroTop = Color(0xFF0F5E43)       // deep turf hero band
val HeroBottom = Color(0xFF0A3D2E)
val GoldDeep = Color(0xFFB36F00)
val TileBg = Color(0xFFF2F5F9)        // tinted stat tile
val Violet = Color(0xFF6D5AE6)
val Teal = Color(0xFF0E7490)          // deep cyan secondary accent
val CardBorder = Color(0xFFE2E8F0)    // hairline card edge
val CardSurface = Color(0xFFFFFFFF)   // card paper
val BgTop = Color(0xFFF5F7FA)         // background gradient top
val BgBottom = Color(0xFFE8EDF2)      // background gradient bottom

private val Scheme = androidx.compose.material3.lightColorScheme(
    primary = PitchGreen,
    onPrimary = Color.White,
    secondary = Teal,
    onSecondary = Color.White,
    background = BgTop,
    onBackground = TextPrimary,
    surface = CardSurface,
    onSurface = TextPrimary,
    surfaceVariant = TileBg,
    onSurfaceVariant = TextDim,
    outline = CardBorder,
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
