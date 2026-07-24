package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.PlayerDB
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.BattingLine
import com.mohithash.cricketlegend.model.BowlingLine
import com.mohithash.cricketlegend.model.CardBat
import com.mohithash.cricketlegend.model.CardBowl
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.FullScorecard
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.InningsCard
import com.mohithash.cricketlegend.model.Level
import kotlin.random.Random

/**
 * Builds full 11-a-side scorecards for both teams, Cricket-Coach style, consistent
 * with the player's actual line and the match result. Names come from the real
 * squad databases where possible.
 */
object Scorecards {

    fun build(
        s: GameState, fx: Fixture,
        playerBat: BattingLine?, playerBowl: BowlingLine?,
        teamTotal: Int, teamWkts: Int, oppTotal: Int, rng: Random,
        playerBattedFirst: Boolean = true
    ): FullScorecard {
        val playerTeamName = teamName(s, fx)
        val teammates = squadNames(s, fx, own = true, rng)
        val opponents = squadNames(s, fx, own = false, rng)

        // your team innings (contains YOU at your chosen batting position) + opposition bowling
        val teamBat = buildBatting(teamTotal, teamWkts, teammates, playerBat, s.playerName, fx, rng, s.battingPosition)
        val oppBowl = buildBowling(teamTotal, teamWkts, opponents, null, "", fx, rng)
        val teamInns = InningsCard(playerTeamName, teamTotal, teamBat.count { it.out },
            oversText(fx, teamTotal), teamBat, oppBowl)

        // opposition innings + your team bowling (contains YOU if you bowl)
        val oppWkts = (6 + rng.nextInt(5)).coerceAtMost(10)
        val oppBat = buildBatting(oppTotal, oppWkts, opponents, null, "", fx, rng, 3)
        val teamBowl = buildBowling(oppTotal, oppWkts, teammates, playerBowl, s.playerName, fx, rng)
        val oppInns = InningsCard(fx.opponent, oppTotal, oppWkts,
            oversText(fx, oppTotal), oppBat, teamBowl)

        // order the innings by who actually batted first
        return if (playerBattedFirst)
            FullScorecard(playerTeamName, fx.opponent, teamInns, oppInns)
        else
            FullScorecard(playerTeamName, fx.opponent, oppInns, teamInns)
    }

    private fun teamName(s: GameState, fx: Fixture): String = when (fx.level) {
        Level.INTERNATIONAL -> s.country
        Level.FRANCHISE -> s.franchiseTeam ?: "Your XI"
        else -> "Your State"
    }

    /** 11 names for a side; the player is inserted separately by the caller. */
    private fun squadNames(s: GameState, fx: Fixture, own: Boolean, rng: Random): List<String> {
        val country = if (own) s.country else fx.opponent
        return when (fx.level) {
            Level.INTERNATIONAL -> {
                val db = PlayerDB.roster().filter { it.country == country }.map { it.name }
                (db.shuffled(rng) + List(11) { RealData.randomName(country) }).distinct().take(11)
            }
            else -> List(11) { RealData.randomName(s.country) }
        }
    }

    private fun oversText(fx: Fixture, total: Int): String = when (fx.format) {
        Format.T20 -> "20 ov"
        Format.ODI -> "50 ov"
        Format.FIRST_CLASS -> "1st inns"
    }

    private fun buildBatting(
        total: Int, wkts: Int, names: List<String>,
        playerLine: BattingLine?, playerName: String, fx: Fixture, rng: Random, playerPos: Int
    ): List<CardBat> {
        val out = ArrayList<CardBat>()
        var remaining = total
        val playerRuns = playerLine?.runs ?: 0
        if (playerLine != null) remaining -= playerRuns

        // distribute remaining runs across the other batters (11 total incl. player if present)
        val others = if (playerLine != null) 10 else 11
        val weights = DoubleArray(others) { 0.3 + rng.nextDouble() }
        val wsum = weights.sum()
        val runsEach = IntArray(others) { (remaining * weights[it] / wsum).toInt() }
        // fix rounding
        var diff = remaining - runsEach.sum()
        var idx = 0
        while (diff != 0 && others > 0) {
            runsEach[idx % others] += if (diff > 0) 1 else -1
            diff += if (diff > 0) -1 else 1; idx++
            if (idx > 5000) break
        }

        var wktsLeft = wkts
        val pool = names.toMutableList()
        // insert the player's card at THEIR chosen batting position (1-based -> 0-based)
        val insertAt = if (playerLine != null) (playerPos - 1).coerceIn(0, 10) else -1

        for (i in 0 until 11) {
            if (i == insertAt && playerLine != null) {
                out.add(CardBat("★ $playerName", playerRuns, playerLine.balls, playerLine.fours,
                    playerLine.sixes, playerLine.out, playerLine.dismissal, isPlayer = true))
                continue
            }
            val oi = out.count { !it.isPlayer }
            if (oi >= others) break
            val runs = runsEach[oi].coerceAtLeast(0)
            val balls = ballsFor(runs, fx, rng)
            val outNow = wktsLeft > 0 && (i < 8 || runs == 0 || rng.nextDouble() < 0.7)
            if (outNow) wktsLeft--
            val name = pool.getOrElse(oi) { RealData.randomName("India") }
            out.add(CardBat(name, runs, balls, runs / 8 + rng.nextInt(2),
                if (fx.format == Format.T20) runs / 20 else runs / 40,
                outNow, if (outNow) dismissal(rng) else "not out"))
        }
        return out.take(11)
    }

    private fun ballsFor(runs: Int, fx: Fixture, rng: Random): Int {
        val sr = when (fx.format) {
            Format.T20 -> 1.1 + rng.nextDouble() * 0.7
            Format.ODI -> 0.75 + rng.nextDouble() * 0.5
            Format.FIRST_CLASS -> 0.45 + rng.nextDouble() * 0.35
        }
        return (runs / sr).toInt().coerceAtLeast(if (runs > 0) 1 else 0)
    }

    private fun buildBowling(
        total: Int, wkts: Int, names: List<String>,
        playerBowl: BowlingLine?, playerName: String, fx: Fixture, rng: Random
    ): List<CardBowl> {
        val bowlerCount = 6
        val totalBalls = when (fx.format) { Format.T20 -> 120; Format.ODI -> 300; Format.FIRST_CLASS -> 480 }
        val out = ArrayList<CardBowl>()
        // the player's own spell can exceed the notional team totals — clamp so the
        // remaining shares never go negative (coerceIn(0, negative) would throw)
        var runsLeft = total
        var wktsLeft = wkts
        var ballsLeft = totalBalls

        if (playerBowl != null && playerBowl.balls > 0) {
            out.add(CardBowl("★ $playerName", playerBowl.balls, rng.nextInt(2),
                playerBowl.runsConceded, playerBowl.wickets, isPlayer = true))
            runsLeft -= playerBowl.runsConceded; wktsLeft -= playerBowl.wickets; ballsLeft -= playerBowl.balls
        }
        runsLeft = runsLeft.coerceAtLeast(0)
        wktsLeft = wktsLeft.coerceAtLeast(0)
        ballsLeft = ballsLeft.coerceAtLeast(0)

        val n = (bowlerCount - out.size).coerceAtLeast(1)
        for (i in 0 until n) {
            val remBowlers = (n - i)
            val share = if (i == n - 1) ballsLeft else (ballsLeft / remBowlers)
            val balls = share.coerceIn(0, ballsLeft)
            ballsLeft = (ballsLeft - balls).coerceAtLeast(0)
            val w = if (i == n - 1) wktsLeft else (0..wktsLeft).random(rng).coerceAtMost(3)
            wktsLeft = (wktsLeft - w).coerceAtLeast(0)
            val runs = if (i == n - 1) runsLeft
            else (runsLeft.toDouble() * (0.5 + rng.nextDouble() * 0.5) / remBowlers).toInt().coerceIn(0, runsLeft)
            runsLeft = (runsLeft - runs).coerceAtLeast(0)
            val name = names.getOrElse(names.size - 1 - i) { RealData.randomName("India") }
            if (balls > 0) out.add(CardBowl(name, balls, rng.nextInt(2), runs, w))
        }
        return out
    }

    private fun dismissal(rng: Random): String =
        listOf("b ", "c ", "lbw b ", "c & b ", "run out", "st ").random(rng).let {
            if (it == "run out") it else it + RealData.randomName("India").split(" ").last()
        }
}
