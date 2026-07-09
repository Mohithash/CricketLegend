package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Events
import com.mohithash.cricketlegend.engine.Finance
import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.engine.Progression
import com.mohithash.cricketlegend.engine.Scheduler
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.StatKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class EngineTest {

    private fun newState(role: Role = Role.ALL_ROUNDER): GameState {
        val s = GameState(
            playerName = "Test Player",
            country = "India",
            role = role,
            batting = 55.0,
            bowling = 50.0
        )
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Scheduler.buildSeason(s, Random(42))
        return s
    }

    /** Mirrors Game.playNextMatch without the Android controller. */
    private fun playSeason(s: GameState, rng: Random) {
        var guard = 0
        while (true) {
            if (guard++ > 300) error("season did not terminate")
            if (s.injuryWeeksLeft > 0) {
                val healWeek = s.week + s.injuryWeeksLeft
                s.fixtures.filter { !it.played && !it.missed && it.week < healWeek }.forEach { it.missed = true }
                Finance.processWeeks(s, s.injuryWeeksLeft)
                s.week = healWeek.coerceAtMost(52)
                s.injuryWeeksLeft = 0
            }
            val fx = s.nextFixture() ?: break
            Finance.processWeeks(s, (fx.week - s.week).coerceAtLeast(0))
            s.week = fx.week
            com.mohithash.cricketlegend.engine.Tournaments.startFor(s, fx, rng)
            val report = MatchEngine.simulate(s, fx, rng)
            Progression.applyReport(s, fx, report, rng)
            s.pendingEvent = Events.maybeEvent(s, rng)
            s.pendingEvent?.let { ev -> Events.resolve(s, ev, rng.nextInt(ev.choices.size)) }
        }
    }

    @Test
    fun seasonProducesFixtures() {
        val s = newState()
        assertTrue("expected a season schedule", s.fixtures.size >= 15)
        assertTrue(s.fixtures.all { it.week in 1..52 })
    }

    @Test
    fun fullCareerSimulationIsStable() {
        val rng = Random(7)
        val s = newState()
        repeat(20) {
            playSeason(s, rng)
            Finance.processWeeks(s, (52 - s.week).coerceAtLeast(0))
            Progression.endSeason(s, rng)
            // resolve auction like the UI would (decline retention half the time)
            if (s.pendingAuction != null) {
                com.mohithash.cricketlegend.engine.AuctionEngine.resolve(s, rng.nextBoolean(), rng)
                Scheduler.buildSeason(s, rng)
            }
        }
        assertEquals(38, s.age)
        assertTrue("should have played many matches", StatKey.ALL.sumOf { s.stat(it).matches } > 200)
        for (key in StatKey.ALL) {
            val st = s.stat(key)
            assertTrue(st.runs >= 0)
            assertTrue(st.innings >= st.notOuts)
            assertTrue(st.hundreds + st.fifties <= st.innings)
        }
        assertTrue("skills stay in range", s.batting in 1.0..99.0 && s.bowling in 1.0..99.0)
        assertTrue("form bounded", s.form in -5.0..5.0)
        assertTrue("fame bounded", s.fame in 0.0..100.0)
        // a 20-year career for a decent player should reach international cricket
        assertTrue("should earn national call-up", s.inNationalT20 || s.inNationalODI || s.inNationalTest)
    }

    @Test
    fun financesFlowBothWays() {
        val s = newState()
        val before = s.money
        Finance.processWeeks(s, 4)
        assertTrue("ledger recorded", s.ledger.isNotEmpty())
        // buy & sell property round-trips within transaction cost
        s.money = 2_000_000_000
        val prop = s.propertyMarket.first()
        assertTrue(Finance.buyProperty(s, prop.id))
        assertTrue(s.ownedProperties.any { it.id == prop.id })
        assertTrue(Finance.sellProperty(s, prop.id))
        assertTrue(s.ownedProperties.none { it.id == prop.id })
        assertTrue(before != s.money)
    }

    @Test
    fun stateSurvivesJsonRoundTrip() {
        val rng = Random(3)
        val s = newState()
        playSeason(s, rng)
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val restored = json.decodeFromString<GameState>(json.encodeToString(s))
        assertEquals(s.playerName, restored.playerName)
        assertEquals(s.money, restored.money)
        assertEquals(s.stat(StatKey.DOM_FC).runs, restored.stat(StatKey.DOM_FC).runs)
        assertEquals(s.fixtures.size, restored.fixtures.size)
    }

    @Test
    fun recordsAreCheckable() {
        val s = newState()
        for (rec in RealData.records) {
            val v = RealData.currentValue(rec.id, s)
            assertTrue("record ${rec.id} readable", v >= 0.0)
        }
    }
}
