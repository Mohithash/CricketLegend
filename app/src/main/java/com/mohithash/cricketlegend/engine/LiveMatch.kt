package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.BattingLine
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import kotlin.random.Random

data class LiveBowler(val name: String, val isSpin: Boolean, val skill: Double)

/** Transient state of a ball-by-ball innings. Not persisted — abandoning mid-innings replays the match. */
class LiveMatchState(
    val fixture: Fixture,
    val chasing: Boolean,
    val target: Int,                  // 0 when batting first
    val maxBalls: Int,
    val bowlers: List<LiveBowler>,
    var teamRuns: Int = 0,
    var teamWkts: Int = 0,
    var ballsBowled: Int = 0,         // team innings balls
    var playerRuns: Int = 0,
    var playerBalls: Int = 0,
    var playerFours: Int = 0,
    var playerSixes: Int = 0,
    var ballsAtHundred: Int = 0,
    var playerOut: Boolean = false,
    var playerOnStrike: Boolean = true,
    var dismissal: String = "not out",
    var inningsOver: Boolean = false,
    val log: ArrayList<String> = ArrayList()
) {
    val currentBowler: LiveBowler get() = bowlers[(ballsBowled / 6) % bowlers.size]
    val oversText: String get() = "${ballsBowled / 6}.${ballsBowled % 6}"
    val runsNeeded: Int get() = (target - teamRuns).coerceAtLeast(0)
    val ballsLeft: Int get() = maxBalls - ballsBowled
    val chaseWon: Boolean get() = chasing && teamRuns > target - 1
}

/**
 * Ball-by-ball interactive innings for T20/ODI. The player chooses aggression
 * per delivery; partners are simulated between your balls.
 */
object LiveMatch {

    const val DEFEND = 0
    const val NORMAL = 1
    const val ATTACK = 2
    const val SLOG = 3

    fun available(fx: Fixture): Boolean = fx.format != Format.FIRST_CLASS

    fun start(s: GameState, fx: Fixture, rng: Random = Random.Default): LiveMatchState {
        val maxBalls = if (fx.format == Format.T20) 120 else 300
        val chasing = rng.nextBoolean()
        val target = if (chasing) {
            val oppBase = if (fx.format == Format.T20) 130 + rng.nextInt(75) else 210 + rng.nextInt(120)
            oppBase
        } else 0

        val lm = LiveMatchState(fx, chasing, target, maxBalls, generateBowlers(s, fx, rng))
        lm.log.add(
            if (chasing) "Chasing ${target}. You bat at #${s.battingPosition}."
            else "Batting first. You come in at #${s.battingPosition}."
        )
        // fast-forward to the player's arrival at the crease
        val pos = s.battingPosition.coerceIn(1, 8)
        if (pos > 2) {
            val arriveWkts = pos - 2
            while (lm.teamWkts < arriveWkts && lm.ballsBowled < lm.maxBalls / 2 && !lm.chaseWon) {
                partnerBall(s, lm, rng, silent = true)
            }
            lm.log.add("Wicket #${lm.teamWkts} falls at ${lm.teamRuns}/${lm.teamWkts} (${lm.oversText} ov). You're in.")
        }
        lm.playerOnStrike = true
        return lm
    }

    private fun generateBowlers(s: GameState, fx: Fixture, rng: Random): List<LiveBowler> {
        val nat = RealData.teams.firstOrNull { it.name == fx.opponent }
        val names = (nat?.starBowlers ?: List(4) { RealData.randomName(s.country) }).take(4)
        return names.mapIndexed { i, n ->
            val spin = n.lowercase().let { it.contains("kuldeep") || it.contains("rashid") || it.contains("lyon") ||
                it.contains("maharaj") || it.contains("santner") || it.contains("hasaranga") ||
                it.contains("theekshana") || it.contains("abrar") || it.contains("mujeeb") ||
                it.contains("hosein") || it.contains("mehidy") || it.contains("rishad") } || rng.nextDouble() < 0.3
            val base = (nat?.strength ?: 70).toDouble()
            LiveBowler(n, spin, (base - 8 + rng.nextDouble() * 20).coerceIn(55.0, 96.0))
        }
    }

    /** Plays one delivery faced by the player, then simulates until they're back on strike. */
    fun playBall(s: GameState, lm: LiveMatchState, aggression: Int, rng: Random = Random.Default) {
        if (lm.inningsOver || lm.playerOut) return

        val bowler = lm.currentBowler
        val matchup = if (bowler.isSpin) s.vsSpin else s.vsPace
        val base = 0.5 * s.batting + 0.5 * matchup
        val bowlerEdge = (bowler.skill - 72) / 3.0
        val phase = phaseOf(lm)
        val eff = (base - bowlerEdge + s.form * 1.5 + phase.effBonus).coerceIn(10.0, 100.0)

        val pOutBase = if (lm.fixture.format == Format.T20) 0.045 else 0.032
        val aggrOut = doubleArrayOf(0.40, 1.0, 1.7, 2.9)[aggression]
        val pOut = (pOutBase * aggrOut * (1.8 - eff / 80.0)).coerceIn(0.004, 0.30)

        lm.playerBalls++
        lm.ballsBowled++

        if (rng.nextDouble() < pOut) {
            lm.playerOut = true
            lm.dismissal = listOf("b ${bowler.name}", "c & b ${bowler.name}", "lbw b ${bowler.name}",
                "c (deep) b ${bowler.name}").random(rng)
            lm.teamWkts++
            lm.log.add("OUT! ${lm.dismissal} — ${lm.playerRuns}(${lm.playerBalls})")
            fastForward(s, lm, rng)
            return
        }

        val runs = scoreBall(eff, s.power, aggression, lm.fixture.format, rng)
        lm.playerRuns += runs
        lm.teamRuns += runs
        when (runs) {
            4 -> { lm.playerFours++; lm.log.add("FOUR! ${shotText(aggression, rng)} off ${bowler.name}.") }
            6 -> { lm.playerSixes++; lm.log.add("SIX! Launched over the ropes off ${bowler.name}!") }
            0 -> if (rng.nextDouble() < 0.25) lm.log.add("Dot ball — ${bowler.name} beats the bat.")
            else -> {}
        }
        if (lm.playerRuns >= 100 && lm.ballsAtHundred == 0) {
            lm.ballsAtHundred = lm.playerBalls
            lm.log.add("CENTURY! ${s.playerName} raises the bat — ${lm.playerRuns} off ${lm.playerBalls}!")
        } else if (lm.playerRuns in 50..53 && runs > 0 && lm.playerRuns - runs < 50) {
            lm.log.add("Fifty up for ${s.playerName}!")
        }

        if (runs % 2 == 1) lm.playerOnStrike = false
        if (lm.ballsBowled % 6 == 0) lm.playerOnStrike = !lm.playerOnStrike
        checkInningsEnd(lm)

        // partner keeps strike until it rotates back
        while (!lm.inningsOver && !lm.playerOnStrike) {
            partnerBall(s, lm, rng, silent = false)
        }
    }

    private fun phaseOf(lm: LiveMatchState): Phase {
        if (lm.fixture.format != Format.T20) return Phase(0.0)
        val over = lm.ballsBowled / 6
        return when {
            over < 6 -> Phase(3.0)      // powerplay: field up, easier boundaries
            over >= 16 -> Phase(-2.0)   // death: yorkers flying
            else -> Phase(0.0)
        }
    }

    data class Phase(val effBonus: Double)

    private fun scoreBall(eff: Double, power: Double, aggression: Int, format: Format, rng: Random): Int {
        val boundaryW = (0.4 + power / 200.0 + eff / 350.0) * doubleArrayOf(0.25, 1.0, 1.8, 2.6)[aggression]
        val t20 = format == Format.T20
        val w = doubleArrayOf(
            /*dot*/ doubleArrayOf(3.2, 1.6, 1.0, 0.8)[aggression] * (if (t20) 1.0 else 1.6),
            /*1*/ doubleArrayOf(1.4, 1.9, 1.1, 0.6)[aggression],
            /*2*/ 0.4,
            /*3*/ 0.05,
            /*4*/ (if (t20) 0.75 else 0.45) * boundaryW,
            /*6*/ (if (t20) 0.40 else 0.18) * boundaryW
        )
        var r = rng.nextDouble() * w.sum()
        for (i in w.indices) { r -= w[i]; if (r <= 0) return intArrayOf(0, 1, 2, 3, 4, 6)[i] }
        return 0
    }

    private fun shotText(aggression: Int, rng: Random) = when (aggression) {
        DEFEND -> "Deft touch past the keeper"
        NORMAL -> listOf("Classical cover drive", "Crisp punch down the ground", "Elegant flick").random(rng)
        ATTACK -> listOf("Slashed hard through point", "Lofted over mid-off", "Pulled with authority").random(rng)
        else -> listOf("Baseball swing", "Slog-swept flat", "Scooped outrageously").random(rng)
    }

    private fun partnerBall(s: GameState, lm: LiveMatchState, rng: Random, silent: Boolean) {
        if (lm.inningsOver) return
        lm.ballsBowled++
        val t20 = lm.fixture.format == Format.T20
        if (rng.nextDouble() < (if (t20) 0.045 else 0.032)) {
            lm.teamWkts++
            if (!silent) lm.log.add("Wicket at the other end! ${lm.teamRuns}/${lm.teamWkts}.")
            if (lm.teamWkts >= 9) { // you run out of partners
                lm.inningsOver = true
                if (!silent) lm.log.add("Innings over — no partners left.")
                return
            }
        } else {
            val runs = intArrayOf(0, 0, 1, 1, 1, 2, 4, 6)[rng.nextInt(8)]
            lm.teamRuns += runs
            if (runs % 2 == 1) lm.playerOnStrike = true
        }
        if (lm.ballsBowled % 6 == 0) lm.playerOnStrike = !lm.playerOnStrike
        checkInningsEnd(lm)
    }

    private fun checkInningsEnd(lm: LiveMatchState) {
        if (lm.ballsBowled >= lm.maxBalls || lm.teamWkts >= 10 || lm.chaseWon) {
            lm.inningsOver = true
            if (lm.chaseWon) lm.log.add("WINNING RUNS! Chase completed with ${lm.ballsLeft} ball(s) to spare!")
            else if (lm.chasing) lm.log.add("Innings closed at ${lm.teamRuns}/${lm.teamWkts} — short of the target.")
        }
    }

    /** After the player's dismissal, resolve the rest of the team innings instantly. */
    private fun fastForward(s: GameState, lm: LiveMatchState, rng: Random) {
        while (!lm.inningsOver) partnerBall(s, lm, rng, silent = true)
        lm.log.add("Innings ends: ${lm.teamRuns}/${lm.teamWkts} (${lm.oversText} ov).")
    }

    fun toBattingLine(lm: LiveMatchState): BattingLine = BattingLine(
        lm.playerRuns, lm.playerBalls, lm.playerFours, lm.playerSixes,
        lm.playerOut, lm.dismissal, lm.ballsAtHundred
    )
}
