package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.Format
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.Stage
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

object Scheduler {

    fun buildSeason(s: GameState, rng: Random = Random.Default) {
        s.fixtures.clear()
        var id = 1

        // banned players sit out the whole season
        if (s.banSeasons > 0) return
        val intlOpps = RealData.opponentsFor(s.country).shuffled(rng)
        val domOpps = RealData.domesticTeams.shuffled(rng)

        fun add(week: Int, format: Format, level: Level, statKey: String, opponent: String,
                venueCountry: String, tournament: String? = null, stage: Stage? = null) {
            val v = RealData.venueIn(venueCountry, rng)
            s.fixtures.add(Fixture(id++, week, format, level, statKey, opponent,
                tournament, stage, venue = v.name, pitch = v.pitch, home = (venueCountry == s.country)))
        }

        // --- Prodigy years: age-group cricket only until ready for domestic ---
        if (s.age < 13) {
            val ageGroups = listOf("U-13 Trophy", "U-16 Challenger", "State Academy XI", "Zonal Colts")
            listOf(3, 6, 9, 12, 15, 18, 22, 26, 30, 34, 38, 42, 46).forEachIndexed { i, w ->
                val fmt = if (i % 3 == 0) Format.ODI else Format.T20
                add(w, fmt, Level.DOMESTIC, StatKey.YOUTH, ageGroups[i % ageGroups.size], s.country)
            }
            s.fixtures.sortWith(compareBy({ it.week }, { it.id }))
            return
        }

        val allowRed = s.formatFocus == "All"        // Tests / first-class
        val allowOdi = s.formatFocus != "T20Only"    // ODIs / List-A

        // --- Weeks 2-13: home season block ---
        if (allowRed) {
            if (s.inNationalTest) {
                val (oppA, oppB) = intlOpps[0].name to intlOpps[1].name
                listOf(2, 4, 6).forEach { add(it, Format.FIRST_CLASS, Level.INTERNATIONAL, StatKey.INTL_TEST, oppA, s.country) }
                listOf(8, 10, 12).forEach { add(it, Format.FIRST_CLASS, Level.INTERNATIONAL, StatKey.INTL_TEST, oppB, s.country) }
            } else {
                listOf(2, 4, 6, 8, 10, 12).forEachIndexed { i, w ->
                    add(w, Format.FIRST_CLASS, Level.DOMESTIC, StatKey.DOM_FC, domOpps[i % domOpps.size], s.country)
                }
            }
        } else if (s.formatFocus == "T20Only") {
            // extra domestic T20 to fill the calendar for a pure white-ball specialist
            listOf(2, 5, 8, 11).forEach { w -> add(w, Format.T20, Level.DOMESTIC, StatKey.DOM_T20, domOpps[w % domOpps.size], s.country) }
        }
        if (allowOdi) {
            if (s.inNationalODI) {
                listOf(3, 5, 7, 9, 11).forEach { add(it, Format.ODI, Level.INTERNATIONAL, StatKey.INTL_ODI, intlOpps[2].name, s.country) }
            } else {
                listOf(3, 5, 7, 9).forEach { w -> add(w, Format.ODI, Level.DOMESTIC, StatKey.DOM_LIST_A, domOpps[w % domOpps.size], s.country) }
            }
        }

        // --- Weeks 14-22: franchise league, real 14-game season (each rival + 5 repeats) ---
        if (s.franchiseTeam != null) {
            val league = "Premier T20 League ${s.year}"
            // a fixed derby rival makes two of those games mean everything
            if (s.derbyRival == null || s.derbyRival == s.franchiseTeam) {
                s.derbyRival = RealData.franchiseTeams.filter { it != s.franchiseTeam }.random(rng)
            }
            val others = RealData.franchiseTeams.filter { it != s.franchiseTeam }.shuffled(rng)
            val slate = (others + others.take(4) + listOf(s.derbyRival!!)).take(14)
            var w = 14
            slate.forEachIndexed { i, opp ->
                add(w, Format.T20, Level.FRANCHISE, StatKey.LEAGUE, opp, "India", league, Stage.GROUP)
                if (i % 2 == 1) w++
            }
        } else {
            (15..22).forEach { w -> add(w, Format.T20, Level.DOMESTIC, StatKey.DOM_T20, domOpps[w % domOpps.size], s.country) }
        }

        // --- Week 28: WTC Final (odd years, if the team qualifies) ---
        if (allowRed && s.year % 2 == 1 && s.inNationalTest) {
            val myStrength = RealData.teams.firstOrNull { it.name == s.country }?.strength ?: 78
            if (rng.nextDouble() < (myStrength - 55) / 50.0) {
                val opp = intlOpps.take(3).maxByOrNull { it.strength }!!.name
                add(28, Format.FIRST_CLASS, Level.INTERNATIONAL, StatKey.INTL_TEST, opp,
                    "England", "WTC Final ${s.year}", Stage.FINAL)
            }
        }

        // --- Weeks 26-38: away tours ---
        if (s.inNationalT20) {
            listOf(26, 27, 33, 34).forEach { add(it, Format.T20, Level.INTERNATIONAL, StatKey.INTL_T20, intlOpps[3].name, intlOpps[3].name) }
        } else {
            listOf(26, 28, 33).forEach { w -> add(w, Format.T20, Level.DOMESTIC, StatKey.DOM_T20, domOpps[(w + 1) % domOpps.size], s.country) }
        }
        if (allowOdi && s.inNationalODI) {
            listOf(29, 30, 36).forEach { add(it, Format.ODI, Level.INTERNATIONAL, StatKey.INTL_ODI, intlOpps[4].name, intlOpps[4].name) }
        } else if (allowOdi) {
            listOf(29, 36).forEach { w -> add(w, Format.ODI, Level.DOMESTIC, StatKey.DOM_LIST_A, domOpps[(w + 2) % domOpps.size], s.country) }
        }
        if (allowRed && s.inNationalTest) {
            listOf(31, 35, 38).forEach { add(it, Format.FIRST_CLASS, Level.INTERNATIONAL, StatKey.INTL_TEST, intlOpps[5].name, intlOpps[5].name) }
        }

        // --- Weeks 42-46: World Cups & Champions Trophy ---
        val host = RealData.teams.random(rng).name
        if (s.year % 2 == 0 && s.inNationalT20) {
            val t = "T20 World Cup ${s.year}"
            (42..46).forEach { w ->
                add(w, Format.T20, Level.INTERNATIONAL, StatKey.INTL_T20, intlOpps[(w - 42) % intlOpps.size].name, host, t, Stage.GROUP)
            }
        }
        if (allowOdi && (s.year - 2027) % 4 == 0 && s.year >= 2027 && s.inNationalODI) {
            val t = "ODI World Cup ${s.year}"
            (42..46).forEach { w ->
                add(w, Format.ODI, Level.INTERNATIONAL, StatKey.INTL_ODI, intlOpps[(w - 42) % intlOpps.size].name, host, t, Stage.GROUP)
            }
        }
        if (allowOdi && (s.year - 2029) % 4 == 0 && s.year >= 2029 && s.inNationalODI) {
            val t = "Champions Trophy ${s.year}"
            (42..45).forEach { w ->
                add(w, Format.ODI, Level.INTERNATIONAL, StatKey.INTL_ODI, intlOpps[(w - 42) % intlOpps.size].name, host, t, Stage.GROUP)
            }
        }

        // --- Weeks 47-50: late-season white-ball series (skipped in ICC-event years) ---
        val iccYear = (s.year % 2 == 0) || ((s.year - 2027) % 4 == 0) || ((s.year - 2029) % 4 == 0)
        if (allowOdi && s.inNationalODI && !iccYear) {
            listOf(47, 48).forEach { add(it, Format.ODI, Level.INTERNATIONAL, StatKey.INTL_ODI, intlOpps[6].name, intlOpps[6].name) }
        }
        if (s.inNationalT20 && !iccYear) {
            listOf(49, 50).forEach { add(it, Format.T20, Level.INTERNATIONAL, StatKey.INTL_T20, intlOpps[7].name, intlOpps[7].name) }
        }

        s.fixtures.sortWith(compareBy({ it.week }, { it.id }))
    }
}
