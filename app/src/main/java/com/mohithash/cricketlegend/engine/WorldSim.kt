package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.DynRecord
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.RivalPlayer
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

/**
 * The world keeps playing without you. A cohort of rival stars accrues stats
 * every season, contests your awards, and can push record marks higher —
 * or snatch back a record you had claimed.
 */
object WorldSim {

    fun seedRivals(s: GameState, rng: Random) {
        if (s.rivals.isNotEmpty()) return
        // full current-player database — the living world you compete against and can browse
        s.rivals.addAll(com.mohithash.cricketlegend.data.PlayerDB.roster().map { it.copy() })
    }

    private fun newDebutant(s: GameState, rng: Random): RivalPlayer {
        val country = RealData.teams.random(rng).name
        return RivalPlayer(
            RealData.randomName(country), country,
            isBowler = rng.nextDouble() < 0.4,
            skill = 68.0 + rng.nextDouble() * 16,
            age = 19 + rng.nextInt(3)
        )
    }

    // ---- browsable world (Cricket-Coach style) ----

    /** All active players in a nation, best first — includes YOU if you play for it. */
    fun squad(s: GameState, country: String): List<RivalPlayer> {
        val list = s.rivals.filter { !it.retired && it.country == country }.toMutableList()
        if (s.country == country && !s.retired) {
            list.add(0, RivalPlayer(
                name = "★ ${s.playerName}", country = country, isBowler = s.role.name == "BOWLER",
                skill = maxOf(s.batting, s.bowling), age = s.age,
                testRuns = s.stat(StatKey.INTL_TEST).runs, odiRuns = s.stat(StatKey.INTL_ODI).runs,
                t20iRuns = s.stat(StatKey.INTL_T20).runs, testWkts = s.stat(StatKey.INTL_TEST).wickets,
                odiWkts = s.stat(StatKey.INTL_ODI).wickets, t20iWkts = s.stat(StatKey.INTL_T20).wickets,
                hundreds = s.intlHundreds, sixes = s.intlSixes, matches = s.intlMatches
            ))
        }
        return list.sortedByDescending { it.skill }
    }

    fun nationsByStrength(): List<String> =
        RealData.teams.sortedByDescending { it.strength }.map { it.name }

    /** Global leaderboard of international run-scorers (rivals + you). */
    fun topRunScorers(s: GameState, n: Int): List<Pair<String, Int>> {
        val all = s.rivals.filter { it.intlRuns > 0 }.map { it.name to it.intlRuns }.toMutableList()
        if (s.intlRuns > 0) all.add("★ ${s.playerName}" to s.intlRuns)
        return all.sortedByDescending { it.second }.take(n)
    }

    fun topWicketTakers(s: GameState, n: Int): List<Pair<String, Int>> {
        val all = s.rivals.filter { it.intlWkts > 0 }.map { it.name to it.intlWkts }.toMutableList()
        if (s.intlWickets > 0) all.add("★ ${s.playerName}" to s.intlWickets)
        return all.sortedByDescending { it.second }.take(n)
    }

    fun dynValue(s: GameState, id: String): Double =
        s.dynamicRecords[id]?.value ?: RealData.records.firstOrNull { it.id == id }?.value ?: 0.0

    fun dynHolder(s: GameState, id: String): String =
        s.dynamicRecords[id]?.holder ?: RealData.records.firstOrNull { it.id == id }?.holder ?: "?"

    /** One season of the rest of the cricketing world. Call from endSeason. */
    fun advanceSeason(s: GameState, rng: Random) {
        val newcomers = ArrayList<RivalPlayer>()
        for (r in s.rivals) {
            if (r.retired) continue
            r.age++
            // form curve
            val ageMult = when {
                r.age <= 27 -> 1.1
                r.age <= 32 -> 1.0
                r.age <= 35 -> 0.75
                else -> 0.45
            }
            val vol = 0.6 + rng.nextDouble() * 0.8
            if (!r.isBowler) {
                r.testRuns += (r.skill * ageMult * vol * 8).toInt()
                r.odiRuns += (r.skill * ageMult * vol * 7).toInt()
                r.t20iRuns += (r.skill * ageMult * vol * 4.5).toInt()
                r.leagueRuns += (r.skill * ageMult * vol * 5).toInt()
                r.hundreds += (rng.nextInt(6) * ageMult).toInt()
                r.odiHundreds += (rng.nextInt(3) * ageMult).toInt()
                r.sixes += (r.skill * ageMult * vol / 2).toInt()
            } else {
                r.testWkts += (r.skill * ageMult * vol * 0.55).toInt()
                r.odiWkts += (r.skill * ageMult * vol * 0.35).toInt()
                r.t20iWkts += (r.skill * ageMult * vol * 0.30).toInt()
            }
            r.matches += 24 + rng.nextInt(14)
            r.skill = (r.skill + if (r.age <= 29) 0.6 else -0.9).coerceIn(55.0, 97.0)

            if (r.age >= 36 && rng.nextDouble() < 0.3) {
                r.retired = true
                s.addNews("End of an era: ${r.name} (${r.country}) retires from international cricket.")
            }
        }
        if (rng.nextDouble() < 0.6) {
            val d = newDebutant(s, rng)
            newcomers.add(d)
            if (rng.nextDouble() < 0.3) s.addNews("Debut watch: ${d.name} (${d.country}) looks special.")
        }
        s.rivals.addAll(newcomers)
        while (s.rivals.size > 40) s.rivals.removeAt(0)

        challengeRecords(s, rng)
    }

    /** Rivals push cumulative record marks — including ones you hold. */
    private fun challengeRecords(s: GameState, rng: Random) {
        val checks: List<Pair<String, (RivalPlayer) -> Double>> = listOf(
            "intl_runs" to { r -> r.intlRuns.toDouble() },
            "test_runs" to { r -> r.testRuns.toDouble() },
            "odi_runs" to { r -> r.odiRuns.toDouble() },
            "t20i_runs" to { r -> r.t20iRuns.toDouble() },
            "intl_hundreds" to { r -> r.hundreds.toDouble() },
            "odi_hundreds" to { r -> r.odiHundreds.toDouble() },
            "intl_sixes" to { r -> r.sixes.toDouble() },
            "intl_matches" to { r -> r.matches.toDouble() },
            "test_wickets" to { r -> r.testWkts.toDouble() },
            "odi_wickets" to { r -> r.odiWkts.toDouble() },
            "t20i_wickets" to { r -> r.t20iWkts.toDouble() },
            "league_runs" to { r -> r.leagueRuns.toDouble() }
        )
        for ((id, metric) in checks) {
            val best = s.rivals.filter { !it.retired }.maxByOrNull(metric) ?: continue
            val v = metric(best)
            val current = dynValue(s, id)
            if (v > current) {
                val recTitle = RealData.records.firstOrNull { it.id == id }?.title ?: id
                val wasMine = id in s.brokenRecords
                s.dynamicRecords[id] = DynRecord(best.name, v)
                if (wasMine) {
                    s.brokenRecords.remove(id)
                    s.fame = (s.fame - 2).coerceAtLeast(0.0)
                    s.addNews("RECORD SNATCHED! ${best.name} takes '$recTitle' from you (${v.toInt()}). Win it back!")
                } else if (rng.nextDouble() < 0.6) {
                    s.addNews("World record: ${best.name} raises the bar for '$recTitle' (${v.toInt()}).")
                }
            }
        }
    }

    /** Returns the rival who beats the player to the ICC award, or null if the player wins. */
    fun awardChallenger(s: GameState, playerSeasonScore: Double, rng: Random): RivalPlayer? {
        val contender = s.rivals.filter { !it.retired && it.age in 22..35 }.maxByOrNull { it.skill } ?: return null
        val rivalScore = contender.skill * (0.75 + rng.nextDouble() * 0.45)
        return if (rivalScore > playerSeasonScore) contender else null
    }
}
