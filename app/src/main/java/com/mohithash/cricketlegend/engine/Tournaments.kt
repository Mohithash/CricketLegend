package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.MatchReport
import com.mohithash.cricketlegend.model.Stage
import com.mohithash.cricketlegend.model.TeamRecord
import com.mohithash.cricketlegend.model.TournamentState
import kotlin.random.Random

/**
 * Runs standings tables, cap races, and the knockout ladder for the franchise
 * league (Qualifier 1 / Eliminator / Qualifier 2 / Final), World Cups,
 * Champions Trophy and WTC Final.
 */
object Tournaments {

    fun start(s: GameState, name: String, isLeague: Boolean, teams: List<String>, playerTeam: String, rng: Random) {
        val t = TournamentState(name, isLeague, playerTeam)
        t.teams.addAll(teams)
        teams.forEach { t.standings[it] = TeamRecord() }
        // seed cap-race rivals
        val rivalBats: List<String>
        val rivalBowls: List<String>
        if (isLeague) {
            rivalBats = listOf("Suryakumar Yadav", "Shubman Gill", "Rishabh Pant", "Travis Head", "Heinrich Klaasen")
            rivalBowls = listOf("Jasprit Bumrah", "Rashid Khan", "Matheesha Pathirana", "Kagiso Rabada")
        } else {
            val nations = teams.mapNotNull { n -> RealData.teams.firstOrNull { it.name == n } }
            rivalBats = nations.flatMap { it.starBatters.take(1) }.take(5)
            rivalBowls = nations.flatMap { it.starBowlers.take(1) }.take(4)
        }
        rivalBats.forEach { t.runScorers[it] = 0 }
        rivalBowls.forEach { t.wicketTakers[it] = 0 }
        s.tournament = t
    }

    /** Lazily initialises the right tournament when its first fixture arrives. */
    fun startFor(s: GameState, fx: Fixture, rng: Random) {
        val name = fx.tournament ?: return
        if (s.tournament?.name == name) return
        when {
            fx.statKey == com.mohithash.cricketlegend.model.StatKey.LEAGUE && name.startsWith("Premier") -> {
                val team = s.franchiseTeam ?: return
                start(s, name, true, RealData.franchiseTeams, team, rng)
            }
            name.startsWith("WTC Final") ->
                start(s, name, false, listOf(s.country, fx.opponent), s.country, rng)
            name.startsWith("Champions Trophy") -> {
                val top8 = RealData.teams.sortedByDescending { it.strength }.take(8).map { it.name }.toMutableList()
                if (s.country !in top8) { top8.removeAt(top8.size - 1); top8.add(s.country) }
                start(s, name, false, top8, s.country, rng)
            }
            name.contains("World Cup") ->
                start(s, name, false, RealData.teams.map { it.name }, s.country, rng)
        }
    }

    private fun rec(t: TournamentState, team: String) = t.standings.getOrPut(team) { TeamRecord() }

    private fun teamStrength(t: TournamentState, team: String): Int =
        if (t.isLeague) RealData.franchise(team)?.strength ?: 50
        else RealData.teams.firstOrNull { it.name == team }?.strength ?: 75

    /** Record the player's match and simulate the rest of the round. */
    fun afterPlayerMatch(s: GameState, fx: Fixture, report: MatchReport, rng: Random) {
        val t = s.tournament ?: return
        if (fx.tournament != t.name) return

        t.playerRuns += report.batting.sumOf { it.runs }
        t.playerWkts += report.bowling.sumOf { it.wickets }
        t.playerRatingSum += report.rating
        t.playerMatches++

        // derby night: beating your fated rival is worth more than two points
        if (t.isLeague && fx.opponent == s.derbyRival) {
            if (report.won) {
                s.fame = (s.fame + 2.0).coerceAtMost(100.0)
                s.followers += 1_000_000
                s.addNews("DERBY DELIGHT! Bragging rights over ${s.derbyRival} — the city is yours tonight.")
            } else {
                s.morale = (s.morale - 3).coerceAtLeast(5.0)
                s.addNews("Derby heartbreak against ${s.derbyRival}. The memes write themselves.")
            }
        }

        if (fx.stage == Stage.GROUP) {
            applyResult(t, t.playerTeam, fx.opponent, report.won, rng)
            // simulate the remaining fixtures of this round
            val others = t.teams.filter { it != t.playerTeam && it != fx.opponent }.shuffled(rng)
            var i = 0
            while (i + 1 < others.size) {
                val a = others[i]; val b = others[i + 1]
                val pA = 0.5 + (teamStrength(t, a) - teamStrength(t, b)) / 120.0
                applyResult(t, a, b, rng.nextDouble() < pA, rng)
                i += 2
            }
            // cap races tick along
            val batMax = if (t.isLeague) 75 else 90
            for (k in t.runScorers.keys) t.runScorers[k] = t.runScorers[k]!! + rng.nextInt(batMax)
            for (k in t.wicketTakers.keys) t.wicketTakers[k] = t.wicketTakers[k]!! + rng.nextInt(4)
        }
    }

    private fun applyResult(t: TournamentState, winnerIfTrue: String, other: String, firstWon: Boolean, rng: Random) {
        val w = if (firstWon) winnerIfTrue else other
        val l = if (firstWon) other else winnerIfTrue
        val margin = 0.05 + rng.nextDouble() * 0.35
        rec(t, w).apply { played++; wins++; points += 2; netRunRate += margin }
        rec(t, l).apply { played++; losses++; netRunRate -= margin }
    }

    fun standingsOrder(t: TournamentState): List<String> =
        t.standings.entries
            .sortedWith(compareByDescending<Map.Entry<String, TeamRecord>> { it.value.points }
                .thenByDescending { it.value.netRunRate })
            .map { it.key }

    /** Called after every tournament fixture; schedules knockouts / finishes the event. */
    fun advance(s: GameState, fx: Fixture, rng: Random) {
        val t = s.tournament ?: return
        if (fx.tournament != t.name || t.completed) return
        val nextId = (s.fixtures.maxOfOrNull { it.id } ?: 0) + 1

        fun addKnockout(stage: Stage, opponent: String) {
            s.fixtures.add(
                fx.copy(id = nextId, week = (fx.week + 1).coerceAtMost(52), opponent = opponent,
                    stage = stage, played = false, missed = false, won = null)
            )
            s.addNews("${t.name}: ${stage.label} vs $opponent up next!")
        }

        when (fx.stage) {
            Stage.GROUP -> {
                val groupGames = s.fixtures.filter { it.tournament == t.name && it.stage == Stage.GROUP }
                if (!groupGames.all { it.played || it.missed }) return
                val order = standingsOrder(t)
                val pos = order.indexOf(t.playerTeam) + 1
                s.addNews("${t.name}: group stage done — ${t.playerTeam} finish #$pos.")
                if (t.isLeague) when {
                    pos in 1..2 -> addKnockout(Stage.QUALIFIER1, if (pos == 1) order[1] else order[0])
                    pos in 3..4 -> addKnockout(Stage.ELIMINATOR, if (pos == 3) order[3] else order[2])
                    else -> finish(s, t, order[0], rng)
                } else when {
                    // semi-final pairing: 1v4, 2v3
                    pos in 1..4 -> addKnockout(Stage.SEMI_FINAL,
                        order[when (pos) { 1 -> 3; 2 -> 2; 3 -> 1; else -> 0 }])
                    else -> finish(s, t, order[0], rng)
                }
            }
            Stage.QUALIFIER1 -> {
                val order = standingsOrder(t)
                if (fx.won == true) addKnockout(Stage.FINAL, simulatedQ2Winner(order, rng))
                else addKnockout(Stage.QUALIFIER2, simulatedEliminatorWinner(order, rng))
            }
            Stage.ELIMINATOR -> {
                val order = standingsOrder(t)
                if (fx.won == true) addKnockout(Stage.QUALIFIER2, order[if (rng.nextBoolean()) 0 else 1])
                else finish(s, t, order[0], rng)
            }
            Stage.QUALIFIER2 -> {
                val order = standingsOrder(t)
                if (fx.won == true) addKnockout(Stage.FINAL, order[if (rng.nextBoolean()) 0 else 1])
                else finish(s, t, order[0], rng)
            }
            Stage.SEMI_FINAL -> {
                val order = standingsOrder(t)
                if (fx.won == true) addKnockout(Stage.FINAL, order.filter { it != t.playerTeam }.take(3).random(rng))
                else finish(s, t, order.filter { it != t.playerTeam }.random(rng), rng)
            }
            Stage.FINAL -> {
                if (fx.won == true) {
                    s.trophies.add(t.name)
                    s.fame = (s.fame + 10.0).coerceAtMost(100.0)
                    s.followers += (s.fame * s.fame * 400).toLong()
                    val prize = prizeFor(t.name)
                    Finance.credit(s, "${t.name} — WINNERS' prize", prize, taxable = true)
                    s.addNews("CHAMPIONS! ${t.playerTeam} win the ${t.name}!")
                    finish(s, t, t.playerTeam, rng)
                } else {
                    s.fame = (s.fame + 3.0).coerceAtMost(100.0)
                    s.addNews("${t.name}: runners-up after a hard-fought final.")
                    finish(s, t, fx.opponent, rng)
                }
            }
            null -> {}
        }
    }

    private fun simulatedQ2Winner(order: List<String>, rng: Random) =
        order.drop(2).take(2).let { if (it.isEmpty()) order.last() else it.random(rng) }

    private fun simulatedEliminatorWinner(order: List<String>, rng: Random) =
        order.drop(2).take(2).let { if (it.isEmpty()) order.last() else it.random(rng) }

    private fun prizeFor(name: String): Long = when {
        name.startsWith("ODI World Cup") -> 50_000_000
        name.startsWith("T20 World Cup") -> 45_000_000
        name.startsWith("Champions Trophy") -> 40_000_000
        name.startsWith("WTC Final") -> 35_000_000
        else -> 30_000_000
    }

    private fun finish(s: GameState, t: TournamentState, champion: String, rng: Random) {
        t.completed = true
        t.champion = champion

        // Orange / Purple cap races
        val bestRivalRuns = t.runScorers.values.maxOrNull() ?: 0
        val bestRivalWkts = t.wicketTakers.values.maxOrNull() ?: 0
        if (t.playerMatches >= 4 && t.playerRuns > bestRivalRuns) {
            val cap = if (t.isLeague) "Orange Cap" else "Top Run-Scorer"
            s.awards.add("${t.name} — $cap (${t.playerRuns} runs)")
            s.fame = (s.fame + 3.0).coerceAtMost(100.0)
            s.addNews("$cap! ${s.playerName} tops the ${t.name} run charts with ${t.playerRuns}.")
        }
        if (t.playerMatches >= 4 && t.playerWkts > bestRivalWkts && t.playerWkts > 0) {
            val cap = if (t.isLeague) "Purple Cap" else "Top Wicket-Taker"
            s.awards.add("${t.name} — $cap (${t.playerWkts} wkts)")
            s.fame = (s.fame + 3.0).coerceAtMost(100.0)
            s.addNews("$cap! ${s.playerName} leads the ${t.name} wickets column with ${t.playerWkts}.")
        }

        // Player of the Tournament
        if (t.playerMatches >= 5 && t.playerRatingSum / t.playerMatches >= 7.2) {
            s.awards.add("${t.name} — Player of the Tournament")
            s.fame = (s.fame + 5.0).coerceAtMost(100.0)
            s.addNews("${s.playerName} named Player of the Tournament at the ${t.name}!")
        }

        // Team of the Tournament + the league history book
        if (t.isLeague) {
            if (t.playerMatches >= 6 && t.playerRatingSum / t.playerMatches >= 6.3) {
                s.awards.add("${t.name} — Team of the Tournament XI")
                s.addNews("${s.playerName} is named in the ${t.name} Team of the Tournament.")
            }
            val pos = standingsOrder(t).indexOf(t.playerTeam) + 1
            val posTxt = when (pos) { 1 -> "champions"; 2 -> "runners-up"; else -> "#$pos" }
            s.leagueHistory.add("${t.name.takeLast(4)}: $champion won · ${t.playerTeam} $posTxt · you ${t.playerRuns} runs, ${t.playerWkts} wkts")
            while (s.leagueHistory.size > 25) s.leagueHistory.removeAt(0)
        }

        // single-World-Cup records
        if (t.name.startsWith("ODI World Cup")) {
            checkWcRecord(s, "wc_runs", t.playerRuns.toDouble())
            checkWcRecord(s, "wc_wickets", t.playerWkts.toDouble())
        }
        if (champion != t.playerTeam) {
            s.addNews("${t.name}: $champion are crowned champions.")
        }
    }

    private fun checkWcRecord(s: GameState, id: String, value: Double) {
        if (id in s.brokenRecords) return
        val rec = RealData.records.firstOrNull { it.id == id } ?: return
        if (value > WorldSim.dynValue(s, id)) {
            s.brokenRecords.add(id)
            s.dynamicRecords[id] = com.mohithash.cricketlegend.model.DynRecord(s.playerName, value)
            s.fame = (s.fame + 6.0).coerceAtMost(100.0)
            s.addNews("WORLD RECORD! ${rec.title} — ${s.playerName} surpasses ${WorldSim.dynHolder(s, id)}!")
        }
    }
}
