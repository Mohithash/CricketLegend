package com.mohithash.cricketlegend

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Events
import com.mohithash.cricketlegend.engine.Finance
import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.engine.Progression
import com.mohithash.cricketlegend.engine.Scheduler
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Role
import com.mohithash.cricketlegend.model.StatKey
import org.junit.Test
import kotlin.random.Random

/** Not an assertion suite — prints a full-career balance report for tuning. */
class BalanceReportTest {

    @Test
    fun printCareer() {
        val rng = Random(2026)
        val s = GameState(playerName = "Vaibhav Jr", country = "India", role = Role.BATTER,
            age = 9, batting = 34.0, bowling = 14.0, playstyle = "Aggressive")
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        com.mohithash.cricketlegend.engine.WorldSim.seedRivals(s, rng)
        Scheduler.buildSeason(s, rng)

        repeat(33) {
            var guard = 0
            while (guard++ < 300) {
                if (s.injuryWeeksLeft > 0) {
                    val heal = s.week + s.injuryWeeksLeft
                    s.fixtures.filter { !it.played && !it.missed && it.week < heal }.forEach { f -> f.missed = true }
                    Finance.processWeeks(s, s.injuryWeeksLeft); s.week = heal.coerceAtMost(52); s.injuryWeeksLeft = 0
                }
                val fx = s.nextFixture() ?: break
                Finance.processWeeks(s, (fx.week - s.week).coerceAtLeast(0)); s.week = fx.week
                com.mohithash.cricketlegend.engine.Tournaments.startFor(s, fx, rng)
                val rep = MatchEngine.simulate(s, fx, rng)
                Progression.applyReport(s, fx, rep, rng)
                s.pendingEvent = Events.maybeEvent(s, rng)
                s.pendingEvent?.let { ev -> Events.resolve(s, ev, 0) } // always take first choice
            }
            Finance.processWeeks(s, (52 - s.week).coerceAtLeast(0))
            println("--- End of ${s.year}: age=${s.age} fame=%.0f bat=%.0f money=${Money.fmt(s.money, "India")} T20I=${s.inNationalT20} ODI=${s.inNationalODI} Test=${s.inNationalTest} team=${s.franchiseTeam} endo=${s.endorsements.size} fol=${s.followers / 1_000_000}M capt=${s.captainNation} img=%.0f".format(s.fame, s.batting, s.publicImage))
            Progression.endSeason(s, rng)
            if (s.pendingAuction != null) {
                com.mohithash.cricketlegend.engine.AuctionEngine.resolve(s, true, rng)
                com.mohithash.cricketlegend.engine.Scheduler.buildSeason(s, rng)
            }
        }
        println("=== CAREER (age ${s.age}) ===")
        for (k in StatKey.ALL) {
            val st = s.stat(k)
            if (st.matches == 0) continue
            println("${StatKey.label(k)}: M=${st.matches} R=${st.runs} avg=%.1f SR=%.1f HS=${st.highest} 100s=${st.hundreds} W=${st.wickets}".format(st.battingAverage, st.strikeRate))
        }
        println("Intl runs=${s.intlRuns} intl 100s=${s.intlHundreds} sixes=${s.intlSixes} matches=${s.intlMatches}")
        println("Records broken (${s.brokenRecords.size}): ${s.brokenRecords}")
        println("Trophies (${s.trophies.size}): ${s.trophies}")
        println("Awards (${s.awards.size}): ${s.awards.take(12)}")
        println("Family: partner=${s.partner} married=${s.married} kids=${s.kids} rel=%.0f".format(s.relationship))
        println("Businesses=${s.businesses.map { it.name }} portfolio=${Money.fmt(com.mohithash.cricketlegend.engine.Finance.portfolioValue(s), "India")}")
        println("Milestones=${s.milestonesSeen} No1=${s.reachedNo1} nemesis=${s.dismissedBy.entries.sortedByDescending { it.value }.take(3)}")
        println("ICON STATUS = ${com.mohithash.cricketlegend.engine.Legacy.iconStatus(s)} (legacy ${com.mohithash.cricketlegend.engine.LifeSystems.goatScore(s)})")
        println("LIFE MILESTONES (${s.lifeAchievements.size}): ${s.lifeAchievements}")
        println("Kids=${s.childNames} ownedFranchise=${s.ownedFranchise} titlesAsOwner=${s.ownedTitles}")
        println("Money=${Money.fmt(s.money, "India")}  endorsements=${s.endorsements.map { it.brand }}")
        Progression.retire(s)
        println("Legacy=${s.legacyScore} -> ${Progression.legacyTitle(s.legacyScore)}")
    }
}
