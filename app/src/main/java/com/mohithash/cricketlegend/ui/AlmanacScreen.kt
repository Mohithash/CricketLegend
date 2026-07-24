package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Browses the bundled 100-year historical almanac (46MB of season archives:
 * tables, leaders, full match scorecards, career registers — all fictional).
 */
@Composable
fun AlmanacScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current
    var decade by remember { mutableIntStateOf(2016) }
    var seasonIdx by remember { mutableIntStateOf(0) }

    // load + split the decade file into season chunks on demand
    val chunks = remember(decade) {
        runCatching {
            ctx.assets.open("almanac/decade_$decade.alm").bufferedReader().readText()
                .split("@@SEASON|").drop(1)
                .map { chunk ->
                    val head = chunk.lineSequence().first()   // "Comp|Year"
                    head.replace("|", " ") to chunk.substringAfter("\n")
                }
        }.getOrElse { listOf("Error" to "Could not load almanac decade.") }
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text("📚 HISTORICAL ALMANAC", color = GoldAccent, fontWeight = FontWeight.Black,
                fontSize = 16.sp, modifier = Modifier.weight(1f))
            Button(onClick = onClose,
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy)) {
                Text("Close", color = TextDim, fontSize = 12.sp)
            }
        }
        Text("100 years of archives — every table, leader board and scorecard. (Fictional names.)",
            color = TextDim, fontSize = 10.sp)
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            (1926..2016 step 10).forEach { d ->
                Button(
                    onClick = { decade = d; seasonIdx = 0 },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (decade == d) GoldAccent else CardNavy),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.padding(2.dp)
                ) { Text("${d}s", fontSize = 10.sp, color = if (decade == d) DeepNavy else TextPrimary) }
            }
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            chunks.forEachIndexed { i, (title, _) ->
                Button(
                    onClick = { seasonIdx = i },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (seasonIdx == i) PitchGreen else CardNavy),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    modifier = Modifier.padding(1.dp)
                ) { Text(title, fontSize = 8.sp, color = TextPrimary, maxLines = 1) }
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            Text(
                chunks.getOrNull(seasonIdx)?.second ?: "",
                color = TextPrimary, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                lineHeight = 12.sp
            )
        }
    }
}
