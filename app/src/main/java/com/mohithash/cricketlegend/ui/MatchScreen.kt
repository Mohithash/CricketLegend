package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.model.GameState

@Composable
fun MatchScreen(s: GameState, modifier: Modifier = Modifier) {
    val r = s.lastReport
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(10.dp))
        if (r == null) {
            InfoCard {
                Text("No match played yet.", color = TextDim)
                Text("Head to Home and hit PLAY MATCH.", color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            return@Column
        }

        r.headline?.let { headline ->
            InfoCard {
                Text("THE CRICKET TIMES", color = TextDim, fontSize = 10.sp,
                    fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                Text(headline, color = GoldAccent, fontSize = 19.sp, fontWeight = FontWeight.Black,
                    lineHeight = 24.sp)
                Text("— our correspondent, ${r.venue.ifEmpty { "the ground" }}",
                    color = TextDim, fontSize = 11.sp)
            }
        }

        InfoCard {
            Text(r.title, fontSize = 17.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text("${r.formatLabel} vs ${r.opponent}", color = TextDim, fontSize = 12.sp)
            if (r.venue.isNotEmpty()) {
                Text(r.venue, color = TextDim, fontSize = 11.sp)
                Text("${r.tossText} · ${r.pitch.lowercase().replaceFirstChar { it.uppercase() }} pitch",
                    color = TextDim, fontSize = 11.sp)
            }
            Spacer(Modifier.height(6.dp))
            Pill(if (r.won) "WON" else "LOST", if (r.won) WinGreen else LossRed)
            Spacer(Modifier.height(8.dp))
            KeyValueRow("Your team", r.teamScoreText)
            KeyValueRow(r.opponent, r.oppScoreText)
            KeyValueRow("Match rating", "%.1f / 10".format(r.rating),
                if (r.rating >= 7) WinGreen else if (r.rating < 4.5) LossRed else TextPrimary)
            if (r.matchFee > 0) KeyValueRow("Match fee", Money.fmt(r.matchFee, s.country), GoldAccent)
            if (r.manOfTheMatch) {
                Spacer(Modifier.height(4.dp))
                Pill("PLAYER OF THE MATCH", GoldAccent)
            }
        }

        SectionHeader("Your Performance")
        InfoCard {
            r.batting.forEachIndexed { i, b ->
                val inningsTag = if (r.batting.size > 1) "Innings ${i + 1}: " else ""
                Text(
                    "$inningsTag${b.runs}${if (!b.out) "*" else ""} (${b.balls}b, ${b.fours}x4, ${b.sixes}x6) — ${b.dismissal}",
                    color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            r.bowling.forEachIndexed { i, bw ->
                val inningsTag = if (r.bowling.size > 1) "Innings ${i + 1}: " else ""
                Text(
                    "$inningsTag${bw.oversText} ov, ${bw.runsConceded} runs, ${bw.wickets} wkt${if (bw.wickets == 1) "" else "s"}",
                    color = TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
            if (r.catches > 0) Text("Catches: ${r.catches}", color = TextPrimary, fontSize = 13.sp)
        }

        if (r.recordsBroken.isNotEmpty()) {
            SectionHeader("WORLD RECORDS BROKEN")
            InfoCard {
                r.recordsBroken.forEach {
                    Text("🏆 $it", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 3.dp))
                }
            }
        }

        if (r.keyMoment.isNotEmpty()) {
            SectionHeader("Key Moment — The Last Over")
            InfoCard {
                r.keyMoment.forEach {
                    Text(it, color = if (it.contains("FINISHES")) GoldAccent else TextPrimary,
                        fontSize = 13.sp, fontWeight = if (it.contains("SIX") || it.contains("FINISHES"))
                            FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        SectionHeader("Commentary")
        InfoCard {
            r.commentary.forEach {
                Text("“$it”", color = TextPrimary, fontSize = 13.sp,
                    modifier = Modifier.padding(vertical = 3.dp))
            }
        }

        s.tournament?.let { t ->
            if (!t.completed || r.tournament == t.name) {
                SectionHeader("${t.name} — Standings")
                InfoCard {
                    val order = com.mohithash.cricketlegend.engine.Tournaments.standingsOrder(t)
                    Row(Modifier.fillMaxWidth()) {
                        Text("Team", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("P", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp))
                        Text("W", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(26.dp))
                        Text("Pts", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                        Text("NRR", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(46.dp))
                    }
                    order.forEachIndexed { i, team ->
                        val rec = t.standings[team] ?: return@forEachIndexed
                        val mine = team == t.playerTeam
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text("${i + 1}. $team",
                                color = if (mine) GoldAccent else TextPrimary,
                                fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1f))
                            Text("${rec.played}", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(26.dp))
                            Text("${rec.wins}", color = TextDim, fontSize = 12.sp, modifier = Modifier.width(26.dp))
                            Text("${rec.points}", color = if (mine) GoldAccent else TextPrimary,
                                fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(30.dp))
                            Text("%+.2f".format(rec.netRunRate), color = TextDim, fontSize = 11.sp,
                                modifier = Modifier.width(46.dp))
                        }
                    }
                }
                SectionHeader(if (t.isLeague) "Orange & Purple Cap Race" else "Tournament Leaders")
                InfoCard {
                    val topBats = (t.runScorers + mapOf(s.playerName to t.playerRuns))
                        .entries.sortedByDescending { it.value }.take(4)
                    Text("Most runs", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    topBats.forEach { (name, runs) ->
                        KeyValueRow(name, "$runs", if (name == s.playerName) GoldAccent else TextPrimary)
                    }
                    if (t.playerWkts > 0 || t.wicketTakers.values.any { it > 0 }) {
                        Spacer(Modifier.height(6.dp))
                        Text("Most wickets", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        (t.wicketTakers + mapOf(s.playerName to t.playerWkts))
                            .entries.sortedByDescending { it.value }.take(4).forEach { (name, w) ->
                                KeyValueRow(name, "$w", if (name == s.playerName) GoldAccent else TextPrimary)
                            }
                    }
                }
            }
        }

        if (s.leagueHistory.isNotEmpty()) {
            SectionHeader("League History Book")
            InfoCard {
                s.leagueHistory.reversed().take(10).forEach {
                    Text(it, color = if (it.contains("champions")) GoldAccent else TextPrimary,
                        fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (!s.retired && !s.seasonOver) {
            Button(
                onClick = { Game.playNextMatch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                enabled = s.pendingEvent == null
            ) {
                Text("PLAY NEXT MATCH", fontWeight = FontWeight.Black, color = TextPrimary)
            }
        } else if (s.seasonOver && !s.retired) {
            Button(onClick = { Game.startNewSeason() }, modifier = Modifier.fillMaxWidth()) {
                Text("Season over — start ${s.year + 1}", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(20.dp))
    }
}
