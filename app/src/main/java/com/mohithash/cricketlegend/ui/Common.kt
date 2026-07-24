package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionHeader(text: String) {
    Row(
        Modifier.padding(top = 18.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // broadcast-style accent bar
        Box(
            Modifier
                .size(width = 4.dp, height = 15.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(listOf(GoldAccent, Teal)),
                    RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text.uppercase(),
            color = TextPrimary,
            fontWeight = FontWeight.Black,
            fontSize = 13.sp,
            letterSpacing = 1.2.sp
        )
    }
}

@Composable
fun InfoCard(content: @Composable () -> Unit) {
    androidx.compose.material3.Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        color = CardSurface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 2.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
    ) {
        Column(Modifier.padding(15.dp)) { content() }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, valueColor: Color = TextPrimary) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextDim, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SkillBar(label: String, value: Double, max: Double = 100.0, color: Color = PitchGreen) {
    // bars ease into place so every change is visible
    val animated by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (value / max).toFloat().coerceIn(0f, 1f),
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 600),
        label = "skillbar"
    )
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = TextDim, fontSize = 12.sp)
            Text("%.0f".format(value), color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        LinearProgressIndicator(
            progress = { animated },
            modifier = Modifier
                .fillMaxWidth()
                .height(7.dp)
                .background(TileBg, RoundedCornerShape(4.dp)),
            color = color,
            trackColor = TileBg
        )
    }
}

@Composable
fun RadarChart(labels: List<String>, values: List<Double>, modifier: Modifier = Modifier, color: Color = GoldAccent) {
    androidx.compose.foundation.Canvas(modifier) {
        val n = labels.size
        if (n < 3) return@Canvas
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) * 0.78f
        fun point(i: Int, frac: Float): androidx.compose.ui.geometry.Offset {
            val angle = Math.PI * 2 * i / n - Math.PI / 2
            return androidx.compose.ui.geometry.Offset(
                cx + (radius * frac * Math.cos(angle)).toFloat(),
                cy + (radius * frac * Math.sin(angle)).toFloat()
            )
        }
        // grid rings
        for (ring in listOf(0.33f, 0.66f, 1f)) {
            val path = androidx.compose.ui.graphics.Path()
            for (i in 0 until n) {
                val p = point(i, ring)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            path.close()
            drawPath(path, TextDim.copy(alpha = 0.25f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        }
        // spokes
        for (i in 0 until n) {
            drawLine(TextDim.copy(alpha = 0.2f), androidx.compose.ui.geometry.Offset(cx, cy), point(i, 1f), 1f)
        }
        // value polygon
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until n) {
            val frac = (values[i] / 100.0).coerceIn(0.05, 1.0).toFloat()
            val p = point(i, frac)
            if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
        }
        path.close()
        drawPath(path, color.copy(alpha = 0.30f))
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
    }
}

@Composable
fun Pill(text: String, color: Color) {
    Text(
        text,
        color = Color(0xFF10131A),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    )
}

/** Gradient hero band with an initials avatar, name, status line and chips. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HeroHeader(
    name: String,
    subtitle: String,
    status: String,
    chips: List<Pair<String, Color>>,
    rightValue: String
) {
    androidx.compose.foundation.layout.Box(
        Modifier
            .fillMaxWidth()
            .background(
                shimmerBrush(listOf(HeroTop, HeroBottom, Color(0xFF11724F), HeroBottom)),
                RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            )
            .padding(horizontal = 16.dp, vertical = 18.dp)
    ) {
        // hero band is deep turf green — text on it is always light
        val heroText = Color.White
        val heroDim = Color(0xCCE9F2ED)
        val heroGold = Color(0xFFFFCF6A)
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.foundation.layout.Box(
                    Modifier
                        .size(52.dp)
                        .background(
                            androidx.compose.ui.graphics.Brush.linearGradient(listOf(heroGold, GoldDeep)),
                            androidx.compose.foundation.shape.CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(initials(name), color = DeepNavy, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
                androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name, color = heroText, fontSize = 20.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text(subtitle, color = heroDim, fontSize = 12.sp)
                    Text(status, color = heroGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Text(rightValue, color = heroGold, fontSize = 15.sp, fontWeight = FontWeight.Black)
            }
            if (chips.isNotEmpty()) {
                androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
                androidx.compose.foundation.layout.FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chips.forEach { (t, c) -> Pill(t, c) }
                }
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotEmpty() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
    }
}

/** Compact metric tile for dashboard grids. Pure-integer values count up smoothly. */
@Composable
fun StatTile(label: String, value: String, accent: Color = GoldAccent, modifier: Modifier = Modifier) {
    val asInt = value.toIntOrNull()
    val shown = if (asInt != null) "${rememberAnimatedInt(asInt)}" else value
    Column(
        modifier
            .background(TileBg, RoundedCornerShape(14.dp))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(shown, color = accent, fontSize = 19.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = TextDim, fontSize = 10.sp, maxLines = 1)
    }
}

@Composable
fun StatTileGrid(tiles: List<Triple<String, String, Color>>) {
    Column(Modifier.fillMaxWidth()) {
        tiles.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (label, value, accent) ->
                    StatTile(label, value, accent, Modifier.weight(1f))
                }
                repeat(3 - row.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        }
    }
}

/** Simple filled line chart (e.g. runs per season). */
@Composable
fun LineChart(values: List<Int>, modifier: Modifier = Modifier, color: Color = GoldAccent) {
    if (values.isEmpty()) return
    androidx.compose.foundation.Canvas(modifier) {
        val maxV = (values.maxOrNull() ?: 1).coerceAtLeast(1).toFloat()
        val n = values.size
        val stepX = if (n > 1) size.width / (n - 1) else size.width
        val pts = values.mapIndexed { i, v ->
            androidx.compose.ui.geometry.Offset(
                i * stepX,
                size.height - (v / maxV) * size.height * 0.9f - size.height * 0.05f
            )
        }
        // baseline
        drawLine(TextDim.copy(alpha = 0.25f),
            androidx.compose.ui.geometry.Offset(0f, size.height),
            androidx.compose.ui.geometry.Offset(size.width, size.height), 1.5f)
        // filled area
        val area = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, size.height)
            pts.forEach { lineTo(it.x, it.y) }
            lineTo(size.width, size.height); close()
        }
        drawPath(area, androidx.compose.ui.graphics.Brush.verticalGradient(
            listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.02f))))
        // line
        val line = androidx.compose.ui.graphics.Path().apply {
            pts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(line, color, style = androidx.compose.ui.graphics.drawscope.Stroke(3f))
        pts.forEach { drawCircle(color, 3.5f, it) }
    }
}

/** A labelled ₹/week budget slider with a live effect readout. */
@Composable
fun BudgetSlider(
    emoji: String,
    label: String,
    blurb: String,
    value: Long,
    max: Long,
    country: String,
    onChange: (Long) -> Unit
) {
    // drag locally (smooth, no global state thrash) and commit once on release
    var pos by androidx.compose.runtime.remember(value) {
        androidx.compose.runtime.mutableFloatStateOf(value.toFloat())
    }
    val shown = pos.toLong()
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("$emoji $label", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f))
            Text(
                if (shown <= 0) "off" else com.mohithash.cricketlegend.engine.Money.fmt(shown, country) + "/wk",
                color = if (shown <= 0) TextDim else GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
        androidx.compose.material3.Slider(
            value = pos,
            onValueChange = { pos = it },
            onValueChangeFinished = { onChange(pos.toLong()) },
            valueRange = 0f..max.toFloat(),
            steps = 9,
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = GoldAccent,
                activeTrackColor = PitchGreen,
                inactiveTrackColor = TileBg
            )
        )
        Text(blurb, color = TextDim, fontSize = 10.sp)
    }
}

/** Achievement/ceremony badge chip. */
@Composable
fun AchievementBadge(text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .background(
                androidx.compose.ui.graphics.Brush.horizontalGradient(
                    listOf(GoldDeep.copy(alpha = 0.30f), TileBg)),
                RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = TextPrimary, fontSize = 12.sp)
    }
}
