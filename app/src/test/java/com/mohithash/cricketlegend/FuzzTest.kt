package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Allocations
import com.mohithash.cricketlegend.engine.AuctionEngine
import com.mohithash.cricketlegend.engine.Events
import com.mohithash.cricketlegend.engine.Finance
import com.mohithash.cricketlegend.engine.Legacy
import com.mohithash.cricketlegend.engine.LifeSystems
import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.engine.Progression
import com.mohithash.cricketlegend.engine.Scheduler
import com.mohithash.cricketlegend.engine.Tournaments
import com.mohithash.cricketlegend.engine.WorldSim
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.StatKey
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

/** Drives every system with randomized input to surface crashes and invariant breaks. */
class FuzzTest {

    private fun freshState(rng: Random): GameState {
        val role = Role.entries.random(rng)
        val country = RealData.teams.random(rng).name
        val diff = listOf("Easy", "Realistic", "Hardcore").random(rng)
        val focus = listOf("All", "WhiteBall", "T20Only").random(rng)
        val style = listOf("Aggressive", "Balanced", "Defensive").random(rng)
        val age = listOf(9, 14, 18).random(rng)
        val s = GameState(
            playerName = "Fuzz ${rng.nextInt(999)}", country = country, role = role,
            age = age, batting = 40.0 + rng.nextDouble() * 40, bowling = 20.0 + rng.nextDouble() * 50,
            vsPace = 50.0, vsSpin = 50.0, power = 50.0, control = 50.0,
            playstyle = style, formatFocus = focus, difficulty = diff
        )
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Finance.initMarket(s)
        Allocations.seedDefaults(s)
        WorldSim.seedRivals(s, rng)
        Scheduler.buildSeason(s, rng)
        return s
    }

    private fun checkInvariants(s: GameState, ctx: String) {
        fun bad(v: Double) = v.isNaN() || v.isInfinite()
        listOf(s.batting, s.bowling, s.fitness, s.morale, s.fame, s.form, s.vsPace,
            s.vsSpin, s.power, s.control, s.sharpness, s.publicImage, s.relationship).forEach {
            if (bad(it)) fail("$ctx: NaN/Inf attribute")
        }
        assertTrue("$ctx: batting range", s.batting in 0.0..100.0)
        assertTrue("$ctx: fame range", s.fame in 0.0..100.0)
        assertTrue("$ctx: sharpness range", s.sharpness in 0.0..100.0)
        assertTrue("$ctx: morale range", s.morale in 0.0..100.0)
        assertTrue("$ctx: money finite", s.money > Long.MIN_VALUE / 2 && s.money < Long.MAX_VALUE / 2)
        assertTrue("$ctx: followers non-neg", s.followers >= 0)
        for (k in StatKey.ALL) {
            val st = s.stat(k)
            assertTrue("$ctx: innings>=notOuts", st.innings >= st.notOuts)
            assertTrue("$ctx: runs non-neg", st.runs >= 0)
        }
    }

    private fun doRandomActions(s: GameState, rng: Random) {
        when (rng.nextInt(12)) {
            0 -> s.propertyMarket.randomOrNull(rng)?.let { Finance.buyProperty(s, it.id) }
            1 -> RealData.lifestyleItems.randomOrNull(rng)?.let { Finance.buyItem(s, it.id) }
            2 -> s.marketInstruments.randomOrNull(rng)?.let { Finance.buyInstrument(s, it.id, 1_000_000) }
            3 -> s.holdings.randomOrNull(rng)?.let { Finance.sellInstrument(s, it.id) }
            4 -> Allocations.categories.randomOrNull(rng)?.let { Allocations.set(s, it.id, rng.nextLong(it.maxWeekly + 1)) }
            5 -> RealData.businessDefs.randomOrNull(rng)?.let { Finance.startBusiness(s, it.id, rng) }
            6 -> LifeSystems.dateOptions.randomOrNull(rng)?.let { LifeSystems.startDating(s, it.type, rng) }
            7 -> LifeSystems.mentor(s, rng)
            8 -> LifeSystems.buyFranchise(s)
            9 -> LifeSystems.post(s, listOf("training", "brand", "hottake").random(rng), rng)
            10 -> if (s.ownedFranchise != null) LifeSystems.signSquadPlayer(s, rng)
            11 -> LifeSystems.acceptFixing(s, rng)
        }
    }

    private fun playSeason(s: GameState, rng: Random) {
        var guard = 0
        while (guard++ < 400) {
            if (s.injuryWeeksLeft > 0) {
                val heal = s.week + s.injuryWeeksLeft
                s.fixtures.filter { !it.played && !it.missed && it.week < heal }.forEach { it.missed = true }
                Finance.processWeeks(s, s.injuryWeeksLeft, rng); s.week = heal.coerceAtMost(52); s.injuryWeeksLeft = 0
            }
            val fx = s.nextFixture() ?: break
            Finance.processWeeks(s, (fx.week - s.week).coerceAtLeast(0), rng); s.week = fx.week
            Tournaments.startFor(s, fx, rng)
            val report = MatchEngine.simulate(s, fx, rng)
            // scorecard sanity
            report.scorecard?.let { c ->
                assertTrue("card 11 batters", c.first.batting.size == 11 && c.second.batting.size == 11)
            }
            Progression.applyReport(s, fx, report, rng)
            s.pendingEvent?.let { ev -> Events.resolve(s, ev, rng.nextInt(ev.choices.size), rng) }
            if (rng.nextDouble() < 0.4) doRandomActions(s, rng)
            checkInvariants(s, "mid-season y${s.year}")
        }
    }

    @Test
    fun fuzzManyCareers() {
        repeat(150) { seed ->
            val rng = Random(seed.toLong() * 100 + 7)
            try {
                val s = freshState(rng)
                var seasons = 0
                while (!s.retired && seasons++ < 30) {
                    if (s.pendingAuction != null) {
                        AuctionEngine.resolve(s, rng.nextBoolean(), rng)
                        Scheduler.buildSeason(s, rng)
                    }
                    playSeason(s, rng)
                    Finance.processWeeks(s, (52 - s.week).coerceAtLeast(0), rng)
                    Progression.endSeason(s, rng)
                    checkInvariants(s, "post-season")
                    if (rng.nextDouble() < 0.1 && s.age >= 30 && !s.retired) {
                        Progression.retire(s)
                        Legacy.chooseSecondCareer(s, Legacy.secondCareers.random(rng).first)
                        repeat(3) { Legacy.advanceRetiredYear(s, rng) }
                    }
                }
                checkInvariants(s, "end seed=$seed")
            } catch (e: Throwable) {
                fail("seed=$seed crashed: ${e::class.simpleName}: ${e.message}")
            }
        }
    }
}
