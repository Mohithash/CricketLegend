package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.model.*
import org.junit.Test
import org.junit.Assert.assertTrue
import kotlin.random.Random

class GodModeTest {
    private fun eliteBat(diff: String) = GameState(country="India", role=Role.BATTER, batting=95.0,
        vsPace=95.0, vsSpin=95.0, power=92.0, fitness=99.0, morale=90.0, battingPosition=3, difficulty=diff)
    private fun avgBat() = GameState(country="India", role=Role.BATTER, batting=60.0,
        vsPace=60.0, vsSpin=60.0, power=58.0, fitness=70.0, battingPosition=3, difficulty="Realistic")

    private fun t20Avg(g: GameState, n: Int): Double {
        val fx = Fixture(1,20,Format.T20,Level.INTERNATIONAL,StatKey.INTL_T20,"Australia",venue="MCG",pitch="FLAT")
        var runs=0; var out=0
        repeat(n){ val b=MatchEngine.simulate(g, fx, Random(it)).batting[0]; runs+=b.runs; if(b.out) out++ }
        return runs.toDouble()/out.coerceAtLeast(1)
    }

    @Test fun realismCurveAndDifficultyOrdering() {
        val elite = t20Avg(eliteBat("Realistic"), 600)
        val avg = t20Avg(avgBat(), 600)
        val eliteEasy = t20Avg(eliteBat("Easy"), 600)
        val eliteHard = t20Avg(eliteBat("Hardcore"), 600)
        println("Realistic elite T20 avg=$elite | avg player=$avg | Easy elite=$eliteEasy | Hardcore elite=$eliteHard")
        // an elite bat should be clearly better than an average one, but not invincible
        assertTrue("elite ($elite) should beat average ($avg)", elite > avg + 8)
        assertTrue("elite T20 avg should be believable (<95), not 175", elite < 95)
        assertTrue("average player realistic (<45)", avg < 45)
        // difficulty must matter: Easy is kinder than Hardcore for the same player
        assertTrue("Easy ($eliteEasy) easier than Hardcore ($eliteHard)", eliteEasy > eliteHard)
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
