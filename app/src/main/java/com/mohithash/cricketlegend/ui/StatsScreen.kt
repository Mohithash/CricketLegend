package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.StatKey

@Composable
fun StatsScreen(s: GameState, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Stats", fontSize = 11.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Records", fontSize = 11.sp) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Honours", fontSize = 11.sp) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("World", fontSize = 11.sp) })
            Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Legacy", fontSize = 11.sp) })
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> StatsTab(s)
                1 -> RecordsTab(s)
                2 -> HonoursTab(s)
                3 -> WorldTab(s)
                else -> LegacyTab(s)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StatsTab(s: GameState) {
    for (key in StatKey.ALL) {
        val st = s.stats[key] ?: continue
        if (st.matches == 0) continue
        SectionHeader(StatKey.label(key))
        InfoCard {
            KeyValueRow("Matches / Innings", "${st.matches} / ${st.innings}")
            KeyValueRow("Runs", "${st.runs}", GoldAccent)
            KeyValueRow("Average / SR", "%.1f / %.1f".format(st.battingAverage, st.strikeRate))
            KeyValueRow("Highest", "${st.highest}${if (st.highestNotOut) "*" else ""}")
            KeyValueRow("100s / 50s", "${st.hundreds} / ${st.fifties}")
            if (st.doubleHundreds > 0) KeyValueRow("Double hundreds", "${st.doubleHundreds}")
            KeyValueRow("4s / 6s", "${st.fours} / ${st.sixes}")
            if (st.ballsBowled > 0) {
                KeyValueRow("Wickets", "${st.wickets}", GoldAccent)
                KeyValueRow("Bowling avg / Econ", "%.1f / %.2f".format(st.bowlingAverage, st.economy))
                KeyValueRow("Best bowling", "${st.bestBowlingWkts}/${st.bestBowlingRuns}")
                if (st.fiveWicketHauls > 0) KeyValueRow("5-wicket hauls", "${st.fiveWicketHauls}")
            }
            KeyValueRow("Catches", "${st.catches}")
            if (st.manOfTheMatch > 0) KeyValueRow("Player of the Match", "${st.manOfTheMatch}", GoldAccent)
        }
    }
    if (s.stats.values.all { it.matches == 0 }) {
        InfoCard { Text("Play some matches to build your career stats.", color = TextDim) }
    }
    StatsHub(s)
}

@Composable
private fun StatsHub(s: GameState) {
    val prog = com.mohithash.cricketlegend.engine.Progression
    if (s.splitRuns.isEmpty()) return

    StatTileGrid(listOf(
        Triple("Fantasy Pts", "${s.fantasyPoints}", GoldAccent),
        Triple("Best Stand", if (s.bestPartnership > 0) "${s.bestPartnership}" else "-", WinGreen),
        Triple("100 conv %", "${conversionPct(s)}%", Violet)
    ))

    SectionHeader("Average by Opposition")
    InfoCard {
        val opps = s.splitRuns.keys.filter { it.startsWith("opp:") }
            .sortedByDescending { s.splitRuns[it] ?: 0 }.take(12)
        if (opps.isEmpty()) Text("No international innings yet.", color = TextDim, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth()) {
            Text("Opponent", color = TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
            Text("Runs", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(50.dp))
            Text("Avg", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(44.dp))
            Text("SR", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(44.dp))
        }
        opps.forEach { k ->
            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                Text(k.removePrefix("opp:"), color = TextPrimary, fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Text("${s.splitRuns[k]}", color = GoldAccent, fontSize = 11.sp, modifier = Modifier.width(50.dp))
                Text("%.1f".format(prog.splitAverage(s, k)), color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(44.dp))
                Text("%.0f".format(prog.splitStrikeRate(s, k)), color = TextDim, fontSize = 11.sp, modifier = Modifier.width(44.dp))
            }
        }
    }

    SectionHeader("Conditions Splits")
    InfoCard {
        val rows = listOf(
            "loc:home" to "Home", "loc:away" to "Away",
            "pitch:FLAT" to "Flat decks", "pitch:PACE" to "Pace decks",
            "pitch:SPIN" to "Spin decks", "pitch:GREEN" to "Green tops"
        )
        rows.forEach { (k, label) ->
            if ((s.splitRuns[k] ?: 0) > 0)
                KeyValueRow(label, "${s.splitRuns[k]} runs · avg %.1f · SR %.0f"
                    .format(prog.splitAverage(s, k), prog.splitStrikeRate(s, k)))
        }
    }

    SectionHeader("Dismissal Breakdown")
    InfoCard {
        val total = s.dismissalTypes.values.sum().coerceAtLeast(1)
        s.dismissalTypes.entries.sortedByDescending { it.value }.forEach { (type, n) ->
            KeyValueRow(type, "$n  (${n * 100 / total}%)",
                if (type == "Not out") WinGreen else TextPrimary)
        }
    }

    if (s.battingAvgBySeason.size >= 2) {
        SectionHeader("Batting Average Progression")
        InfoCard {
            LineChart(s.battingAvgBySeason.map { it }, modifier = Modifier.fillMaxWidth().height(70.dp), color = GoldAccent)
            Text("Career international average over the seasons (×0.1). Now: %.1f"
                .format((s.battingAvgBySeason.lastOrNull() ?: 0) / 10.0), color = TextDim, fontSize = 10.sp)
        }
    }
    val testRank = s.rankHist["INTL_TEST"]
    if (testRank != null && testRank.size >= 2) {
        SectionHeader("ICC Test Ranking History")
        InfoCard {
            // invert so up = better (rank 1 at top)
            LineChart(testRank.map { 101 - it }, modifier = Modifier.fillMaxWidth().height(60.dp), color = WinGreen)
            Text("Higher = better. Best: World #${testRank.minOrNull()}", color = TextDim, fontSize = 10.sp)
        }
    }
    if (s.bestPartnership > 0) {
        InfoCard { KeyValueRow("Best partnership", "${s.bestPartnership} with ${s.bestPartnershipWith}", GoldAccent) }
    }
}

@Composable
private fun CompareRow(label: String, mine: Int, theirs: Int) {
    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
        Text("$mine", color = if (mine >= theirs) WinGreen else TextDim,
            fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(70.dp))
        Text(label, color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("$theirs", color = if (theirs >= mine) WinGreen else TextDim,
            fontSize = 12.sp, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, modifier = Modifier.width(70.dp))
    }
}

private fun conversionPct(s: GameState): Int {
    val fifties = StatKey.INTL.sumOf { s.stat(it).fifties }
    val tons = StatKey.INTL.sumOf { s.stat(it).hundreds }
    val starts = fifties + tons
    return if (starts > 0) tons * 100 / starts else 0
}

@Composable
private fun RecordsTab(s: GameState) {
    SectionHeader("Real World Records To Beat")
    Text("Data snapshot: mid-2026", color = TextDim, fontSize = 11.sp)
    for (rec in RealData.records) {
        val broken = rec.id in s.brokenRecords
        val holder = com.mohithash.cricketlegend.engine.WorldSim.dynHolder(s, rec.id)
        val bar = com.mohithash.cricketlegend.engine.WorldSim.dynValue(s, rec.id)
        val current = RealData.currentValue(rec.id, s)
        InfoCard {
            Text(rec.title, fontWeight = FontWeight.Bold, color = if (broken) GoldAccent else TextPrimary, fontSize = 14.sp)
            Text("$holder — ${bar.toInt()} ${rec.unit}" +
                (if (holder != rec.holder && holder != s.playerName) "  (was ${rec.holder}, ${rec.value.toInt()})" else ""),
                color = if (holder == s.playerName) GoldAccent else TextDim, fontSize = 12.sp)
            if (broken) {
                Spacer(Modifier.height(4.dp))
                Pill("RECORD BROKEN — YOURS!", GoldAccent)
            } else if (!rec.lowerIsBetter) {
                SkillBar("You: ${current.toInt()} ${rec.unit}", current, bar, color = PitchGreen)
            } else {
                Text("Score a century in under ${bar.toInt()} balls", color = TextDim, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun HonoursTab(s: GameState) {
    StatTileGrid(listOf(
        Triple("Trophies", "${s.trophies.size}", GoldAccent),
        Triple("Awards", "${s.awards.size}", WinGreen),
        Triple("Records", "${s.brokenRecords.size}", BallRed)
    ))
    SectionHeader("Trophy Cabinet")
    InfoCard {
        if (s.trophies.isEmpty()) Text("No silverware yet.", color = TextDim)
        s.trophies.forEach { AchievementBadge("🏆 $it") }
    }
    SectionHeader("Individual Awards")
    InfoCard {
        if (s.awards.isEmpty()) Text("The cabinet awaits its first medal.", color = TextDim)
        s.awards.reversed().forEach { AchievementBadge("🎖 $it") }
    }
    SectionHeader("Season History")
    InfoCard {
        if (s.history.isEmpty()) Text("Your journey starts now.", color = TextDim)
        s.history.reversed().forEach {
            Text("${it.year}: ${it.headline}", color = TextPrimary, fontSize = 12.sp, modifier = Modifier.padding(vertical = 2.dp))
        }
    }
}

@Composable
private fun WorldTab(s: GameState) {
    WorldDatabase(s)
    SectionHeader("ICC Player Rankings")
    InfoCard {
        val keys = listOf(StatKey.INTL_TEST, StatKey.INTL_ODI, StatKey.INTL_T20)
        var any = false
        for (k in keys) {
            val rp = s.rankPoints[k] ?: continue
            any = true
            val rank = com.mohithash.cricketlegend.engine.Progression.rankOf(rp)
            KeyValueRow(StatKey.label(k), "World #$rank", if (rank <= 3) GoldAccent else TextPrimary)
        }
        if (!any) Text("Play international cricket to enter the rankings.", color = TextDim, fontSize = 12.sp)
    }
    SectionHeader("Nemesis Watch")
    InfoCard {
        val rivals = s.dismissedBy.entries.sortedByDescending { it.value }.take(6)
        if (rivals.isEmpty()) Text("No bowler has your number. Yet.", color = TextDim, fontSize = 12.sp)
        rivals.forEachIndexed { i, (bowler, times) ->
            KeyValueRow(
                (if (i == 0 && times >= 3) "☠ " else "") + bowler,
                "$times dismissal${if (times > 1) "s" else ""}",
                if (i == 0 && times >= 3) LossRed else TextPrimary
            )
        }
        if ((rivals.firstOrNull()?.value ?: 0) >= 3) {
            Text("A century against your nemesis' team would feel extra sweet.",
                color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
    androidx.compose.material3.Button(
        onClick = { com.mohithash.cricketlegend.Game.almanacOpen = true },
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = CardNavy)
    ) { Text("📚 Open the 100-Year Historical Almanac", color = GoldAccent, fontSize = 12.sp) }

    if (s.worldLog.isNotEmpty()) {
        SectionHeader("World Season Review — squads decide results")
        InfoCard {
            s.worldLog.take(16).forEach {
                Text("• $it", color = if (it.contains("World Cup")) GoldAccent else TextPrimary,
                    fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }

    SectionHeader("Rival Watch — Your Generation")
    InfoCard {
        val top = s.rivals.filter { !it.retired }
            .sortedByDescending { if (it.isBowler) it.intlWkts * 30 else it.intlRuns }
            .take(6)
        if (top.isEmpty()) Text("The next generation is still in school.", color = TextDim, fontSize = 12.sp)
        top.forEach { r ->
            Text("${r.name} (${r.country}, ${r.age})", color = TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold)
            Text(
                if (r.isBowler) "   ${r.intlWkts} intl wickets · skill ${r.skill.toInt()}"
                else "   ${r.intlRuns} intl runs · ${r.hundreds} tons · skill ${r.skill.toInt()}",
                color = TextDim, fontSize = 11.sp
            )
        }
    }

    SectionHeader("Milestones Reached")
    InfoCard {
        if (s.milestonesSeen.isEmpty()) Text("The road is long. Walk it.", color = TextDim, fontSize = 12.sp)
        s.milestonesSeen.forEach { Text("✓ ${milestoneLabel(it)}", color = WinGreen, fontSize = 12.sp, modifier = Modifier.padding(vertical = 1.dp)) }
    }
}

@Composable
private fun LegacyTab(s: GameState) {
    SectionHeader("Icon Status")
    InfoCard {
        Text(com.mohithash.cricketlegend.engine.Legacy.iconStatus(s), color = GoldAccent,
            fontWeight = FontWeight.Black, fontSize = 20.sp)
        val goat = com.mohithash.cricketlegend.engine.LifeSystems.goatScore(s)
        KeyValueRow("Legacy score", "$goat")
        if (s.generation > 1) {
            KeyValueRow("Dynasty", "${s.dynastyName} — generation ${s.generation}")
            KeyValueRow("Inherited legacy", "${s.inheritedLegacy}")
        }
    }

    SectionHeader("What Comes Next — Record Projection")
    InfoCard {
        val projections = com.mohithash.cricketlegend.engine.Legacy.recordProjection(s)
        if (projections.isEmpty())
            Text("Play international cricket to start chasing history.", color = TextDim, fontSize = 12.sp)
        projections.forEach { p ->
            Text(p.label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("   ${p.current} / ${p.target} — about ${p.etaMatches} match${if (p.etaMatches == 1) "" else "es"} away",
                color = if (p.etaMatches <= 10) GoldAccent else TextDim, fontSize = 11.sp)
        }
    }

    if (s.legacyBySeason.size >= 2) {
        SectionHeader("Legacy Growth")
        InfoCard {
            LineChart(s.legacyBySeason, modifier = Modifier.fillMaxWidth().height(80.dp), color = GoldAccent)
        }
    }

    SectionHeader("Life Milestones")
    InfoCard {
        if (s.lifeAchievements.isEmpty())
            Text("The grand moments of a legendary life will appear here.", color = TextDim, fontSize = 12.sp)
        s.lifeAchievements.reversed().forEach {
            AchievementBadge(com.mohithash.cricketlegend.engine.Legacy.achievementText(s, it))
        }
    }
}

@Composable
private fun WorldDatabase(s: GameState) {
    var team by remember { mutableStateOf(s.country) }
    var view by remember { mutableIntStateOf(0) }  // 0 squads, 1 run leaders, 2 wicket leaders

    SectionHeader("World Database — ${com.mohithash.cricketlegend.data.PlayerDB.roster().size} players, ${RealData.teams.size} nations")
    Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(bottom = 4.dp)) {
        listOf("Squads", "Runs", "Wkts", "Leagues", "Compare").forEachIndexed { i, label ->
            androidx.compose.material3.Button(
                onClick = { view = i },
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = if (view == i) PitchGreen else CardNavy),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(1.dp)
            ) { Text(label, fontSize = 10.sp, color = TextPrimary) }
        }
    }

    when (view) {
        0 -> {
            // team picker (horizontally scrollable)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                com.mohithash.cricketlegend.engine.WorldSim.nationsByStrength().forEach { n ->
                    androidx.compose.material3.Button(
                        onClick = { team = n },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (team == n) GoldAccent else CardNavy),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.padding(2.dp)
                    ) { Text(n, fontSize = 10.sp, color = if (team == n) DeepNavy else TextPrimary) }
                }
            }
            InfoCard {
                val squad = com.mohithash.cricketlegend.engine.WorldSim.squad(s, team)
                Text("$team squad", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text("Player", color = TextDim, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    Text("Age", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(30.dp))
                    Text("Skill", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(34.dp))
                    Text("Runs/Wkts", color = TextDim, fontSize = 10.sp, modifier = Modifier.width(64.dp))
                }
                squad.forEach { r ->
                    val mine = r.name.startsWith("★")
                    Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                        Text((if (r.isBowler) "🎯 " else "🏏 ") + r.name,
                            color = if (mine) GoldAccent else TextPrimary, fontSize = 11.sp,
                            fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                            maxLines = 1, modifier = Modifier.weight(1f))
                        Text("${r.age}", color = TextDim, fontSize = 11.sp, modifier = Modifier.width(30.dp))
                        Text("${r.skill.toInt()}", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.width(34.dp))
                        Text(if (r.isBowler) "${r.intlWkts}w" else "${r.intlRuns}",
                            color = TextDim, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                    }
                }
            }
        }
        1 -> InfoCard {
            Text("Most International Runs", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            com.mohithash.cricketlegend.engine.WorldSim.topRunScorers(s, 15).forEachIndexed { i, (name, runs) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${i + 1}. $name", color = if (name.startsWith("★")) GoldAccent else TextPrimary,
                        fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("$runs", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        2 -> InfoCard {
            Text("Most International Wickets", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            com.mohithash.cricketlegend.engine.WorldSim.topWicketTakers(s, 15).forEachIndexed { i, (name, w) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${i + 1}. $name", color = if (name.startsWith("★")) GoldAccent else TextPrimary,
                        fontSize = 11.sp, maxLines = 1, modifier = Modifier.weight(1f))
                    Text("$w", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        4 -> {
            var rivalName by remember { mutableStateOf(s.rivals.firstOrNull { !it.retired }?.name ?: "") }
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                s.rivals.filter { !it.retired }.sortedByDescending { it.skill }.take(20).forEach { r ->
                    androidx.compose.material3.Button(
                        onClick = { rivalName = r.name },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (rivalName == r.name) GoldAccent else CardNavy),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                        modifier = Modifier.padding(2.dp)
                    ) { Text(r.name, fontSize = 9.sp, color = if (rivalName == r.name) DeepNavy else TextPrimary, maxLines = 1) }
                }
            }
            val rival = s.rivals.firstOrNull { it.name == rivalName }
            InfoCard {
                Text("You  vs  ${rivalName}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (rival != null) {
                    CompareRow("Skill", maxOf(s.batting, s.bowling).toInt(), rival.skill.toInt())
                    CompareRow("Age", s.age, rival.age)
                    CompareRow("Intl runs", s.intlRuns, rival.intlRuns)
                    CompareRow("Intl 100s", s.intlHundreds, rival.hundreds)
                    CompareRow("Intl wickets", s.intlWickets, rival.intlWkts)
                    CompareRow("Sixes", s.intlSixes, rival.sixes)
                    CompareRow("Matches", s.intlMatches, rival.matches)
                }
            }
        }
        else -> InfoCard {
            Text("Franchise T20 Leagues of the World", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                Text("Premier T20 League", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Text("India · 10 teams · Tier 1", color = GoldAccent, fontSize = 10.sp)
            }
            com.mohithash.cricketlegend.data.RealData.overseasLeagues.forEach { lg ->
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                    Text(lg.name, color = TextPrimary, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1)
                    Text("${lg.country} · ${lg.teams} · T${lg.tier}", color = TextDim, fontSize = 10.sp)
                }
            }
            Text("Chase overseas-league contracts via events after week 44 to play abroad.",
                color = TextDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

private fun milestoneLabel(id: String): String = when (id) {
    "intl_debut" -> "International debut"
    "intl_50" -> "50 internationals"
    "intl_100" -> "100 internationals"
    "intl_200" -> "200 internationals"
    "intl_300" -> "300 internationals"
    "intl_400" -> "400 internationals"
    "intl_500" -> "500 internationals"
    "test_100" -> "100th Test match"
    else -> id
}
