package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.MatchReport
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.SeasonSummary
import com.mohithash.cricketlegend.model.Stage
import com.mohithash.cricketlegend.model.StatKey
import kotlin.math.max
import kotlin.random.Random

object Progression {

    /** Applies a simulated match to career stats & attributes. Returns records broken this match. */
    fun applyReport(s: GameState, fx: Fixture, report: MatchReport, rng: Random = Random.Default): List<String> {
        val st = s.stat(fx.statKey)
        st.matches++
        for (line in report.batting) {
            st.innings++
            st.runs += line.runs
            st.ballsFaced += line.balls
            st.fours += line.fours
            st.sixes += line.sixes
            if (!line.out) st.notOuts++
            if (line.runs > st.highest || (line.runs == st.highest && !line.out)) {
                st.highest = line.runs; st.highestNotOut = !line.out
            }
            when {
                line.runs >= 200 -> { st.doubleHundreds++; st.hundreds++ }
                line.runs >= 100 -> st.hundreds++
                line.runs >= 50 -> st.fifties++
            }
        }
        for (b in report.bowling) {
            st.ballsBowled += b.balls
            st.runsConceded += b.runsConceded
            st.wickets += b.wickets
            if (b.wickets >= 5) st.fiveWicketHauls++
            if (b.wickets > st.bestBowlingWkts ||
                (b.wickets == st.bestBowlingWkts && b.runsConceded < st.bestBowlingRuns)
            ) {
                st.bestBowlingWkts = b.wickets; st.bestBowlingRuns = b.runsConceded
            }
        }
        st.catches += report.catches
        if (report.manOfTheMatch) st.manOfTheMatch++

        fx.played = true
        fx.won = report.won

        val isCaptainHere = (fx.level == Level.INTERNATIONAL && s.captainNation) ||
            (fx.level == Level.FRANCHISE && s.captainFranchise)
        if (report.matchFee > 0) {
            val fee = if (isCaptainHere) (report.matchFee * 1.2).toLong() else report.matchFee
            Finance.credit(s, "Match fee — ${report.formatLabel}", fee, taxable = true)
        }

        // form / morale / fame
        s.form = (s.form + (report.rating - 5.5) * 0.35).coerceIn(-5.0, 5.0)
        s.morale = (s.morale + (report.rating - 5.0) * 1.2 + if (report.won) 1.0 else -1.0).coerceIn(5.0, 100.0)
        var fameGain = (report.rating - 5.0) * when (fx.level) {
            Level.INTERNATIONAL -> 0.35
            Level.FRANCHISE -> 0.22
            Level.DOMESTIC -> 0.08
        }
        if (isCaptainHere) fameGain *= 1.2
        val diminishing = (1.1 - s.fame / 100.0).coerceIn(0.15, 1.0)
        s.fame = (s.fame + fameGain * diminishing).coerceIn(0.0, 100.0)
        val topScore = report.batting.maxOfOrNull { it.runs } ?: 0
        if (topScore >= 100 && fx.level != Level.DOMESTIC) s.fame = (s.fame + 2.0).coerceIn(0.0, 100.0)
        if (report.bowling.sumOf { it.wickets } >= 5 && fx.level != Level.DOMESTIC) s.fame = (s.fame + 2.0).coerceIn(0.0, 100.0)

        // social following is hard-won: only strong performances move the needle, and it
        // gets progressively harder to grow the bigger you already are (diminishing returns)
        val levelMult = when (fx.level) {
            Level.INTERNATIONAL -> 1.0; Level.FRANCHISE -> 0.55; Level.DOMESTIC -> 0.06
        }
        if (report.rating >= 5.5) {
            val perf = ((report.rating - 5.0) / 5.0).coerceIn(0.0, 1.0)
            LifeSystems.gainFollowers(s, (s.fame * 55 * perf * levelMult).toLong())
        }

        // nemesis tracking (international bowlers only)
        if (fx.level == Level.INTERNATIONAL) {
            for (line in report.batting) {
                if (line.out && line.dismissal.contains("b ")) {
                    val bowler = line.dismissal.substringAfterLast("b ").trim()
                    if (bowler.isNotEmpty() && bowler != "run out") {
                        s.dismissedBy[bowler] = (s.dismissedBy[bowler] ?: 0) + 1
                    }
                }
            }
        }

        // ICC ranking points per format
        if (fx.level == Level.INTERNATIONAL) {
            val rp = s.rankPoints.getOrDefault(fx.statKey, 250.0)
            val newRp = rp * 0.85 + report.rating * 10 * 6.67
            s.rankPoints[fx.statKey] = newRp
            if (rankOf(newRp) == 1 && "no1_${fx.statKey}" !in s.reachedNo1) {
                s.reachedNo1.add("no1_${fx.statKey}")
                s.fame = (s.fame + 4.0).coerceAtMost(100.0)
                s.addNews("WORLD No.1! ${s.playerName} tops the ICC ${StatKey.label(fx.statKey)} rankings!")
            }
        }

        // family strain of the touring life
        if (s.partner != null) s.relationship = (s.relationship - 0.22).coerceAtLeast(0.0)

        // rolling form streak (last 5 scores at franchise/international level)
        if (fx.level != Level.DOMESTIC) {
            report.batting.forEach { s.recentScores.add(it.runs) }
            while (s.recentScores.size > 5) s.recentScores.removeAt(0)
        }

        // mid-season axe: a genuinely poor run can cost your international spot right now
        if (fx.level == Level.INTERNATIONAL && s.form < -2.5 && s.recentScores.size >= 4 &&
            s.recentScores.takeLast(4).all { it < 20 } &&
            rng.nextDouble() < Difficulty.midSeasonDropChance(s)
        ) {
            when {
                s.inNationalTest && fx.statKey == StatKey.INTL_TEST -> { s.inNationalTest = false; s.addNews("AXED mid-series from the Test XI after a wretched run of scores.") }
                s.inNationalODI && fx.statKey == StatKey.INTL_ODI -> { s.inNationalODI = false; s.addNews("Dropped from the ODI side — the selectors have seen enough.") }
                s.inNationalT20 && fx.statKey == StatKey.INTL_T20 -> { s.inNationalT20 = false; s.addNews("Left out of the T20I team mid-season.") }
            }
            s.morale = (s.morale - 10).coerceAtLeast(5.0)
        }

        // story-arc timers
        s.activeArc?.let { if (it.matchesUntilNext > 0) it.matchesUntilNext-- }

        checkMilestones(s, fx)
        recordSplits(s, fx, report, rng)

        // every match dulls your edge; rest between games restores it (Finance.processWeeks)
        s.sharpness = (s.sharpness - 9.0).coerceAtLeast(0.0)

        s.seasonRatings.add(report.rating)
        growSkills(s, report)
        maybeInjury(s, rng)
        val broken = checkRecords(s, report)
        Tournaments.afterPlayerMatch(s, fx, report, rng)
        Tournaments.advance(s, fx, rng)
        Series.afterMatch(s, fx, report)
        LifeSystems.ensureArchRival(s, rng)
        LifeSystems.checkGoat(s)
        LifeSystems.updateNickname(s)
        Legacy.checkLifeMilestones(s)
        Media.afterMatch(s, fx, report, rng)
        Media.weeklyBuzz(s, rng)
        return broken
    }

    /** ICC-style rank from rolling points (converges around 66.7 x avg rating). */
    fun rankOf(points: Double): Int = (100 - (points - 250) / 3.2).toInt().coerceIn(1, 100)

    private fun checkMilestones(s: GameState, fx: Fixture) {
        fun fire(id: String, msg: String) {
            if (id in s.milestonesSeen) return
            s.milestonesSeen.add(id)
            s.fame = (s.fame + 2.0).coerceAtMost(100.0)
            s.morale = (s.morale + 4.0).coerceAtMost(100.0)
            LifeSystems.gainFollowers(s, 60_000)
            s.addNews(msg)
        }
        if (fx.level == Level.INTERNATIONAL) {
            val m = s.intlMatches
            when {
                m == 1 -> fire("intl_debut", "A dream comes true — international debut for ${s.playerName}!")
                m == 50 -> fire("intl_50", "50 international appearances for ${s.playerName}.")
                m == 100 -> fire("intl_100", "A century of international matches!")
                m == 200 -> fire("intl_200", "200 internationals — a modern warhorse.")
                m == 300 -> fire("intl_300", "300 internationals! Rarefied air.")
                m == 400 -> fire("intl_400", "400 internationals — chasing Sachin's 664.")
                m == 500 -> fire("intl_500", "FIVE HUNDRED internationals!")
            }
            if (s.stat(StatKey.INTL_TEST).matches == 100) fire("test_100", "100th TEST MATCH — the ultimate badge of longevity!")
        }
    }

    /** Records deep split statistics for the Stats Hub. */
    private fun recordSplits(s: GameState, fx: Fixture, report: MatchReport, rng: Random) {
        if (fx.level == Level.DOMESTIC) return
        fun bump(map: MutableMap<String, Int>, key: String, v: Int) { map[key] = (map[key] ?: 0) + v }

        val loc = if (fx.home) "loc:home" else "loc:away"
        for (line in report.batting) {
            listOf("opp:${fx.opponent}", "pitch:${fx.pitch}", loc).forEach { k ->
                bump(s.splitRuns, k, line.runs)
                bump(s.splitBalls, k, line.balls)
                if (line.out) bump(s.splitOuts, k, 1)
            }
            // dismissal-type breakdown
            val type = when {
                !line.out -> "Not out"
                line.dismissal.startsWith("lbw") -> "LBW"
                line.dismissal.startsWith("run out") -> "Run out"
                line.dismissal.contains("(keeper)") || line.dismissal.startsWith("st ") -> "Caught behind"
                line.dismissal.startsWith("b ") || line.dismissal.startsWith("c & b") -> "Bowled/c&b"
                line.dismissal.startsWith("c ") -> "Caught"
                else -> "Other"
            }
            bump(s.dismissalTypes, type, 1)
        }

        // best partnership (approximated around your top score with a real teammate)
        val top = report.batting.maxOfOrNull { it.runs } ?: 0
        if (top >= 40) {
            val stand = top + 20 + rng.nextInt(70)
            if (stand > s.bestPartnership) {
                s.bestPartnership = stand
                s.bestPartnershipWith = s.rivals.filter { !it.retired && it.country == s.country && !it.isBowler }
                    .randomOrNull(rng)?.name ?: "a teammate"
            }
        }

        // fantasy points (a single composite for the optimisers)
        var fp = 0L
        report.batting.forEach { fp += it.runs + it.fours + it.sixes * 2 + (if (it.runs >= 100) 16 else if (it.runs >= 50) 8 else 0) }
        report.bowling.forEach { fp += it.wickets * 25 + (if (it.wickets >= 5) 16 else 0) }
        fp += report.catches * 8 + (if (report.manOfTheMatch) 25 else 0)
        s.fantasyPoints += fp
    }

    fun splitAverage(s: GameState, key: String): Double {
        val runs = s.splitRuns[key] ?: return 0.0
        val outs = s.splitOuts[key] ?: 0
        return if (outs > 0) runs.toDouble() / outs else runs.toDouble()
    }

    fun splitStrikeRate(s: GameState, key: String): Double {
        val runs = s.splitRuns[key] ?: return 0.0
        val balls = s.splitBalls[key] ?: 0
        return if (balls > 0) runs * 100.0 / balls else 0.0
    }

    private fun growSkills(s: GameState, report: MatchReport) {
        val ageFactor = when {
            s.age <= 15 -> 2.6   // prodigy years: talent blossoms fast
            s.age <= 19 -> 2.0
            s.age <= 24 -> 1.5
            s.age <= 29 -> 1.0
            s.age <= 33 -> 0.45
            else -> 0.15
        }
        // coaching is now a continuous weekly-budget dial (see Allocations)
        val coach = Allocations.coachingMult(s)
        val batCoach = coach
        val bowlCoach = coach
        val focusBat = when (s.trainingFocus) { "Batting" -> 1.6; "Bowling" -> 0.5; "Fitness" -> 0.7; else -> 1.0 }
        val focusBowl = when (s.trainingFocus) { "Bowling" -> 1.6; "Batting" -> 0.5; "Fitness" -> 0.7; else -> 1.0 }
        val perf = (report.rating / 6.5).coerceIn(0.5, 1.5) * Difficulty.growthMult(s)

        s.batting = (s.batting + 0.06 * ageFactor * batCoach * focusBat * perf).coerceIn(1.0, 99.0)
        if (s.role == Role.BOWLER || s.role == Role.ALL_ROUNDER) {
            s.bowling = (s.bowling + 0.06 * ageFactor * bowlCoach * focusBowl * perf).coerceIn(1.0, 99.0)
        }
        s.fielding = (s.fielding + 0.03 * ageFactor).coerceIn(1.0, 99.0)

        // technique sub-skills trail the main skill, faster under a targeted focus
        val subBase = 0.05 * ageFactor * batCoach * perf
        s.vsPace = (s.vsPace + subBase * (if (s.trainingFocus == "Playing pace") 2.6 else 0.8)).coerceIn(1.0, 99.0)
        s.vsSpin = (s.vsSpin + subBase * (if (s.trainingFocus == "Playing spin") 2.6 else 0.8)).coerceIn(1.0, 99.0)
        s.power = (s.power + subBase * (if (s.trainingFocus == "Power-hitting") 2.6 else 0.7)).coerceIn(1.0, 99.0)
        if (s.role == Role.BOWLER || s.role == Role.ALL_ROUNDER) {
            s.control = (s.control + 0.05 * ageFactor * bowlCoach * perf *
                (if (s.trainingFocus == "Bowling") 1.8 else 0.9)).coerceIn(1.0, 99.0)
        }
        val trainer = 1.0 + (s.staff["trainer"] ?: -1).plus(1) * 0.4
        val fitnessDrift = if (s.age <= 30) 0.05 * trainer else -0.10 / trainer * (s.age - 29)
        val focusFit = if (s.trainingFocus == "Fitness") 0.15 else 0.0
        s.fitness = (s.fitness + fitnessDrift + focusFit).coerceIn(20.0, 99.0)
    }

    private fun maybeInjury(s: GameState, rng: Random) {
        val physio = (s.staff["physio"] ?: -1) + 1  // 0..3
        // tired, under-cooked bodies break down more often
        val fatigueRisk = 1.0 + (100.0 - s.sharpness) / 140.0
        val risk = 0.045 * (1.35 - s.fitness / 100.0) * (1.0 - physio * 0.22) *
            fatigueRisk * Difficulty.injuryMult(s) * Allocations.injuryReduction(s)
        if (rng.nextDouble() < risk.coerceAtLeast(0.004)) {
            val weeks = 1 + rng.nextInt(6) - physio.coerceAtMost(2)
            if (weeks > 0) {
                s.injuryWeeksLeft = weeks
                val type = listOf(
                    "hamstring strain", "side strain", "lower-back spasm", "finger fracture",
                    "ankle twist", "shoulder impingement", "groin niggle"
                ).random(rng)
                s.addNews("Injury blow! ${s.playerName} out for $weeks week(s) with a $type.")
                s.morale = (s.morale - 8).coerceAtLeast(5.0)
                if (weeks >= 4 && s.activeArc == null) {
                    s.activeArc = com.mohithash.cricketlegend.model.ArcState("comeback", step = 1)
                }
            }
        }
    }

    private fun checkRecords(s: GameState, report: MatchReport): List<String> {
        val broken = ArrayList<String>()
        for (rec in RealData.records) {
            if (rec.id in s.brokenRecords) continue
            val holder = WorldSim.dynHolder(s, rec.id)
            val bar = WorldSim.dynValue(s, rec.id)
            val scoredTon = report.batting.any { it.runs >= 100 }
            val done = if (rec.lowerIsBetter) {
                when (rec.id) {
                    "fastest_odi_100", "fastest_t20i_100" -> report.batting.any { line ->
                        line.ballsAtHundred in 1 until bar.toInt() &&
                            report.formatLabel == (if (rec.id == "fastest_odi_100") "ODI" else "T20I")
                    }
                    // youngest-centurion records: age at this century must beat the bar
                    "young_league_100" -> scoredTon && report.formatLabel == "Franchise T20" && s.age < bar.toInt()
                    "young_intl_100" -> scoredTon && report.formatLabel in listOf("Test", "ODI", "T20I") && s.age < bar.toInt()
                    else -> false
                }
            } else {
                RealData.currentValue(rec.id, s) > bar
            }
            if (done && rec.id.startsWith("fantasy_")) {
                // fantasy records never "complete" — they leap higher so there's always a next mountain
                broken.add("${rec.title} conquered!")
                val cur = RealData.currentValue(rec.id, s)
                val nextBar = when (rec.id) {
                    "fantasy_test_high" -> cur + 100      // +100-run tiers: 400 → 500 → 600...
                    else -> (cur * 1.5)                    // 50k → 75k → 112k...
                }
                s.dynamicRecords[rec.id] = com.mohithash.cricketlegend.model.DynRecord(s.playerName, nextBar)
                s.fame = 100.0
                s.addNews("HISTORY! ${s.playerName} conquers '${rec.title}'. The bar is raised to ${nextBar.toInt()}.")
            } else if (done) {
                s.brokenRecords.add(rec.id)
                broken.add(rec.title)
                val newValue = when {
                    rec.id.startsWith("young_") -> s.age.toDouble()
                    rec.lowerIsBetter -> report.batting.filter { it.ballsAtHundred > 0 }.minOf { it.ballsAtHundred }.toDouble()
                    else -> RealData.currentValue(rec.id, s)
                }
                s.dynamicRecords[rec.id] = com.mohithash.cricketlegend.model.DynRecord(s.playerName, newValue)
                s.fame = (s.fame + 6.0).coerceAtMost(100.0)
                s.addNews("WORLD RECORD! ${rec.title} — ${s.playerName} surpasses $holder!")
            }
        }
        return broken
    }

    // ---------------- Season rollover ----------------

    fun endSeason(s: GameState, rng: Random = Random.Default) {
        val avgRating = if (s.seasonRatings.isNotEmpty()) s.seasonRatings.average() else 5.0

        // fame fades a little every off-season unless you keep making headlines
        s.fame = (s.fame * 0.96).coerceAtLeast(0.0)

        // age & decline
        s.age++
        s.year++
        s.week = 1
        s.seasonOver = false

        if (s.age >= 34) {
            val trainer = 1.0 + ((s.staff["trainer"] ?: -1) + 1) * 0.35
            s.batting = (s.batting - (s.age - 33) * 0.5 / trainer).coerceAtLeast(20.0)
            s.bowling = (s.bowling - (s.age - 33) * 0.5 / trainer).coerceAtLeast(15.0)
        }

        Series.evaluateObjectives(s)
        s.series = null
        WorldSim.advanceSeason(s, rng)
        WorldFixtures.simulateWorldSeason(s, rng)
        awardsNight(s, avgRating)
        updateSelection(s, avgRating, rng)
        updateContract(s, avgRating)
        updateCaptaincy(s, avgRating, rng)
        Finance.yearlyBusinessReview(s, rng)
        LifeSystems.franchiseYearlyPnL(s, rng)
        if (s.banSeasons > 0) {
            s.banSeasons--
            if (s.banSeasons == 0) s.addNews("${s.playerName}'s ban is lifted — a long road back to redemption begins.")
            else s.addNews("Serving a ban: ${s.banSeasons} more season(s) on the sidelines.")
        }
        if (s.married) s.relationship = (s.relationship + 5).coerceAtMost(100.0)

        // endorsement aging
        val expired = s.endorsements.filter { --it.yearsLeft <= 0 }
        for (deal in expired) {
            s.endorsements.remove(deal)
            s.addNews("Endorsement with ${deal.brand} has ended.")
        }

        // property market drift
        for (p in s.propertyMarket) {
            val drift = 0.97 + rng.nextDouble() * 0.16   // -3% .. +13%
            p.price = (p.price * drift).toLong()
        }

        val runsThisSeason = s.seasonRatings.size
        s.history.add(
            SeasonSummary(
                s.year - 1,
                "Age ${s.age - 1}: $runsThisSeason matches, avg rating ${"%.1f".format(avgRating)}" +
                    (if (s.trophies.isNotEmpty()) ", trophies: ${s.trophies.size}" else "")
            )
        )
        // snapshot career trajectory for the Home dashboard graph
        s.careerRunsBySeason.add(s.intlRuns + s.stat(StatKey.LEAGUE).runs)
        s.legacyBySeason.add(LifeSystems.goatScore(s))

        // rank + batting-average history for the Stats Hub graphs
        for (k in StatKey.INTL) {
            s.rankPoints[k]?.let { s.rankHist.getOrPut(k) { mutableListOf() }.add(rankOf(it)) }
        }
        val intlInns = StatKey.INTL.sumOf { s.stat(it).innings - s.stat(it).notOuts }
        s.battingAvgBySeason.add(if (intlInns > 0) (s.intlRuns * 10 / intlInns) else 0)
        while (s.battingAvgBySeason.size > 30) s.battingAvgBySeason.removeAt(0)
        while (s.careerRunsBySeason.size > 30) s.careerRunsBySeason.removeAt(0)
        while (s.legacyBySeason.size > 30) s.legacyBySeason.removeAt(0)

        s.seasonRatings.clear()
        s.tournament = null

        // auction first — the schedule depends on where (or whether) you land
        AuctionEngine.createSeasonAuction(s, rng)
        if (s.pendingAuction == null) {
            Scheduler.buildSeason(s, rng)
        }
        Series.setSeasonObjectives(s, rng)
        s.addNews("A new season begins: ${s.year}. Age ${s.age}.")

        if (s.age >= 42 && !s.retired) retire(s)
    }

    private fun awardsNight(s: GameState, avgRating: Double) {
        val intlMatchesThisSeason = s.seasonRatings.size
        if (avgRating >= 7.6 && intlMatchesThisSeason >= 15 &&
            (s.inNationalT20 || s.inNationalODI || s.inNationalTest)
        ) {
            val challenger = WorldSim.awardChallenger(s, avgRating * 11 + s.fame / 4, Random.Default)
            if (challenger != null) {
                s.addNews("AWARDS NIGHT: ${challenger.name} pips you to ICC Cricketer of the Year. Fuel for next season.")
                s.morale = (s.morale - 3).coerceAtLeast(5.0)
            } else {
                s.awards.add("${s.year} — ICC Cricketer of the Year")
                s.fame = (s.fame + 5.0).coerceAtMost(100.0)
                s.addNews("AWARDS NIGHT: ${s.playerName} is the ICC Cricketer of the Year!")
            }
        } else if (avgRating >= 7.0 && intlMatchesThisSeason >= 12) {
            s.awards.add("${s.year} — National Cricketer of the Year")
            s.fame = (s.fame + 3.0).coerceAtMost(100.0)
            s.addNews("AWARDS NIGHT: ${s.playerName} named National Cricketer of the Year.")
        }
        val arjuna = if (s.country == "India") "Arjuna Award" else "National Sports Honour"
        if (s.fame >= 60 && s.awards.none { it.contains(arjuna) }) {
            s.awards.add("${s.year} — $arjuna")
            s.addNews("$arjuna conferred upon ${s.playerName}!")
        }
        val ratna = if (s.country == "India") "Khel Ratna" else "Order of Sporting Merit"
        if (s.fame >= 85 && s.brokenRecords.isNotEmpty() && s.awards.none { it.contains(ratna) }) {
            s.awards.add("${s.year} — $ratna")
            s.fame = (s.fame + 3.0).coerceAtMost(100.0)
            s.addNews("The nation's highest sporting honour — $ratna for ${s.playerName}!")
        }
    }

    private fun updateCaptaincy(s: GameState, avgRating: Double, rng: Random) {
        if (!s.captainNation && s.inNationalODI && s.inNationalT20 && s.fame >= 60 &&
            s.age >= 26 && avgRating >= 6.0 && s.publicImage >= -5 && rng.nextDouble() < 0.4
        ) {
            s.captainNation = true
            s.fame = (s.fame + 5.0).coerceAtMost(100.0)
            s.addNews("CAPTAIN! ${s.playerName} is named captain of ${s.country}!")
        } else if (s.captainNation && (avgRating < 4.5 || s.publicImage < -25)) {
            s.captainNation = false
            s.addNews("The board relieves ${s.playerName} of the national captaincy.")
            s.morale = (s.morale - 8).coerceAtLeast(5.0)
        }
        if (!s.captainFranchise && s.franchiseTeam != null && s.fame >= 45 &&
            s.age >= 25 && rng.nextDouble() < 0.35
        ) {
            s.captainFranchise = true
            s.addNews("${s.franchiseTeam} hand ${s.playerName} the captaincy!")
        }
    }

    private fun updateSelection(s: GameState, avgRating: Double, rng: Random) {
        val bestSkill = max(s.batting, s.bowling)
        val bar = Difficulty.selectionBar(s)   // harder difficulty raises the whole ladder
        // international cricket has a minimum age (17), however famous the teenager is
        if (s.age < 17) { s.fame = s.fame.coerceIn(0.0, 100.0); return }
        if (!s.inNationalT20 && "T20I" !in s.retiredFormats && s.fame >= 20 + bar && bestSkill >= 63 + bar) {
            s.inNationalT20 = true
            s.addNews("MAIDEN CALL-UP! Selected for ${s.country}'s T20I squad!")
            s.fame += 5
        }
        if (s.formatFocus != "T20Only" &&
            !s.inNationalODI && "ODI" !in s.retiredFormats && s.fame >= 30 + bar && bestSkill >= 68 + bar) {
            s.inNationalODI = true
            s.addNews("Selected for ${s.country}'s ODI squad!")
            s.fame += 4
        }
        if (s.formatFocus == "All" &&
            !s.inNationalTest && "TEST" !in s.retiredFormats && s.fame >= 40 && bestSkill >= 72) {
            s.inNationalTest = true
            s.addNews("The ultimate honour — Test cap incoming for ${s.playerName}!")
            s.fame += 5
        }
        // poor seasons can cost your spot
        if (avgRating < 4.2 && rng.nextDouble() < 0.5) {
            when {
                s.inNationalTest -> { s.inNationalTest = false; s.addNews("Dropped from the Test side after a lean run.") }
                s.inNationalODI -> { s.inNationalODI = false; s.addNews("Dropped from the ODI squad.") }
                s.inNationalT20 -> { s.inNationalT20 = false; s.addNews("Left out of the T20I setup.") }
            }
        }
        s.fame = s.fame.coerceIn(0.0, 100.0)
    }

    private fun updateContract(s: GameState, avgRating: Double) {
        val formats = listOf(s.inNationalT20, s.inNationalODI, s.inNationalTest).count { it }
        val (grade, value) = when {
            formats == 3 && avgRating >= 7.0 -> "A+" to 70_000_000L
            formats >= 2 && avgRating >= 6.0 -> "A" to 50_000_000L
            formats >= 2 -> "B" to 30_000_000L
            formats == 1 -> "C" to 10_000_000L
            else -> null to 0L
        }
        if (grade != s.contractGrade) {
            if (grade != null) s.addNews("Central contract: Grade $grade (${Money.fmt(value, s.country)}/yr).")
            else if (s.contractGrade != null) s.addNews("Central contract not renewed.")
        }
        s.contractGrade = grade
        s.contractYearly = value
    }

    fun retire(s: GameState) {
        s.retired = true
        var score = 0.0
        score += s.intlRuns / 100.0
        score += s.intlWickets * 1.6
        score += s.intlHundreds * 6.0
        score += s.brokenRecords.size * 60.0
        score += s.trophies.size * 45.0
        score += s.awards.size * 12.0
        score += s.fame
        s.legacyScore = score.toInt()
        // a farewell testimonial match — the fans pay tribute, and the cheque is fat
        val testimonial = (s.legacyScore * 3_000_000L).coerceIn(20_000_000L, 3_000_000_000L)
        Finance.credit(s, "Testimonial / farewell match", testimonial, taxable = true)
        s.addNews("A packed house bids farewell at ${s.playerName}'s testimonial match. ${Money.fmt(testimonial, s.country)} raised!")
        s.addNews("${s.playerName} announces retirement. Legacy score: ${s.legacyScore}.")
    }

    fun legacyTitle(score: Int): String = when {
        score >= 900 -> "The Greatest of All Time"
        score >= 600 -> "An All-Time Legend"
        score >= 400 -> "A Modern Great"
        score >= 250 -> "A National Hero"
        score >= 120 -> "A Solid International"
        score >= 50 -> "A Domestic Stalwart"
        else -> "A Journeyman Cricketer"
    }
}
