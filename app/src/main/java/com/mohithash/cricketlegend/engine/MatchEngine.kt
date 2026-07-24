package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.BattingLine
import com.mohithash.cricketlegend.model.BowlingLine
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.MatchReport
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.Stage
import com.mohithash.cricketlegend.model.StatKey
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

object MatchEngine {

    fun matchFee(statKey: String): Long = when (statKey) {
        StatKey.YOUTH -> 20_000
        StatKey.DOM_FC -> 800_000
        StatKey.DOM_LIST_A -> 300_000
        StatKey.DOM_T20 -> 150_000
        StatKey.LEAGUE -> 0 // paid as auction salary
        StatKey.INTL_TEST -> 1_500_000
        StatKey.INTL_ODI -> 600_000
        StatKey.INTL_T20 -> 300_000
        else -> 0
    }

    fun simulate(s: GameState, fx: Fixture, rng: Random = Random.Default): MatchReport {
        val fitnessFactor = 0.75 + (s.fitness / 100.0) * 0.30
        val moraleFactor = 0.90 + (s.morale / 100.0) * 0.15
        val formFactor = 1.0 + s.form * 0.035
        // being under-cooked (low sharpness) hurts you at the crease
        val sharpFactor = 0.88 + (s.sharpness / 100.0) * 0.12
        // analytics & match prep (a weekly-budget dial) shaves the opposition's edge
        val effOppStrength = oppStrength(fx) + Difficulty.oppBump(s) - Allocations.analyticsEdge(s)
        val oppQuality = if (fx.level == Level.DOMESTIC) 0.95
            else 1.0 + (effOppStrength - 80) / 250.0

        // pitch interacts with technique sub-skills
        val batSkillBase = when (fx.pitch) {
            "PACE", "GREEN" -> 0.55 * s.batting + 0.45 * s.vsPace
            "SPIN" -> 0.55 * s.batting + 0.45 * s.vsSpin
            "FLAT" -> s.batting * 1.06
            else -> s.batting
        } * (if (fx.pitch == "GREEN") 0.96 else 1.0)

        val spinner = listOf("spin", "orthodox").any { s.bowlingStyle.lowercase().contains(it) }
        val bowlPitchFactor = when {
            fx.pitch == "SPIN" && spinner -> 1.12
            (fx.pitch == "PACE" || fx.pitch == "GREEN") && !spinner -> 1.12
            fx.pitch == "FLAT" -> 0.92
            else -> 1.0
        }

        // knockout nights weigh heavy — nerve comes from morale and form
        val pressure = if (fx.stage != null && fx.stage != Stage.GROUP)
            (0.92 + (s.morale / 100.0) * 0.10 + s.form * 0.008).coerceIn(0.85, 1.06)
        else 1.0

        // hot streak / slump from the last few hits
        val last3 = s.recentScores.takeLast(3)
        val streakMod = when {
            last3.size == 3 && last3.all { it >= 50 } -> 4.0
            last3.size == 3 && last3.all { it <= 8 } -> -3.0
            else -> 0.0
        }

        val effBat = ((batSkillBase + streakMod) * fitnessFactor * moraleFactor * formFactor * sharpFactor * pressure / oppQuality)
            .coerceIn(5.0, 108.0)
        val style = s.playstyle
        val effBowl = (s.bowling * bowlPitchFactor * fitnessFactor * moraleFactor * formFactor * sharpFactor * pressure / oppQuality)
            .coerceIn(5.0, 108.0)

        val inningsCount = if (fx.format == Format.FIRST_CLASS) 2 else 1
        val batting = ArrayList<BattingLine>()
        val bowling = ArrayList<BowlingLine>()
        repeat(inningsCount) {
            batting += simBattingInnings(s, fx, effBat, rng, style)
            if (s.role == Role.BOWLER || s.role == Role.ALL_ROUNDER) {
                bowling += simBowlingInnings(s, fx, effBowl, rng)
            }
        }
        // In FC cricket the second innings sometimes never happens
        val battingFinal = if (fx.format == Format.FIRST_CLASS && rng.nextDouble() < 0.35)
            batting.subList(0, 1).toList() else batting.toList()

        val catches = simCatches(s, rng)
        val totalRuns = battingFinal.sumOf { it.runs }
        val totalWickets = bowling.sumOf { it.wickets }

        val rating = rating(fx.format, battingFinal, bowling, catches)
        val won = decideResult(s, fx, rating, rng)
        val motm = won && rating >= 8.0 && fx.level != Level.DOMESTIC || rating >= 9.2

        var fee = matchFee(fx.statKey)
        if (motm && fx.level != Level.DOMESTIC) fee += 200_000

        val (teamScore, oppScore) = buildScores(fx.format, totalRuns, won, rng)
        val commentary = commentary(s, fx, battingFinal, bowling, totalWickets, won, motm, rng)

        // in limited-overs the winner usually chased (buildScores models the win as a chase);
        // in FC we treat your side as batting first. Toss text + scorecard order follow this.
        val playerBattedFirst = if (fx.format == Format.FIRST_CLASS) true else !won

        // full 11-a-side scorecard, consistent with the totals just computed
        fun firstInt(t: String) = Regex("\\d+").find(t)?.value?.toInt() ?: 0
        fun secondInt(t: String) = Regex("\\d+").findAll(t).elementAtOrNull(1)?.value?.toIntOrNull() ?: 6
        val scorecard = Scorecards.build(
            s, fx, battingFinal.firstOrNull(), bowling.firstOrNull(),
            teamTotal = firstInt(teamScore), teamWkts = secondInt(teamScore).coerceIn(0, 10),
            oppTotal = firstInt(oppScore), rng = rng, playerBattedFirst = playerBattedFirst
        )

        val stageTag = fx.stage?.let { " — ${it.label}" } ?: ""
        val title = (fx.tournament?.plus(stageTag) ?: "${fx.format.label} vs ${fx.opponent}")

        val tossText = if (playerBattedFirst) "Your side batted first"
            else "${fx.opponent} batted first; you chased"

        val best = battingFinal.maxByOrNull { it.runs }
        val headline = when {
            fx.stage == Stage.FINAL && won -> "CHAMPIONS! ${s.playerName.uppercase()} DELIVERS ON THE BIGGEST STAGE"
            (best?.runs ?: 0) >= 200 -> "DOUBLE TON! ${s.playerName.uppercase()} REWRITES THE COACHING MANUAL"
            (best?.runs ?: 0) >= 100 && motm -> "${s.playerName.uppercase()} CONQUERS ${fx.opponent.uppercase()}"
            totalWickets >= 5 -> "${s.playerName.uppercase()} RIPS THROUGH ${fx.opponent.uppercase()}"
            else -> null
        }

        val keyMoment = if (fx.format == Format.T20 && won && rng.nextDouble() < 0.45)
            lastOverChase(s, rng) else emptyList()

        return MatchReport(
            fixtureId = fx.id,
            title = title,
            formatLabel = StatKey.label(fx.statKey),
            opponent = fx.opponent,
            tournament = fx.tournament,
            batting = battingFinal,
            bowling = bowling.toList(),
            catches = catches,
            teamScoreText = teamScore,
            oppScoreText = oppScore,
            won = won,
            resultText = if (won) "Victory vs ${fx.opponent}!" else "Lost to ${fx.opponent}",
            rating = rating,
            manOfTheMatch = motm,
            matchFee = fee,
            commentary = commentary,
            venue = fx.venue,
            pitch = fx.pitch,
            tossText = tossText,
            headline = headline,
            keyMoment = keyMoment,
            scorecard = scorecard
        )
    }

    /** Builds the full match report around a ball-by-ball innings the player just batted. */
    fun simulateLive(s: GameState, fx: Fixture, lm: LiveMatchState, rng: Random = Random.Default): MatchReport {
        val line = LiveMatch.toBattingLine(lm)
        val batting = listOf(line)

        val fitnessFactor = 0.75 + (s.fitness / 100.0) * 0.30
        val effBowl = (s.bowling * fitnessFactor).coerceIn(5.0, 100.0)
        val bowling = if (s.role == Role.BOWLER || s.role == Role.ALL_ROUNDER)
            listOf(simBowlingInnings(s, fx, effBowl, rng)) else emptyList()
        val catches = simCatches(s, rng)

        val rating = rating(fx.format, batting, bowling, catches)
        val won = if (lm.chasing) lm.chaseWon else {
            val par = if (fx.format == Format.T20) 165 else 270
            val p = (0.5 + (lm.teamRuns - par) / (par * 0.55) + bowling.sumOf { it.wickets } * 0.04)
                .coerceIn(0.08, 0.92)
            rng.nextDouble() < p
        }
        val motm = won && rating >= 8.0 || rating >= 9.2
        var fee = matchFee(fx.statKey)
        if (motm && fx.level != Level.DOMESTIC) fee += 200_000

        val teamScore = "${lm.teamRuns}/${lm.teamWkts} (${lm.oversText} ov)"
        val oppScore = if (lm.chasing) "${lm.target - 1}/${4 + rng.nextInt(6)}"
        else if (won) "${lm.teamRuns - 1 - rng.nextInt(25)}/${7 + rng.nextInt(4)}"
        else "${lm.teamRuns + 1 + rng.nextInt(15)}/${3 + rng.nextInt(6)}"

        val totalWkts = bowling.sumOf { it.wickets }
        val stageTag = fx.stage?.let { " — ${it.label}" } ?: ""
        val headline = when {
            fx.stage == Stage.FINAL && won -> "CHAMPIONS! ${s.playerName.uppercase()} DELIVERS ON THE BIGGEST STAGE"
            line.runs >= 100 && won -> "${s.playerName.uppercase()} CONQUERS ${fx.opponent.uppercase()}"
            else -> null
        }
        return MatchReport(
            fixtureId = fx.id,
            title = (fx.tournament?.plus(stageTag) ?: "${fx.format.label} vs ${fx.opponent}"),
            formatLabel = StatKey.label(fx.statKey),
            opponent = fx.opponent,
            tournament = fx.tournament,
            batting = batting,
            bowling = bowling,
            catches = catches,
            teamScoreText = teamScore,
            oppScoreText = oppScore,
            won = won,
            resultText = if (won) "Victory vs ${fx.opponent}!" else "Lost to ${fx.opponent}",
            rating = rating,
            manOfTheMatch = motm,
            matchFee = fee,
            commentary = commentary(s, fx, batting, bowling, totalWkts, won, motm, rng),
            venue = fx.venue,
            pitch = fx.pitch,
            tossText = if (lm.chasing) "${fx.opponent} batted first" else "Your side batted first",
            headline = headline,
            keyMoment = lm.log.takeLast(6)
        )
    }

    /** Generates a nail-biting final-over chase that ends in victory. */
    private fun lastOverChase(s: GameState, rng: Random): List<String> {
        var need = 5 + rng.nextInt(9)
        val lines = ArrayList<String>()
        lines += "FINAL OVER — $need needed off 6. ${s.playerName} on strike."
        var ball = 1
        while (ball <= 6 && need > 0) {
            val hit = when {
                need > 8 && rng.nextDouble() < 0.5 -> 6
                need > 4 && rng.nextDouble() < 0.45 -> 4
                else -> listOf(0, 1, 1, 2, 4, 6).random(rng)
            }
            need -= hit
            val ballTxt = when {
                hit == 6 -> "SIX! Into the crowd!"
                hit == 4 -> "FOUR! Threaded through the gap!"
                hit == 0 -> "Dot ball. Hearts in mouths."
                else -> "$hit run${if (hit > 1) "s" else ""}, scampered hard."
            }
            lines += "Ball $ball: $ballTxt" + if (need > 0) " $need off ${6 - ball}." else ""
            ball++
        }
        if (need <= 0) lines += "${s.playerName.uppercase()} FINISHES IT! Scenes!"
        return lines
    }

    private fun oppStrength(fx: Fixture): Int =
        RealData.teams.firstOrNull { it.name == fx.opponent }?.strength ?: 75

    private fun simBattingInnings(s: GameState, fx: Fixture, effBat: Double, rng: Random,
                                  style: String = "Balanced"): BattingLine {
        val pos = s.battingPosition.coerceIn(1, 8)
        val ballCap = when (fx.format) {
            Format.T20 -> (84 - (pos - 1) * 9).coerceAtLeast(18)
            Format.ODI -> (185 - (pos - 1) * 20).coerceAtLeast(40)
            // declarations cap even the greatest innings — enough for a 400+ but not a 1200
            Format.FIRST_CLASS -> if (pos <= 7) 320 else 120
        }
        val pOutBase = when (fx.format) {
            Format.T20 -> 0.050
            Format.ODI -> 0.033
            Format.FIRST_CLASS -> 0.023
        }
        // playstyle: Aggressive (Vaibhav) = more boundaries, more risk; Defensive = safer, slower
        val (riskMult, boundaryMult) = when (style) {
            "Aggressive" -> 1.30 to 1.55
            "Defensive" -> 0.62 to 0.72
            else -> 1.0 to 1.0
        }
        // Class helps, but on Realistic even the best get out — no invincibility.
        // eliteMult (per difficulty) controls how far the old dominance curve survives.
        val elite = ((effBat - 70.0) / 30.0).coerceIn(0.0, 1.3) * Difficulty.eliteMult(s)
        val survival = 1.0 - elite * 0.80
        val pOutFloor = when (s.difficulty) { "Easy" -> 0.004; "Hardcore" -> 0.014; else -> 0.008 }
        val pOut = (pOutBase * (1.65 - effBat / 95.0) * riskMult * survival * Difficulty.dismissalMult(s))
            .coerceIn(pOutFloor, 0.22)

        // scoring weights: dot, 1, 2, 3, 4, 6 — raw power + class drive the boundary count
        val boundaryBoost = (0.42 + s.power / 340.0 + effBat / 460.0 + elite * 0.35) * boundaryMult
        val w = when (fx.format) {
            Format.T20 -> doubleArrayOf(0.29 - elite * 0.10, 0.37, 0.10, 0.01, 0.15 * boundaryBoost, 0.08 * boundaryBoost)
            Format.ODI -> doubleArrayOf(0.44 - elite * 0.12, 0.34, 0.09, 0.01, 0.07 * boundaryBoost, 0.025 * boundaryBoost)
            Format.FIRST_CLASS -> doubleArrayOf(0.61 - elite * 0.14, 0.24, 0.06, 0.01, 0.055 * boundaryBoost, 0.008 * boundaryBoost)
        }
        val total = w.sum()

        var runs = 0; var balls = 0; var fours = 0; var sixes = 0
        var out = false; var ballsAtHundred = 0
        while (balls < ballCap) {
            balls++
            // marathon innings tire everyone — big scores get progressively harder to extend
            val fatigue = 1.0 + (runs / 190.0) * (runs / 190.0) * 0.45 * (1.0 - elite * 0.4)
            if (rng.nextDouble() < pOut * fatigue) { out = true; break }
            var r = rng.nextDouble() * total
            var outcome = 0
            for (i in w.indices) { r -= w[i]; if (r <= 0) { outcome = i; break } }
            when (outcome) {
                1 -> runs += 1
                2 -> runs += 2
                3 -> runs += 3
                4 -> { runs += 4; fours++ }
                5 -> { runs += 6; sixes++ }
            }
            if (runs >= 100 && ballsAtHundred == 0) ballsAtHundred = balls
        }
        val dismissal = if (out) dismissalText(fx, rng) else "not out"
        return BattingLine(runs, balls, fours, sixes, out, dismissal, ballsAtHundred)
    }

    private fun simBowlingInnings(s: GameState, fx: Fixture, effBowl: Double, rng: Random): BowlingLine {
        val mainBowler = s.role == Role.BOWLER
        val overs = when (fx.format) {
            Format.T20 -> 4
            Format.ODI -> if (mainBowler) 10 else 6 + rng.nextInt(3)
            Format.FIRST_CLASS -> if (mainBowler) 16 + rng.nextInt(12) else 8 + rng.nextInt(8)
        }
        val balls = overs * 6
        val pWktBase = when (fx.format) {
            Format.T20 -> 0.036
            Format.ODI -> 0.031
            Format.FIRST_CLASS -> 0.026
        }
        // class helps, but wickets are earned — no auto-fifers on Realistic
        val elite = ((effBowl - 70.0) / 30.0).coerceIn(0.0, 1.3) * Difficulty.eliteMult(s)
        val wktCap = 7
        val pWkt = (pWktBase * (0.35 + effBowl / 145.0 + elite * 0.7)).coerceIn(0.005, 0.13)
        var wickets = 0
        repeat(balls) { if (rng.nextDouble() < pWkt && wickets < wktCap) wickets++ }
        val rpb = when (fx.format) {
            Format.T20 -> 1.40
            Format.ODI -> 0.92
            Format.FIRST_CLASS -> 0.54
        } * (1.38 - effBowl / 300.0 - s.control / 800.0 - elite * 0.25) * (0.85 + rng.nextDouble() * 0.3)
        val conceded = (balls * rpb).roundToInt().coerceAtLeast(0)
        return BowlingLine(balls, conceded, wickets.coerceAtMost(10))
    }

    private fun simCatches(s: GameState, rng: Random): Int {
        val keeper = s.role == Role.WICKET_KEEPER
        val p = (s.fielding / 100.0) * (if (keeper) 0.65 else 0.28)
        var c = 0
        repeat(3) { if (rng.nextDouble() < p / 2.2) c++ }
        return c
    }

    private fun rating(format: Format, bat: List<BattingLine>, bowl: List<BowlingLine>, catches: Int): Double {
        val par = when (format) { Format.T20 -> 32.0; Format.ODI -> 46.0; Format.FIRST_CLASS -> 58.0 }
        val wktWeight = when (format) { Format.T20 -> 1.5; Format.ODI -> 1.2; Format.FIRST_CLASS -> 0.85 }
        val runs = bat.sumOf { it.runs }.toDouble()
        val wkts = bowl.sumOf { it.wickets }.toDouble()
        val srBonus = if (format == Format.T20 && bat.isNotEmpty() && bat[0].balls > 12) {
            val sr = bat[0].runs * 100.0 / bat[0].balls
            ((sr - 130) / 130).coerceIn(-0.6, 0.8)
        } else 0.0
        return (3.0 + (runs / par) * 3.2 + wkts * wktWeight + catches * 0.3 + srBonus).coerceIn(1.0, 10.0)
    }

    private fun decideResult(s: GameState, fx: Fixture, rating: Double, rng: Random): Boolean {
        val myStrength = when (fx.level) {
            Level.DOMESTIC -> 50
            Level.FRANCHISE -> 50
            Level.INTERNATIONAL -> RealData.teams.firstOrNull { it.name == s.country }?.strength ?: 78
        }
        val opp = if (fx.level == Level.INTERNATIONAL) oppStrength(fx) else 46 + rng.nextInt(9)
        val p = (0.5 + (myStrength - opp) / 180.0 + (rating - 5.8) * 0.055).coerceIn(0.10, 0.90)
        return rng.nextDouble() < p
    }

    /**
     * Builds a scoreboard that is CONSISTENT with the result: the winning side's
     * line always reflects the win (higher limited-overs total, or a chase completed).
     * Returns (yourTeamText, oppText).
     */
    private fun buildScores(format: Format, playerRuns: Int, won: Boolean, rng: Random): Pair<String, String> {
        fun wkts() = 3 + rng.nextInt(7)
        return when (format) {
            Format.T20 -> {
                val ourTotal = (playerRuns + 95 + rng.nextInt(75)).coerceAtLeast(playerRuns + 20)
                val margin = 6 + rng.nextInt(40)
                if (won) {
                    // opponent set a total, you chased it down with wickets in hand
                    "${ourTotal}/${1 + rng.nextInt(6)} (${17 + rng.nextInt(4)} ov)" to
                        "${ourTotal - margin}/${wkts()} (20 ov)"
                } else {
                    // you posted a total, opponent chased it
                    "${ourTotal}/${wkts()} (20 ov)" to
                        "${ourTotal + margin}/${1 + rng.nextInt(6)} (${17 + rng.nextInt(4)} ov)"
                }
            }
            Format.ODI -> {
                val ourTotal = (playerRuns + 150 + rng.nextInt(120)).coerceAtLeast(playerRuns + 30)
                val margin = 8 + rng.nextInt(60)
                if (won) {
                    "${ourTotal}/${1 + rng.nextInt(6)} (${44 + rng.nextInt(6)} ov)" to
                        "${ourTotal - margin}/${wkts()} (50 ov)"
                } else {
                    "${ourTotal}/${wkts()} (50 ov)" to
                        "${ourTotal + margin}/${1 + rng.nextInt(6)} (${44 + rng.nextInt(6)} ov)"
                }
            }
            Format.FIRST_CLASS -> {
                val ourAgg = playerRuns + 220 + rng.nextInt(220)
                val margin = 30 + rng.nextInt(140)
                val ourText = "${ourAgg / 2 + rng.nextInt(60)} & ${ourAgg / 2}"
                val oppAgg = if (won) ourAgg - margin else ourAgg + margin
                val oppText = "${oppAgg / 2} & ${(oppAgg / 2 - rng.nextInt(60)).coerceAtLeast(80)}"
                ourText to oppText
            }
        }
    }

    private fun dismissalText(fx: Fixture, rng: Random): String {
        val bowler = if (fx.level == Level.INTERNATIONAL) {
            RealData.teams.firstOrNull { it.name == fx.opponent }?.starBowlers?.random(rng) ?: "the quick"
        } else {
            RealData.randomName("India")
        }
        return listOf("b $bowler", "c & b $bowler", "lbw b $bowler", "c (keeper) b $bowler", "run out").random(rng)
    }

    private fun commentary(
        s: GameState, fx: Fixture, bat: List<BattingLine>, bowl: List<BowlingLine>,
        wkts: Int, won: Boolean, motm: Boolean, rng: Random
    ): List<String> {
        val lines = ArrayList<String>()
        val best = bat.maxByOrNull { it.runs }
        if (best != null) {
            when {
                best.runs >= 200 -> lines += "DOUBLE CENTURY! ${s.playerName} makes an astonishing ${best.runs} off ${best.balls} balls!"
                best.runs >= 100 -> lines += "CENTURY! ${s.playerName} brings up a superb hundred (${best.runs} off ${best.balls})."
                best.runs >= 50 -> lines += "${s.playerName} compiles a fine half-century: ${best.runs} off ${best.balls}."
                best.runs == 0 && best.out -> lines += "A duck for ${s.playerName} — ${best.dismissal}."
                else -> lines += "${s.playerName} scores ${best.runs} off ${best.balls} (${best.dismissal})."
            }
            if (best.sixes >= 5) lines += "${best.sixes} sixes rain into the stands!"
        }
        if (wkts >= 5) lines += "FIVE-WICKET HAUL! ${s.playerName} rips through ${fx.opponent} with $wkts wickets!"
        else if (wkts >= 3) lines += "${s.playerName} strikes $wkts times with the ball."
        if (fx.level == Level.INTERNATIONAL) {
            val star = RealData.teams.firstOrNull { it.name == fx.opponent }?.starBatters?.random(rng)
            if (star != null && wkts > 0) lines += "The big wicket of $star among them!"
        }
        if (motm) lines += "${s.playerName} is named Player of the Match!"
        lines += if (won) "${if (fx.level == Level.INTERNATIONAL) s.country else "Your side"} win a ${if (rng.nextBoolean()) "thrilling" else "commanding"} contest."
        else "Despite the effort, ${fx.opponent} take the game."
        return lines
    }
}
