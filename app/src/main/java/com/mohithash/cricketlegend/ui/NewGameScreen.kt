package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Role

@Composable
fun NewGameScreen() {
    var name by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("India") }
    var role by remember { mutableStateOf(Role.BATTER) }
    var startAge by remember { mutableIntStateOf(18) }
    var playstyle by remember { mutableStateOf("Balanced") }
    var statTier by remember { mutableStateOf("Talented") }
    var formatFocus by remember { mutableStateOf("All") }

    Column(
        Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Text("CRICKET LEGEND", color = GoldAccent, fontSize = 30.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Text("From gully cricket to the record books",
            color = TextDim, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))

        SectionHeader("Your Name")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("Leave blank for a random name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SectionHeader("Country")
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(260.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(RealData.teams) { team ->
                val selected = team.name == country
                Button(
                    onClick = { country = team.name },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) PitchGreen else CardNavy
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(team.name, color = TextPrimary, fontSize = 12.sp, maxLines = 1)
                }
            }
        }

        SectionHeader("Role")
        Role.entries.forEach { r ->
            val selected = r == role
            Button(
                onClick = { role = r },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selected) PitchGreen else CardNavy
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(r.label, color = TextPrimary)
            }
        }

        SectionHeader("Start Age")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                9 to "Prodigy (9)", 14 to "Teen star (14)", 18 to "Debutant (18)"
            ).forEach { (a, lbl) ->
                Button(
                    onClick = { startAge = a },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (startAge == a) PitchGreen else CardNavy),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) { Text(lbl, color = TextPrimary, fontSize = 11.sp) }
            }
        }
        if (startAge < 18) {
            Text(
                "The Vaibhav path: raw skills, years of age-group cricket, but chase the youngest-centurion records and a longer run at GOAT status.",
                color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
            )
        }

        SectionHeader("Playing Style")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(
                "Aggressive" to "Fearless hitting, high risk",
                "Balanced" to "All-round game",
                "Defensive" to "Patient, low risk"
            ).forEach { (st, desc) ->
                Button(
                    onClick = { playstyle = st },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = if (playstyle == st) PitchGreen else CardNavy),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(4.dp)
                ) { Text(st, color = TextPrimary, fontSize = 11.sp) }
            }
        }

        SectionHeader("Starting Ability")
        listOf(
            "Rookie" to "Raw and unproven — the grind of a lifetime",
            "Talented" to "A promising prospect (default)",
            "Superstar" to "Elite from day one",
            "GOD MODE" to "Max stats — invincible, break records every game"
        ).forEach { (tier, desc) ->
            val sel = statTier == tier
            Button(
                onClick = { statTier = tier },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (sel) (if (tier == "GOD MODE") GoldAccent else PitchGreen) else CardNavy
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("$tier — $desc", fontSize = 11.sp,
                    color = if (sel && tier == "GOD MODE") DeepNavy else TextPrimary)
            }
        }

        SectionHeader("Format Focus")
        listOf(
            "All" to "Play every format (Tests, ODIs, T20s)",
            "WhiteBall" to "White-ball only — no Tests",
            "T20Only" to "T20 & IPL only — the franchise life"
        ).forEach { (ff, desc) ->
            val sel = formatFocus == ff
            Button(
                onClick = { formatFocus = ff },
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = if (sel) PitchGreen else CardNavy),
                shape = RoundedCornerShape(10.dp)
            ) { Text("$desc", fontSize = 11.sp, color = TextPrimary) }
        }

        if (Game.hallOfFame.isNotEmpty()) {
            SectionHeader("Hall of Fame")
            Game.hallOfFame.sortedByDescending { it.legacyScore }.take(8).forEach { h ->
                Text(
                    "🏛 ${h.name} (${h.country}, ${h.role}) — ${h.legacyTitle}",
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
                Text(
                    "   Legacy ${h.legacyScore} · ${h.intlRuns} intl runs · ${h.intlHundreds} tons · " +
                        "${h.recordsBroken} records · ${h.trophies} trophies · retired ${h.retiredYear}",
                    color = TextDim, fontSize = 11.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { Game.newGame(name.trim(), country, role, startAge, playstyle, statTier, formatFocus) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("BEGIN CAREER", color = DeepNavy, fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.height(30.dp))
    }
}
