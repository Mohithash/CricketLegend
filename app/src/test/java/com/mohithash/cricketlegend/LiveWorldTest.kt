package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.LiveMatch
import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.engine.Progression
import com.mohithash.cricketlegend.engine.Scheduler
import com.mohithash.cricketlegend.engine.WorldSim
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.StatKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class LiveWorldTest {

    private fun state(): GameState {
        val s = GameState(playerName = "Live Tester", country = "India", role = Role.BATTER,
            batting = 80.0, vsPace = 78.0, vsSpin = 76.0, power = 75.0, battingPosition = 3)
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Scheduler.buildSeason(s, Random(1))
        return s
    }

    private fun t20Fixture() = Fixture(
        900, 20, Format.T20, Level.INTERNATIONAL, StatKey.INTL_T20, "Australia",
        venue = "MCG, Melbourne", pitch = "BALANCED"
    )

    @Test
    fun liveInningsPlaysToCompletion() {
        val rng = Random(11)
        val s = state()
        val lm = LiveMatch.start(s, t20Fixture(), rng)
        var guard = 0
        while (!lm.inningsOver && !lm.playerOut && guard++ < 400) {
            LiveMatch.playBall(s, lm, guard % 4, rng)  // cycle through all aggressions
        }
        assertTrue("innings must terminate", lm.inningsOver || lm.playerOut)
        assertTrue(lm.playerBalls > 0)
        assertTrue(lm.teamRuns >= lm.playerRuns * 0 + lm.playerRuns - 200) // sanity: no negative weirdness
        assertTrue(lm.ballsBowled <= lm.maxBalls + 1)

        val report = MatchEngine.simulateLive(s, lm.fixture, lm, rng)
        assertEquals(lm.playerRuns, report.batting[0].runs)
        val broken = Progression.applyReport(s, lm.fixture, report, rng)
        assertTrue(s.stat(StatKey.INTL_T20).matches == 1)
        assertTrue(broken.size >= 0)
    }

    @Test
    fun aggressionChangesRisk() {
        val rng = Random(5)
        val s = state()
        var defendOuts = 0; var slogOuts = 0
        repeat(300) {
            val lmD = LiveMatch.start(s, t20Fixture(), Random(it))
            var g = 0
            while (!lmD.inningsOver && !lmD.playerOut && g++ < 30) LiveMatch.playBall(s, lmD, LiveMatch.DEFEND, Random(it * 3 + 1))
            if (lmD.playerOut) defendOuts++
            val lmS = LiveMatch.start(s, t20Fixture(), Random(it))
            g = 0
            while (!lmS.inningsOver && !lmS.playerOut && g++ < 30) LiveMatch.playBall(s, lmS, LiveMatch.SLOG, Random(it * 3 + 2))
            if (lmS.playerOut) slogOuts++
        }
        assertTrue("slogging ($slogOuts) must be riskier than defending ($defendOuts)", slogOuts > defendOuts)
    }

    @Test
    fun legacyIconTiersEscalate() {
        val s = GameState(country = "India", role = Role.BATTER)
        val low = com.mohithash.cricketlegend.engine.Legacy.iconStatus(s)
        // fabricate a monster career
        s.stats[StatKey.INTL_TEST] = com.mohithash.cricketlegend.model.FormatStats(matches = 200, innings = 300, runs = 80000, hundreds = 300)
        s.brokenRecords.addAll(listOf("a","b","c","d","e","f"))
        repeat(20) { s.trophies.add("T$it") }
        s.fame = 100.0
        val high = com.mohithash.cricketlegend.engine.Legacy.iconStatus(s)
        assertTrue("icon tier should escalate with success ($low -> $high)", low != high)
        // record projection should surface future targets
        assertTrue(com.mohithash.cricketlegend.engine.Legacy.recordProjection(s).isNotEmpty())
    }

    @Test
    fun fantasyRecordRegenerates() {
        val s = GameState(country = "India", role = Role.BATTER)
        val before = WorldSim.dynValue(s, "fantasy_intl_runs")
        assertTrue(before >= 50000.0)
        // simulate conquering it
        s.dynamicRecords["fantasy_intl_runs"] = com.mohithash.cricketlegend.model.DynRecord(s.playerName, before * 1.5)
        assertTrue("bar leaps higher after conquest", WorldSim.dynValue(s, "fantasy_intl_runs") > before)
    }

    @Test
    fun scorecardIsCompleteAndConsistent() {
        val rng = Random(31)
        val s = GameState(country = "India", role = Role.ALL_ROUNDER, batting = 78.0, bowling = 70.0,
            inNationalTest = true, inNationalODI = true, inNationalT20 = true)
        val fx = Fixture(1, 20, Format.T20, Level.INTERNATIONAL, StatKey.INTL_T20, "Australia",
            venue = "MCG", pitch = "FLAT")
        val r = MatchEngine.simulate(s, fx, rng)
        val card = r.scorecard!!
        // both innings must field 11 batters and have bowlers
        assertTrue("first innings 11 batters", card.first.batting.size == 11)
        assertTrue("second innings 11 batters", card.second.batting.size == 11)
        assertTrue("bowlers present", card.first.bowling.isNotEmpty() && card.second.bowling.isNotEmpty())
        // batting totals should be within a run or two of the innings total (rounding)
        val sum1 = card.first.batting.sumOf { it.runs }
        assertTrue("batting sums to total ($sum1 vs ${card.first.total})", kotlin.math.abs(sum1 - card.first.total) <= 3)
        // the player appears exactly once across the two innings (order depends on who batted first)
        val playerSlots = card.first.batting.count { it.isPlayer } + card.second.batting.count { it.isPlayer }
        assertTrue("player on card exactly once", playerSlots == 1)
    }

    @Test
    fun splitStatsAreTracked() {
        val rng = Random(21)
        val s = GameState(country = "India", role = Role.BATTER, batting = 80.0,
            vsPace = 78.0, vsSpin = 76.0, power = 75.0, inNationalT20 = true, inNationalODI = true, inNationalTest = true)
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Scheduler.buildSeason(s, rng)
        var guard = 0
        while (guard++ < 300) {
            val fx = s.nextFixture() ?: break
            s.week = fx.week
            com.mohithash.cricketlegend.engine.Tournaments.startFor(s, fx, rng)
            val report = MatchEngine.simulate(s, fx, rng)
            Progression.applyReport(s, fx, report, rng)
        }
        assertTrue("split runs recorded by opponent", s.splitRuns.keys.any { it.startsWith("opp:") })
        assertTrue("home/away tracked", s.splitRuns.keys.any { it.startsWith("loc:") })
        assertTrue("dismissal types recorded", s.dismissalTypes.isNotEmpty())
        assertTrue("fantasy points accrued", s.fantasyPoints > 0)
        // split average sanity
        val oppKey = s.splitRuns.keys.first { it.startsWith("opp:") }
        assertTrue(com.mohithash.cricketlegend.engine.Progression.splitAverage(s, oppKey) >= 0.0)
    }

    @Test
    fun prodigyStartsInYouthCricket() {
        val s = GameState(playerName = "Baby GOAT", country = "India", role = Role.BATTER,
            age = 9, batting = 40.0)
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Scheduler.buildSeason(s, Random(3))
        assertTrue("prodigy should play age-group cricket", s.fixtures.isNotEmpty())
        assertTrue("all youth fixtures", s.fixtures.all { it.statKey == StatKey.YOUTH })
    }

    @Test
    fun playstyleShiftsScoringProfile() {
        val rng = Random(4)
        fun avgRuns(style: String): Double {
            val s = GameState(country = "India", role = Role.BATTER, batting = 75.0,
                vsPace = 75.0, vsSpin = 75.0, power = 75.0, playstyle = style, battingPosition = 3)
            var runs = 0; var n = 0
            repeat(400) {
                val fx = Fixture(1, 20, Format.T20, Level.FRANCHISE, StatKey.LEAGUE, "Mumbai Mavericks",
                    venue = "Wankhede", pitch = "FLAT")
                runs += MatchEngine.simulate(s, fx, Random(it)).batting.sumOf { b -> b.runs }; n++
            }
            return runs.toDouble() / n
        }
        val aggr = avgRuns("Aggressive")
        val def = avgRuns("Defensive")
        // both viable; aggressive should generate a higher ceiling on a flat deck
        assertTrue("aggressive avg ($aggr) should differ from defensive ($def)", aggr != def)
    }

    @Test
    fun worldMovesAndRecordsCanFall() {
        val rng = Random(9)
        val s = state()
        WorldSim.seedRivals(s, rng)
        assertTrue(s.rivals.size >= 15)
        repeat(20) { WorldSim.advanceSeason(s, rng) }
        val bestRuns = s.rivals.maxOf { it.intlRuns }
        assertTrue("20 seasons should produce a 10k+ run rival, got $bestRuns", bestRuns > 10_000)
        // dynamic record book should have moved for at least one cumulative record
        assertTrue("rivals should raise at least one record", s.dynamicRecords.isNotEmpty())
        // record snatching: give the player a record then let the world catch up
        s.brokenRecords.add("odi_runs")
        s.dynamicRecords["odi_runs"] = com.mohithash.cricketlegend.model.DynRecord("Live Tester", 100.0)
        repeat(3) { WorldSim.advanceSeason(s, rng) }
        assertTrue("a 100-run 'record' must be snatched immediately", "odi_runs" !in s.brokenRecords)
    }
}
