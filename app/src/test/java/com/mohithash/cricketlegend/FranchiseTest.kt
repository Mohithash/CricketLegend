package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.FranchiseEngine
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.random.Random

class FranchiseTest {

    @Test
    fun ruinedFranchiseIsPlayable() {
        val rng = Random(5)
        val g = FranchiseEngine.newRuinedFranchise("Lucknow Legends", rng)
        assertTrue("starts in debt", g.debt > 0)
        assertTrue("weak starting squad", g.squadStrength() < 65)
        assertTrue("auction pool filled", g.auctionPool.isNotEmpty())
        assertTrue("has a board target", g.boardTarget.isNotEmpty())
        assertTrue("all 9 rivals have real squads", g.rivalSquads.size == 9 &&
            g.rivalSquads.values.all { it.size >= 15 })
    }

    @Test
    fun matchByMatchSeasonAccruesRealStats() {
        val rng = Random(9)
        val g = FranchiseEngine.newRuinedFranchise("Chennai Emperors", rng, "Skipper", "BAT", true)
        FranchiseEngine.startSeason(g, rng)
        assertTrue("14 fixtures", g.fixtures.size == 14)
        // play one match: scorecard + accrual + standings move
        assertTrue(FranchiseEngine.playNextFixture(g, rng))
        val card = g.lastScorecard!!
        assertTrue("plausible batters per innings", card.first.batting.size in 1..11 && card.second.batting.size in 1..11)
        val totalRuns = (g.squad + g.rivalSquads.values.flatten()).sumOf { it.seasonRuns }
        assertTrue("league-wide runs accrued from real sims", totalRuns > 0)
        assertTrue("standings updated", g.standings.values.sum() >= 4)
        // finish the season; manager career should accrue and world evolve
        FranchiseEngine.simulateSeason(g, rng)
        assertTrue("season completed back to auction", g.phase == "AUCTION")
        assertTrue("manager career accrued", g.myMatches > 0)
        assertTrue("rival squads evolved (aged)", g.rivalSquads.values.flatten().any { it.age >= 19 })
        assertTrue("cap-race leaders exist", FranchiseEngine.leagueTopBats(g, 3).first().seasonRuns >= 0)
    }

    @Test
    fun financesAndSeasonsAreStable() {
        repeat(60) { seed ->
            val rng = Random(seed.toLong())
            try {
                val g = FranchiseEngine.newRuinedFranchise(RealData.franchises.random(rng).name, rng)
                var guard = 0
                while (!g.won && !g.bankrupt && !g.sacked && guard++ < 40) {
                    // random auction signings within purse
                    repeat(rng.nextInt(5)) {
                        if (g.auctionPool.isNotEmpty()) FranchiseEngine.signPlayer(g, rng.nextInt(g.auctionPool.size))
                    }
                    // random upgrades + debt payments
                    if (rng.nextBoolean()) FranchiseEngine.upgrade(g, FranchiseEngine.facilities.random(rng).id)
                    if (rng.nextBoolean() && g.cash > 0) FranchiseEngine.payDebt(g, g.cash / 3)
                    FranchiseEngine.simulateSeason(g, rng)

                    // invariants
                    assertTrue("squad >= 11 after season", g.squad.size >= 11)
                    assertTrue("squad <= 26", g.squad.size <= 26)
                    assertTrue("fans in range", g.fanHappiness in 0.0..100.0)
                    assertTrue("board in range", g.boardConfidence in 0.0..100.0)
                    g.squad.forEach { assertTrue("rating in range", it.rating in 30..99) }
                    assertTrue("cash finite", g.cash > Long.MIN_VALUE / 2 && g.cash < Long.MAX_VALUE / 2)
                }
            } catch (e: Throwable) {
                fail("seed=$seed franchise crashed: ${e::class.simpleName}: ${e.message}")
            }
        }
    }

    @Test
    fun playerManagerAccruesPersonalStats() {
        val rng = Random(3)
        val g = FranchiseEngine.newRuinedFranchise("Delhi Dynamos", rng, "Test Star", "AR", true)
        assertTrue("manager is in the squad", g.squad.any { it.isManager })
        // can't release yourself
        val me = g.squad.first { it.isManager }
        FranchiseEngine.releasePlayer(g, me.name)
        assertTrue("manager not releasable", g.squad.any { it.isManager })
        repeat(6) { FranchiseEngine.simulateSeason(g, rng) }
        assertTrue("accrued matches", g.myMatches > 0)
        assertTrue("accrued runs", g.myRuns > 0)
        assertTrue("season line set", g.mySeasonLine.isNotEmpty())
        assertTrue("rating developed from 52", g.squad.first { it.isManager }.rating >= 52)
    }

    @Test
    fun goodManagementCanEscapeDebt() {
        // a competent manager (always signs the best, upgrades marketing/stadium, pays debt)
        val rng = Random(11)
        val g = FranchiseEngine.newRuinedFranchise("Mumbai Mavericks", rng)
        var escaped = false
        repeat(25) {
            // shrewd strategy: invest in stars early, then run a lean wage bill
            if (g.seasonsRun < 3) {
                g.auctionPool.sortedByDescending { it.rating }.take(3).forEach {
                    FranchiseEngine.signPlayer(g, g.auctionPool.indexOf(it))
                }
            }
            while (g.squad.size > 15) {
                val weakest = g.squad.filter { !it.isManager }.minByOrNull { it.rating } ?: break
                FranchiseEngine.releasePlayer(g, weakest.name)
            }
            FranchiseEngine.upgrade(g, "marketing")
            FranchiseEngine.upgrade(g, "stadium")
            if (g.cash > 0) FranchiseEngine.payDebt(g, g.cash)
            FranchiseEngine.simulateSeason(g, rng)
            if (g.debt <= 0) escaped = true
        }
        assertTrue("a strong manager should be able to clear the debt within 25 seasons", escaped)
    }
}
