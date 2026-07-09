package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.model.*
import org.junit.Test
import org.junit.Assert.assertTrue
import kotlin.random.Random

class GodModeTest {
    private fun godBat() = GameState(country="India", role=Role.BATTER, batting=99.0,
        vsPace=99.0, vsSpin=99.0, power=99.0, fitness=99.0, morale=100.0, battingPosition=1)
    private fun avgBat() = GameState(country="India", role=Role.BATTER, batting=60.0,
        vsPace=60.0, vsSpin=60.0, power=58.0, fitness=70.0, battingPosition=3)

    @Test fun godModeDominates() {
        val fx = Fixture(1,20,Format.T20,Level.INTERNATIONAL,StatKey.INTL_T20,"Australia",venue="MCG",pitch="FLAT")
        var god=0; var godTons=0; var avg=0
        val g = godBat(); val a = avgBat()
        repeat(500){
            val gr = MatchEngine.simulate(g, fx, Random(it)).batting.sumOf{b->b.runs}
            god+=gr; if(gr>=100) godTons++
            avg += MatchEngine.simulate(a, fx, Random(it+9999)).batting.sumOf{b->b.runs}
        }
        println("GOD T20 avg=${god/500.0} tons/500=$godTons | AVG player=${avg/500.0}")
        // ODI
        val fxo = Fixture(1,20,Format.ODI,Level.INTERNATIONAL,StatKey.INTL_ODI,"Australia",venue="MCG",pitch="FLAT")
        var godO=0; var godODoubles=0
        repeat(300){ val r=MatchEngine.simulate(g, fxo, Random(it)).batting.sumOf{b->b.runs}; godO+=r; if(r>=200) godODoubles++ }
        println("GOD ODI avg=${godO/300.0} doubles/300=$godODoubles")
        assertTrue("god T20 avg should be huge", god/500.0 > 80)
        assertTrue("god should hit a ton most games", godTons > 300)
        assertTrue("average player must stay realistic", avg/500.0 < 45)
    }

    @Test fun scoreboardMatchesResult() {
        // limited-overs winner's total must be higher than the loser's — no more "won with a lower score"
        fun firstInt(txt: String) = Regex("\\d+").find(txt)?.value?.toInt() ?: 0
        val s = avgBat()
        var checked = 0
        for (fmt in listOf(Format.T20 to StatKey.INTL_T20, Format.ODI to StatKey.INTL_ODI)) {
            repeat(400) {
                val fx = Fixture(1, 20, fmt.first, Level.INTERNATIONAL, fmt.second, "Australia", venue="MCG", pitch="FLAT")
                val r = MatchEngine.simulate(s, fx, Random(it))
                val us = firstInt(r.teamScoreText); val them = firstInt(r.oppScoreText)
                if (r.won) assertTrue("won but our $us <= their $them", us > them)
                else assertTrue("lost but our $us > their $them", us < them)
                checked++
            }
        }
        assertTrue(checked > 700)
    }

    @Test fun formatFocusRemovesTests() {
        val s = GameState(country="India", role=Role.BATTER, batting=90.0, fame=80.0,
            inNationalTest=true, inNationalODI=true, inNationalT20=true, formatFocus="T20Only")
        com.mohithash.cricketlegend.engine.Scheduler.buildSeason(s, Random(1))
        assertTrue("no first-class/Test in T20-only", s.fixtures.none { it.format == Format.FIRST_CLASS })
        assertTrue("no ODI in T20-only", s.fixtures.none { it.format == Format.ODI })
        assertTrue("has T20 fixtures", s.fixtures.any { it.format == Format.T20 })
    }
}
