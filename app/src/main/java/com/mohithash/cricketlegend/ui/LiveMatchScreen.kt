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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.engine.LiveMatch
import com.mohithash.cricketlegend.engine.LiveMatchState
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.StatKey

@Composable
fun LiveMatchScreen(s: GameState, lm: LiveMatchState) {
    Column(
        Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .padding(16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            lm.fixture.tournament ?: "${StatKey.label(lm.fixture.statKey)} vs ${lm.fixture.opponent}",
            color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Text(lm.fixture.venue, color = TextDim, fontSize = 11.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))

        // scoreboard
        InfoCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${lm.teamRuns}/${lm.teamWkts}", color = TextPrimary,
                        fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("${lm.oversText} overs", color = TextDim, fontSize = 12.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${s.playerName.split(" ").last()} ${lm.playerRuns}${if (!lm.playerOut) "*" else ""} (${lm.playerBalls})",
                        color = GoldAccent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${lm.playerFours}x4  ${lm.playerSixes}x6", color = TextDim, fontSize = 11.sp)
                }
            }
            if (lm.chasing) {
                Spacer(Modifier.height(6.dp))
                val rrr = if (lm.ballsLeft > 0) lm.runsNeeded * 6.0 / lm.ballsLeft else 0.0
                Text(
                    "Need ${lm.runsNeeded} off ${lm.ballsLeft} (RRR %.1f)".format(rrr),
                    color = if (rrr > 10) LossRed else WinGreen,
                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                )
            }
        }

        // bowler
        if (!lm.inningsOver && !lm.playerOut) {
            InfoCard {
                Text("BOWLING", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    "${lm.currentBowler.name} — ${if (lm.currentBowler.isSpin) "spin" else "pace"}, skill ${lm.currentBowler.skill.toInt()}",
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        // ball log
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 6.dp)
        ) {
            lm.log.takeLast(9).reversed().forEach {
                Text(
                    it,
                    color = when {
                        it.startsWith("OUT") || it.contains("Wicket") -> LossRed
                        it.startsWith("SIX") || it.startsWith("FOUR") ||
                            it.contains("CENTURY") || it.contains("WINNING") -> GoldAccent
                        else -> TextPrimary
                    },
                    fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }

        if (lm.inningsOver || lm.playerOut) {
            Button(
                onClick = { Game.finishLiveMatch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                shape = RoundedCornerShape(12.dp)
            ) { Text("SEE MATCH RESULT", fontWeight = FontWeight.Black, color = TextPrimary) }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                ShotButton("DEFEND", CardNavy, Modifier.weight(1f)) { Game.liveBall(LiveMatch.DEFEND) }
                ShotButton("NORMAL", PitchGreen, Modifier.weight(1f)) { Game.liveBall(LiveMatch.NORMAL) }
                ShotButton("ATTACK", GoldAccent, Modifier.weight(1f)) { Game.liveBall(LiveMatch.ATTACK) }
                ShotButton("SLOG", BallRed, Modifier.weight(1f)) { Game.liveBall(LiveMatch.SLOG) }
            }
        }
        Spacer(Modifier.height(14.dp))
    }
}

@Composable
private fun ShotButton(label: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color),
        shape = RoundedCornerShape(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Black,
            color = if (color == GoldAccent) DeepNavy else TextPrimary)
    }
}
