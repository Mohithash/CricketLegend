package com.mohithash.cricketlegend.ui

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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.engine.Progression
import androidx.compose.foundation.layout.width
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.StatKey

fun fmtFollowers(n: Long): String = when {
    n >= 1_000_000_000 -> "%.1fB".format(n / 1_000_000_000.0)
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
    n >= 1_000 -> "%.0fK".format(n / 1_000.0)
    else -> "$n"
}

fun imageLabel(image: Double): String = when {
    image >= 30 -> "National Treasure"
    image >= 12 -> "Fan Favourite"
    image >= -12 -> "Neutral"
    image >= -30 -> "Controversial"
    else -> "Villain of the Piece"
}

@Composable
fun HomeScreen(s: GameState, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Gradient hero header
        val chips = buildList {
            if (s.captainNation) add("CAPTAIN" to GoldAccent)
            if (s.inNationalTest) add("TEST" to GoldAccent)
            if (s.inNationalODI) add("ODI" to WinGreen)
            if (s.inNationalT20) add("T20I" to PitchGreen)
            s.franchiseTeam?.let { add((it.uppercase() + if (s.captainFranchise) " (C)" else "") to BallRed) }
            s.contractGrade?.let { add("GRADE $it" to TextDim) }
        }
        HeroHeader(
            name = if (s.nickname.isNotEmpty()) "${s.playerName} \"${s.nickname}\"" else s.playerName,
            subtitle = "${s.role.label} · ${s.country} · Age ${s.age} · ${s.year} W${s.week}",
            status = com.mohithash.cricketlegend.engine.Legacy.iconStatus(s),
            chips = chips,
            rightValue = Money.fmt(s.money, s.country)
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            // Career stat tiles
            StatTileGrid(listOf(
                Triple("Runs (all)", "${s.intlRuns + s.stat(StatKey.LEAGUE).runs}", GoldAccent),
                Triple("100s", "${s.intlHundreds}", WinGreen),
                Triple("Wickets", "${s.intlWickets}", Violet),
                Triple("Trophies", "${s.trophies.size}", GoldAccent),
                Triple("Records", "${s.brokenRecords.size}", BallRed),
                Triple("Followers", fmtFollowers(s.followers), WinGreen)
            ))

            // Skills + radar
            InfoCard {
                Row {
                    Column(Modifier.weight(1f)) {
                        SkillBar("Batting", s.batting)
                        if (s.bowling > 25) SkillBar("Bowling", s.bowling)
                        SkillBar("Fitness", s.fitness, color = WinGreen)
                        SkillBar("Sharpness", s.sharpness, color = if (s.sharpness > 50) WinGreen else LossRed)
                        SkillBar("Form", s.form + 5, 10.0, color = if (s.form >= 0) WinGreen else LossRed)
                        SkillBar("Fame", s.fame, color = GoldAccent)
                    }
                    Spacer(Modifier.width(8.dp))
                    RadarChart(
                        labels = listOf("Bat", "Pace", "Spin", "Pow", "Fit"),
                        values = listOf(s.batting, s.vsPace, s.vsSpin, s.power, s.fitness),
                        modifier = Modifier.width(120.dp).height(120.dp)
                    )
                }
                val ranks = listOf("INTL_TEST" to "Test", "INTL_ODI" to "ODI", "INTL_T20" to "T20I")
                    .mapNotNull { (k, label) -> s.rankPoints[k]?.let { "$label #${Progression.rankOf(it)}" } }
                if (ranks.isNotEmpty()) KeyValueRow("ICC Rankings", ranks.joinToString("  ·  "), WinGreen)
                KeyValueRow("Public image", imageLabel(s.publicImage))
                s.partner?.let { p ->
                    KeyValueRow(
                        if (s.married) "Family" else "Partner",
                        "$p${if (s.kids > 0) " + ${s.kids} kid${if (s.kids > 1) "s" else ""}" else ""} · ❤ ${s.relationship.toInt()}%"
                    )
                }
            }

            // Career trajectory graph — cumulative runs season by season
            if (s.careerRunsBySeason.size >= 2) {
                SectionHeader("Career Trajectory — Cumulative Runs")
                InfoCard {
                    LineChart(
                        values = s.careerRunsBySeason,
                        modifier = Modifier.fillMaxWidth().height(90.dp)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Season 1", color = TextDim, fontSize = 10.sp)
                        Text("${s.careerRunsBySeason.last()} runs", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Now", color = TextDim, fontSize = 10.sp)
                    }
                }
            }

        if (s.retired) {
            RetirementCard(s)
        } else if (s.seasonOver) {
            InfoCard {
                Text("Season ${s.year} complete!", fontWeight = FontWeight.Bold, color = GoldAccent, fontSize = 16.sp)
                Text("Contracts, auctions and call-ups will be decided now.", color = TextDim, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                Button(onClick = { Game.startNewSeason() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Start ${s.year + 1} Season", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            val fx = s.nextFixture()
            if (fx != null) {
                InfoCard {
                    Text("NEXT MATCH", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        fx.tournament ?: "${StatKey.label(fx.statKey)} vs ${fx.opponent}",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                    )
                    Text("vs ${fx.opponent} · Week ${fx.week} · ${StatKey.label(fx.statKey)}",
                        color = TextDim, fontSize = 12.sp)
                    if (s.injuryWeeksLeft > 0) {
                        Text("Injured — out ${s.injuryWeeksLeft} more week(s); playing will skip fixtures.",
                            color = LossRed, fontSize = 12.sp)
                    }

                    // immersive build-up
                    val preview = com.mohithash.cricketlegend.engine.MatchPreview.build(s, fx)
                    Spacer(Modifier.height(8.dp))
                    preview.lines.forEach {
                        Text("• $it", color = TextDim, fontSize = 11.sp,
                            modifier = Modifier.padding(vertical = 1.dp))
                    }
                    preview.warning?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = LossRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    preview.stakes?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, color = GoldAccent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(Modifier.height(10.dp))
                    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            Game.playNextMatch()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                        shape = RoundedCornerShape(10.dp),
                        enabled = s.pendingEvent == null
                    ) {
                        Text("▶  PLAY MATCH", fontWeight = FontWeight.Black, color = TextPrimary, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { Game.simRestOfSeason() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = s.pendingEvent == null
                    ) {
                        Text("⏩ Sim rest of season", color = GoldAccent, fontSize = 13.sp)
                    }
                }
            }
        }

        if (!s.retired && s.age >= 32) {
            val active = listOf(
                "TEST" to s.inNationalTest, "ODI" to s.inNationalODI, "T20I" to s.inNationalT20
            ).filter { it.second && it.first !in s.retiredFormats }
            if (active.isNotEmpty()) {
                SectionHeader("Manage Workload")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    active.forEach { (fmt, _) ->
                        OutlinedButton(onClick = { Game.retireFromFormat(fmt) }) {
                            Text("Quit $fmt", color = LossRed, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        if (!s.retired && s.age >= 30) {
            OutlinedButton(onClick = { Game.retire() }, modifier = Modifier.fillMaxWidth()) {
                Text("Announce Retirement", color = LossRed)
            }
        }

        // Live bilateral series scoreboard
        s.series?.let { sr ->
            if (sr.length >= 2) {
                SectionHeader("Series in Progress")
                InfoCard {
                    Text("${StatKey.label(sr.statKey)} vs ${sr.opponent}", color = TextPrimary,
                        fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${s.country}  ${sr.wins} — ${sr.losses}  ${sr.opponent}   (match ${sr.played}/${sr.length})",
                        color = if (sr.wins >= sr.losses) WinGreen else LossRed,
                        fontWeight = FontWeight.Black, fontSize = 16.sp)
                }
            }
        }

        // Board objectives with live progress
        if (s.seasonObjectives.isNotEmpty() && !s.retired) {
            SectionHeader("Season Objectives")
            InfoCard {
                s.seasonObjectives.forEach { o ->
                    val (cur, target) = com.mohithash.cricketlegend.engine.Series.objectiveProgress(s, o)
                    val done = cur >= target
                    Text((if (done) "✔ " else "• ") + o.label,
                        color = if (done) WinGreen else TextPrimary, fontSize = 12.sp,
                        fontWeight = if (done) FontWeight.Bold else FontWeight.Normal)
                    if (!done && target > 1) {
                        SkillBar("${cur.coerceAtLeast(0)} / $target",
                            cur.coerceAtLeast(0).toDouble(), target.toDouble(), color = GoldAccent)
                    }
                }
            }
        }

        SectionHeader("Icon Status")
        InfoCard {
            val goat = com.mohithash.cricketlegend.engine.LifeSystems.goatScore(s)
            Text(
                com.mohithash.cricketlegend.engine.Legacy.iconStatus(s),
                color = if (goat >= 2500) GoldAccent else TextPrimary,
                fontWeight = FontWeight.Black, fontSize = 18.sp
            )
            val (prog, band) = com.mohithash.cricketlegend.engine.Legacy.iconProgress(s)
            SkillBar("Legacy $goat  ($band)", prog * 100, 100.0, color = GoldAccent)
            com.mohithash.cricketlegend.engine.LifeSystems.archRivalScore(s)?.let { (me, rival) ->
                Spacer(Modifier.height(6.dp))
                Text("Arch-Rival: ${s.archRival}", color = BallRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                KeyValueRow("You vs Rival (legacy)", "$me — $rival",
                    if (me >= rival) WinGreen else LossRed)
            }
        }

        SectionHeader("News Feed")
        var newsExpanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
        InfoCard {
            if (s.news.isEmpty()) Text("No news yet. Go make headlines!", color = TextDim, fontSize = 13.sp)
            s.news.take(if (newsExpanded) 25 else 6).forEach {
                Text("• $it", color = TextPrimary, fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 3.dp))
            }
            if (s.news.size > 6) {
                OutlinedButton(onClick = { newsExpanded = !newsExpanded }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (newsExpanded) "Show less ▲" else "Show ${s.news.size - 6} more ▼",
                        color = GoldAccent, fontSize = 12.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        } // end inner padded Column
    }
}

@Composable
private fun RetirementCard(s: GameState) {
    InfoCard {
        Text("CAREER COMPLETE", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Spacer(Modifier.height(6.dp))
        Text(com.mohithash.cricketlegend.engine.Legacy.iconStatus(s), fontSize = 16.sp,
            fontWeight = FontWeight.Bold, color = TextPrimary)
        KeyValueRow("Legacy score", "${s.legacyScore}")
        KeyValueRow("International runs", "${s.intlRuns}")
        KeyValueRow("International hundreds", "${s.intlHundreds}")
        KeyValueRow("World records broken", "${s.brokenRecords.size}", GoldAccent)
        KeyValueRow("Trophies", "${s.trophies.size}")
        KeyValueRow("Life milestones", "${s.lifeAchievements.size}", GoldAccent)
        KeyValueRow("Net worth", Money.fmt(s.money, s.country), GoldAccent)
    }

    // --- Second career: what comes next ---
    if (s.secondCareer == null) {
        SectionHeader("The Next Innings")
        InfoCard {
            Text("Retirement isn't the end. Choose your second act:", color = TextDim, fontSize = 12.sp)
            com.mohithash.cricketlegend.engine.Legacy.secondCareers.forEach { (path, desc) ->
                Button(
                    onClick = { Game.chooseSecondCareer(path) },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                ) { Text("$path — $desc", fontSize = 11.sp, color = TextPrimary) }
            }
        }
    } else {
        SectionHeader("Life as a ${s.secondCareer}")
        InfoCard {
            Text("Age ${s.age} · ${s.year}", color = TextDim, fontSize = 12.sp)
            KeyValueRow("Net worth", Money.fmt(s.money, s.country), GoldAccent)
            Spacer(Modifier.height(8.dp))
            if (s.age < 75) {
                Button(onClick = { Game.advanceRetiredYear() }, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)) {
                    Text("Live another year", fontWeight = FontWeight.Bold)
                }
            } else {
                Text("A long, storied life, fully lived.", color = GoldAccent, fontWeight = FontWeight.Bold)
            }
        }
        if (s.childNames.isNotEmpty()) {
            SectionHeader("Continue the Dynasty")
            InfoCard {
                Text("Your child ${s.childNames.first()} ${s.playerName.split(" ").last()} carries the bloodline.",
                    color = TextPrimary, fontSize = 13.sp)
                Text("Start a new prodigy career as your child — inheriting name, wealth and elite talent.",
                    color = TextDim, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { Game.continueAsChild(s.role, s.playstyle, s.formatFocus) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                ) { Text("Continue as your child (Gen ${s.generation + 1})", color = DeepNavy, fontWeight = FontWeight.Bold) }
            }
        }
    }

    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = { Game.abandonCareer() }, modifier = Modifier.fillMaxWidth()) {
        Text("Start a fresh career (new bloodline)", color = TextDim)
    }
}
