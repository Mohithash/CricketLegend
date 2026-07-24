package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.model.CardBat
import com.mohithash.cricketlegend.model.CardBowl
import com.mohithash.cricketlegend.model.FullScorecard
import com.mohithash.cricketlegend.model.InningsCard
import com.mohithash.cricketlegend.model.SquadPlayer
import kotlin.random.Random

/**
 * The unified match engine: plays a REAL T20 between two XIs of rated players.
 * Every run and wicket is attributed to a named player, and skills are decisive —
 * this is what the whole living world (franchise league, tournaments) runs on.
 */
object TeamMatchSim {

    data class Result(
        val card: FullScorecard,
        val teamAWon: Boolean,
        val motm: String
    )

    /** Best XI: top-rated, keeping at least 4 frontline bowlers and a keeper if possible. */
    fun pickXI(squad: List<SquadPlayer>): List<SquadPlayer> {
        val sorted = squad.sortedByDescending { it.rating }
        val xi = sorted.take(11).toMutableList()
        val bowlers = xi.count { it.role == "BOWL" || it.role == "AR" }
        if (bowlers < 4) {
            val extra = sorted.drop(11).filter { it.role == "BOWL" || it.role == "AR" }
                .take(4 - bowlers)
            extra.forEach { e ->
                xi.remove(xi.filter { it.role == "BAT" }.minByOrNull { it.rating } ?: return@forEach)
                xi.add(e)
            }
        }
        return xi
    }

    /** Batting order: openers/top from BAT+WK by rating, AR middle, bowlers last. */
    private fun battingOrder(xi: List<SquadPlayer>): List<SquadPlayer> =
        xi.sortedWith(compareBy({ roleOrder(it.role) }, { -it.rating }))

    private fun roleOrder(r: String) = when (r) { "BAT" -> 0; "WK" -> 0; "AR" -> 1; else -> 2 }

    private fun bowlingAttack(xi: List<SquadPlayer>): List<SquadPlayer> {
        val front = xi.filter { it.role == "BOWL" || it.role == "AR" }.sortedByDescending { it.rating }
        return if (front.size >= 5) front.take(6) else (front + xi.sortedByDescending { it.rating }.take(6 - front.size))
    }

    /** Simulates one T20 innings; returns the card + accrues season/career stats. */
    private fun simInnings(batting: List<SquadPlayer>, bowlingXI: List<SquadPlayer>,
                           teamName: String, target: Int?, rng: Random): InningsCard {
        val order = battingOrder(batting)
        val attack = bowlingAttack(bowlingXI)
        val attackAvg = attack.map { it.rating }.average()
        val bats = ArrayList<CardBat>()
        val bowlWkts = IntArray(attack.size)
        val bowlRuns = IntArray(attack.size)

        var total = 0; var wkts = 0; var balls = 0
        var idx = 0
        while (balls < 120 && wkts < 10 && (target == null || total <= target)) {
            val p = order[idx.coerceAtMost(10)]
            // batter's spell: skill+form vs the attack decides output and survival
            val eff = (p.rating * (0.75 + p.form / 200.0) - (attackAvg - 70) * 0.5).coerceIn(15.0, 105.0)
            val ballsLeft = (120 - balls).coerceAtLeast(1)
            val faced = (6 + rng.nextInt(26) + ((eff - 50) / 3).toInt()).coerceIn(1, ballsLeft)
            val sr = (0.7 + eff / 90.0 + rng.nextDouble() * 0.5)
            var runs = (faced * sr).toInt()
            val out = rng.nextDouble() < (0.85 - eff / 300.0) || wkts >= 9
            if (target != null && total + runs > target + 6) runs = (target + 1 - total).coerceAtLeast(0) + rng.nextInt(6)
            total += runs; balls += faced
            val bIdx = rng.nextInt(attack.size)
            if (out) { wkts++; bowlWkts[bIdx]++ }
            bowlRuns[bIdx] += runs
            bats.add(CardBat(p.name, runs, faced, runs / 8, runs / 18, out,
                if (out) "b ${attack[bIdx].name.split(" ").last()}" else "not out",
                isPlayer = p.isManager))
            // accrue live stats — the heart of the unified world
            p.seasonRuns += runs; p.careerRuns += runs
            idx++
            if (idx > 10) break
            if (target != null && total > target) break
        }
        val bowls = attack.mapIndexed { i, b ->
            val overs = if (i < 4) 4 else 2
            b.seasonWkts += bowlWkts[i]; b.careerWkts += bowlWkts[i]
            CardBowl(b.name, overs * 6, 0, bowlRuns[i].coerceAtLeast(overs * 4), bowlWkts[i], isPlayer = b.isManager)
        }
        batting.forEach { it.careerMatches++ }
        return InningsCard(teamName, total, wkts, "20 ov", bats, bowls)
    }

    /** Full match: A bats, B chases. Skills of all 22 named players decide it. */
    fun play(teamA: String, squadA: List<SquadPlayer>, teamB: String, squadB: List<SquadPlayer>, rng: Random): Result {
        val xiA = pickXI(squadA); val xiB = pickXI(squadB)
        val first = simInnings(xiA, xiB, teamA, null, rng)
        val second = simInnings(xiB, xiA, teamB, first.total, rng)
        val aWon = first.total >= second.total
        val bestBat = (first.batting + second.batting).maxByOrNull { it.runs }
        val bestBowl = (first.bowling + second.bowling).maxByOrNull { it.wickets }
        val motm = if ((bestBowl?.wickets ?: 0) >= 4) bestBowl!!.name else bestBat?.name ?: ""
        return Result(FullScorecard(teamA, teamB, first, second), aWon, motm)
    }
}
