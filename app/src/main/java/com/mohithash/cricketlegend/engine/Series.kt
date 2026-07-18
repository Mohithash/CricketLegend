package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.MatchReport
import com.mohithash.cricketlegend.model.Objective
import com.mohithash.cricketlegend.model.SeriesState
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

/** Bilateral series tracking (2-1 scorelines, Player of the Series) + board objectives. */
object Series {

    /** Call after every match. Bilateral internationals only (tournaments have their own tables). */
    fun afterMatch(s: GameState, fx: Fixture, report: MatchReport) {
        if (fx.level != Level.INTERNATIONAL || fx.tournament != null) {
            return
        }
        var st = s.series
        if (st == null || st.opponent != fx.opponent || st.statKey != fx.statKey) {
            // a new rubber begins — length = this match + remaining identical fixtures
            val remaining = s.fixtures.count {
                !it.played && !it.missed && it.tournament == null &&
                    it.opponent == fx.opponent && it.statKey == fx.statKey
            }
            st = SeriesState(fx.opponent, fx.statKey, length = remaining + 1)
            s.series = st
            if (st.length > 1) {
                s.addNews("${st.length}-match ${StatKey.label(fx.statKey)} series vs ${fx.opponent} begins.")
            }
        }
        st.played++
        if (report.won) st.wins++ else st.losses++
        st.ratingSum += report.rating

        if (st.played >= st.length) {
            finish(s, st)
            s.series = null
        } else if (st.length >= 3) {
            s.addNews("Series score: ${s.country} ${st.wins}-${st.losses} ${st.opponent} (${st.played}/${st.length}).")
        }
    }

    private fun finish(s: GameState, st: SeriesState) {
        if (st.length < 2) return
        val label = "${StatKey.label(st.statKey)} series vs ${st.opponent}"
        when {
            st.wins > st.losses -> {
                s.fame = (s.fame + 2.5).coerceAtMost(100.0)
                s.addNews("SERIES WON ${st.wins}-${st.losses}! The $label is ${s.country}'s.")
            }
            st.wins == st.losses -> s.addNews("The $label ends level at ${st.wins}-${st.losses}.")
            else -> s.addNews("Series lost ${st.wins}-${st.losses} — the $label slips away.")
        }
        if (st.ratingSum / st.played >= 7.3) {
            s.awards.add("${s.year} — Player of the Series vs ${st.opponent} (${StatKey.label(st.statKey)})")
            s.fame = (s.fame + 3.0).coerceAtMost(100.0)
            s.addNews("PLAYER OF THE SERIES! ${s.playerName} owned the $label.")
        }
    }

    // ---------------- Board objectives ----------------

    fun setSeasonObjectives(s: GameState, rng: Random) {
        s.seasonObjectives.clear()
        if (s.age < 16 || s.retired) return
        val runTarget = when {
            s.batting >= 90 -> 2500
            s.batting >= 75 -> 1500
            else -> 700
        }
        s.seasonObjectives.add(Objective(
            "runs", "Score $runTarget runs across all cricket", runTarget,
            s.intlRuns + StatKey.ALL.sumOf { k -> if (k in StatKey.INTL) 0 else s.stat(k).runs },
            20_000_000, 2.0))
        s.seasonObjectives.add(Objective(
            "trophy", "Win a trophy this season", s.trophies.size + 1, s.trophies.size,
            50_000_000, 4.0))
        if (rng.nextBoolean()) {
            s.seasonObjectives.add(Objective(
                "image", "End the season a fan favourite (image 10+)", 10, Int.MIN_VALUE, 10_000_000, 1.5))
        } else {
            s.seasonObjectives.add(Objective(
                "tons", "Hit ${if (s.batting >= 85) 8 else 3} hundreds",
                (if (s.batting >= 85) 8 else 3) + s.intlHundreds + s.stat(StatKey.LEAGUE).hundreds,
                s.intlHundreds + s.stat(StatKey.LEAGUE).hundreds, 30_000_000, 2.5))
        }
    }

    fun objectiveProgress(s: GameState, o: Objective): Pair<Int, Int> = when (o.id) {
        "runs" -> {
            val cur = s.intlRuns + StatKey.ALL.sumOf { k -> if (k in StatKey.INTL) 0 else s.stat(k).runs }
            (cur - o.baseline) to o.target
        }
        "trophy" -> (s.trophies.size - o.baseline) to 1
        "image" -> s.publicImage.toInt() to o.target
        "tons" -> {
            val cur = s.intlHundreds + s.stat(StatKey.LEAGUE).hundreds
            (cur - o.baseline) to (o.target - o.baseline)
        }
        else -> 0 to 1
    }

    fun evaluateObjectives(s: GameState) {
        for (o in s.seasonObjectives) {
            val (cur, target) = objectiveProgress(s, o)
            if (cur >= target) {
                Finance.credit(s, "Board objective bonus: ${o.label}", o.rewardMoney, taxable = true)
                s.fame = (s.fame + o.rewardFame).coerceAtMost(100.0)
                s.addNews("OBJECTIVE MET ✔ ${o.label} — bonus ${Money.fmt(o.rewardMoney, s.country)}.")
            } else {
                s.addNews("Objective missed: ${o.label} ($cur/$target). The board takes note.")
            }
        }
        s.seasonObjectives.clear()
    }
}
