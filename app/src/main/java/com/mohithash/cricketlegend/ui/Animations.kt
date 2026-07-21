package com.mohithash.cricketlegend.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.sin
import kotlin.random.Random

/** A number that smoothly counts up/down to its target when it changes. */
@Composable
fun rememberAnimatedInt(target: Int): Int {
    val v by animateFloatAsState(
        targetValue = target.toFloat(),
        animationSpec = tween(700, easing = LinearEasing),
        label = "count"
    )
    return v.toInt()
}

/** An infinitely shifting gradient brush for hero bands and premium surfaces. */
@Composable
fun shimmerBrush(colors: List<Color>): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Reverse),
        label = "shimmerX"
    )
    return Brush.linearGradient(colors, start = Offset(x - 400f, 0f), end = Offset(x, 400f))
}

/** A soft pulsing alpha for glowing call-to-action elements. */
@Composable
fun pulseAlpha(min: Float = 0.55f, max: Float = 1f): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val a by transition.animateFloat(
        initialValue = min, targetValue = max,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "pulseA"
    )
    return a
}

private class Particle(
    var x: Float, var y: Float, var vx: Float, var vy: Float,
    val color: Color, val size: Float, var rot: Float, val vr: Float
)

/**
 * A one-shot confetti burst overlay. Place at the top of a Box that fills the
 * screen; [trigger] any changing key to re-fire the celebration.
 */
@Composable
fun ConfettiOverlay(trigger: Any?, modifier: Modifier = Modifier, intensity: Int = 90) {
    if (trigger == null) return
    val palette = listOf(GoldAccent, WinGreen, BallRed, Violet, Color(0xFF4FC3F7), Color(0xFFF06292))
    val particles = remember(trigger) {
        val rng = Random(trigger.hashCode())
        List(intensity) {
            Particle(
                x = 0.5f + (rng.nextFloat() - 0.5f) * 0.2f,
                y = 0.35f + (rng.nextFloat() - 0.5f) * 0.1f,
                vx = (rng.nextFloat() - 0.5f) * 0.9f,
                vy = -0.6f - rng.nextFloat() * 0.7f,
                color = palette[rng.nextInt(palette.size)],
                size = 5f + rng.nextFloat() * 9f,
                rot = rng.nextFloat() * 6.28f,
                vr = (rng.nextFloat() - 0.5f) * 0.6f
            )
        }
    }
    val transition = rememberInfiniteTransition(label = "confetti")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "confettiT"
    )

    Canvas(modifier.fillMaxSize()) {
        val g = 1.4f
        particles.forEach { p ->
            // simple projectile with gravity over the loop's normalized time
            val px = (p.x + p.vx * t) * size.width
            val py = (p.y + p.vy * t + 0.5f * g * t * t) * size.height
            if (py < size.height + 20) {
                val wobble = sin((t * 12 + p.rot).toDouble()).toFloat() * p.size * 0.4f
                rotate(degrees = (p.rot + p.vr * t * 360), pivot = Offset(px + wobble, py)) {
                    drawRect(
                        color = p.color.copy(alpha = (1f - t).coerceIn(0f, 1f)),
                        topLeft = Offset(px + wobble - p.size / 2, py - p.size / 2),
                        size = androidx.compose.ui.geometry.Size(p.size, p.size * 1.6f)
                    )
                }
            }
        }
    }
}

/** A spinning cricket-ball loading indicator. */
@Composable
fun CricketBallSpinner(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "ball")
    val rot by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "ballRot"
    )
    Canvas(modifier) {
        val r = size.minDimension / 2f * 0.8f
        val c = Offset(size.width / 2, size.height / 2)
        drawCircle(BallRed, r, c)
        rotate(rot, c) {
            // seam
            drawLine(Color(0xFFF5E6C8), Offset(c.x - r * 0.6f, c.y), Offset(c.x + r * 0.6f, c.y), 3f)
            for (i in -2..2) {
                val x = c.x + i * r * 0.25f
                drawLine(Color(0xFFF5E6C8), Offset(x, c.y - 4), Offset(x, c.y + 4), 2f)
            }
        }
    }
}
