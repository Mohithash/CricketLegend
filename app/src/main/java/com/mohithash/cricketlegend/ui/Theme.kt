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

// ── "Night Turf" — pure-black AMOLED identity with vivid emerald ─────────
// (Constant names kept so every screen restyles at once.)
val PitchGreen = Color(0xFF16C47F)    // vivid emerald primary
val DeepNavy = Color(0xFF0A0D10)      // ink used on bright accents
val CardNavy = Color(0xFF1D242C)      // secondary chip / unselected button (graphite)
val GoldAccent = Color(0xFFF5C044)    // trophy gold — reserved for glory moments
val BallRed = Color(0xFFE2554A)
val TextPrimary = Color(0xFFEDF2F6)
val TextDim = Color(0xFF8B97A3)
val WinGreen = Color(0xFF3DDC84)
val LossRed = Color(0xFFFF6B6B)

// gradient + structural tokens
val HeroTop = Color(0xFF0F4A34)       // emerald-black hero band
val HeroBottom = Color(0xFF081D14)
val GoldDeep = Color(0xFFB8862E)
val TileBg = Color(0xFF161B21)        // graphite stat tile
val Violet = Color(0xFF9B8CFF)
val Teal = Color(0xFF2DD4BF)          // mint secondary accent
val CardBorder = Color(0xFF262D35)    // hairline card edge
val CardSurface = Color(0xFF12161B)   // graphite card on true black
val BgTop = Color(0xFF0A0C0E)         // near-black gradient top
val BgBottom = Color(0xFF040506)      // AMOLED black bottom

private val Scheme = darkColorScheme(
    primary = PitchGreen,
    onPrimary = DeepNavy,
    secondary = Teal,
    onSecondary = DeepNavy,
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
