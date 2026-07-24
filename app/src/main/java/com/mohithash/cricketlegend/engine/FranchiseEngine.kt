package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.FacilityLevels
import com.mohithash.cricketlegend.model.FranchiseGame
import com.mohithash.cricketlegend.model.SquadPlayer
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Franchise-management simulation: take a debt-ridden T20 club with a weak squad
 * and rebuild it through auctions, facility upgrades and shrewd finances.
 * All money in rupees (Money.fmt shows Cr). Deliberately gameplay-balanced, not
 * a literal reproduction of real franchise economics.
 */
object FranchiseEngine {

    private val CR = 10_000_000L

    private val firstNames = listOf("Arjun","Rohan","Aditya","Karan","Dev","Ishaan","Vihaan","Aryan","Rudra",
        "Kabir","Advait","Reyansh","Yuvan","Sai","Manav","Neel","Om","Parth","Tejas","Vivaan","Jai","Ansh")
    private val lastNames = listOf("Sharma","Patel","Nair","Iyer","Singh","Reddy","Menon","Kumar","Rao","Gowda",
        "Pillai","Desai","Joshi","Shetty","Verma","Yadav","Chauhan","Bose","Naidu","Kohli")
    private val overseasNames = listOf("Jake Miller","Tom Baxter","Liam Cross","Kyle Fisher","Ryan Poole",
        "Dane Vetter","Cody Grant","Wade Nix","Brett Ollie","Sam Rourke","Curtis Vale","Josh Trent")
    private val roles = listOf("BAT", "BOWL", "AR", "WK")

    private fun genPlayer(rng: Random, minR: Int, maxR: Int, overseas: Boolean = false): SquadPlayer {
        val rating = (minR..maxR).random(rng)
        val role = if (rng.nextDouble() < 0.15) "WK" else roles[rng.nextInt(3)]
        val name = if (overseas) overseasNames.random(rng)
            else "${firstNames.random(rng)} ${lastNames.random(rng)}"
        val age = 18 + rng.nextInt(20)
        // salary scales steeply with rating (stars are expensive)
        val salary = ((rating - 40).coerceAtLeast(1).toDouble().let { it * it } * 90_000L).roundToLong()
        return SquadPlayer(name, role, rating, age, salary, 45 + rng.nextInt(25), overseas)
    }

    /** Creates the takeover scenario: a broke club, weak squad, a demanding board. */
    fun newRuinedFranchise(teamName: String, rng: Random): FranchiseGame {
        val fr = RealData.franchise(teamName) ?: RealData.franchises.random(rng)
        val g = FranchiseGame(
            teamName = fr.name, city = fr.city, colorHex = fr.colorHex,
            cash = -50 * CR, debt = 150 * CR, fanHappiness = 32.0, boardConfidence = 45.0
        )
        // a threadbare squad of journeymen and kids
        repeat(13) { g.squad.add(genPlayer(rng, 38, 58, overseas = it >= 11)) }
        g.boardTarget = "Survive: avoid finishing last and don't go bankrupt."
        g.addNews("TAKEOVER: You inherit ${fr.name} — ${Money.fmt(g.debt, "India")} in debt and a bottom-of-the-table squad.")
        g.addNews("The board gives you a season to show progress. Fans are furious.")
        openAuction(g, rng)
        return g
    }

    /** Refills the auction pool and sets the purse for a new window. */
    fun openAuction(g: FranchiseGame, rng: Random) {
        g.auctionPool.clear()
        // purse is tight while in debt; grows as finances recover
        g.purse = (60 * CR + (g.netWorth.coerceAtLeast(-200 * CR) / 4)).coerceIn(20 * CR, 120 * CR)
        repeat(6) { g.auctionPool.add(genPlayer(rng, 78, 92, overseas = rng.nextDouble() < 0.5)) } // marquee
        repeat(10) { g.auctionPool.add(genPlayer(rng, 62, 78, overseas = rng.nextDouble() < 0.35)) } // solid
        repeat(12) { g.auctionPool.add(genPlayer(rng, 45, 62)) } // squad filler
        g.phase = "AUCTION"
    }

    fun playerPrice(p: SquadPlayer): Long = (p.salary * 2.4).roundToLong()

    fun signPlayer(g: FranchiseGame, index: Int): String {
        val p = g.auctionPool.getOrNull(index) ?: return "No such player."
        val price = playerPrice(p)
        if (g.purse < price) return "Not enough purse (${Money.fmt(g.purse, "India")} left)."
        if (g.squad.size >= 25) return "Squad is full (25 max). Release someone first."
        val overseasCount = g.squad.count { it.overseas }
        if (p.overseas && overseasCount >= 8) return "Overseas quota full (8 max)."
        // the purse is a board-allocated auction allowance — only ongoing salary hits club cash
        g.purse -= price
        g.squad.add(p)
        g.auctionPool.removeAt(index)
        g.addNews("SIGNED: ${p.name} (${p.role} · ${p.rating}) for ${Money.fmt(price, "India")}.")
        return "Signed ${p.name}!"
    }

    fun releasePlayer(g: FranchiseGame, name: String): String {
        val p = g.squad.firstOrNull { it.name == name } ?: return "Not in squad."
        if (g.squad.size <= 11) return "Can't go below 11 players."
        g.squad.remove(p)
        // recoup a little of the salary as a transfer fee
        g.cash += (p.salary * 0.6).roundToLong()
        g.addNews("Released ${p.name}, freeing ${Money.fmt(p.salary, "India")}/season in wages.")
        return "Released ${p.name}."
    }

    // ---- Facility upgrades ----

    data class Facility(val id: String, val label: String, val blurb: String, val emoji: String)
    val facilities = listOf(
        Facility("stadium", "Stadium", "Bigger crowds = more gate revenue every home game", "🏟️"),
        Facility("marketing", "Marketing", "Sponsors and fan happiness — the money and the mood", "📣"),
        Facility("training", "Training Centre", "Your squad improves faster between seasons", "🏋️"),
        Facility("academy", "Youth Academy", "Produces a home-grown prospect each year for free", "🎓"),
        Facility("medical", "Medical Wing", "Fewer injuries, players hold form through the season", "🏥")
    )

    fun facilityLevel(g: FranchiseGame, id: String): Int = when (id) {
        "stadium" -> g.facilities.stadium; "marketing" -> g.facilities.marketing
        "training" -> g.facilities.training; "academy" -> g.facilities.academy
        else -> g.facilities.medical
    }

    fun upgradeCost(level: Int): Long = (level * level * 8L * CR)  // 8, 32, 72, 128 Cr...

    fun upgrade(g: FranchiseGame, id: String): String {
        val lvl = facilityLevel(g, id)
        if (lvl >= 8) return "Already maxed."
        val cost = upgradeCost(lvl)
        if (g.cash < cost) return "Need ${Money.fmt(cost, "India")} in cash."
        g.cash -= cost
        when (id) {
            "stadium" -> g.facilities.stadium++
            "marketing" -> { g.facilities.marketing++; g.fanHappiness = (g.fanHappiness + 5).coerceAtMost(100.0) }
            "training" -> g.facilities.training++
            "academy" -> g.facilities.academy++
            "medical" -> g.facilities.medical++
        }
        g.addNews("Upgraded ${facilities.first { it.id == id }.label} to level ${lvl + 1}.")
        return "Upgraded to level ${lvl + 1}."
    }

    fun payDebt(g: FranchiseGame, amount: Long): String {
        val pay = amount.coerceAtMost(minOf(g.cash, g.debt)).coerceAtLeast(0)
        if (pay <= 0) return "No cash to pay debt."
        g.cash -= pay; g.debt -= pay
        g.addNews("Repaid ${Money.fmt(pay, "India")} of debt. Owed: ${Money.fmt(g.debt, "India")}.")
        return "Repaid ${Money.fmt(pay, "India")}."
    }

    // ---- Season simulation ----

    fun simulateSeason(g: FranchiseGame, rng: Random) {
        val myStrength = (g.squadStrength() + g.facilities.training * 1.2 +
            (g.fanHappiness - 50) / 25.0).coerceIn(30.0, 99.0)

        // 14-game league; win prob from strength vs a ~55-rated field
        var wins = 0
        repeat(14) {
            val oppStr = 48 + rng.nextInt(18)
            val p = (0.5 + (myStrength - oppStr) / 60.0).coerceIn(0.08, 0.92)
            if (rng.nextDouble() < p) wins++
        }
        // rival results, ranked
        val table = RealData.franchises.filter { it.name != g.teamName }
            .associate { it.name to (2 + rng.nextInt(12)) }.toMutableMap()
        table[g.teamName] = wins
        g.standings.clear(); g.standings.putAll(table)
        val order = table.entries.sortedByDescending { it.value }.map { it.key }
        val pos = order.indexOf(g.teamName) + 1

        val madePlayoffs = pos <= 4
        val wonTitle = madePlayoffs && rng.nextDouble() < ((myStrength - 55) / 70.0).coerceIn(0.08, 0.8)
        g.lastFinish = when {
            wonTitle -> "CHAMPIONS"
            madePlayoffs -> "Playoffs (#$pos)"
            else -> "#$pos of 10"
        }
        if (pos < g.bestFinish) g.bestFinish = pos
        if (wonTitle) g.titles++

        // ---- finances ----
        val homeGames = 7
        val gate = (g.facilities.stadium * 6L * CR * (g.fanHappiness / 100.0) * homeGames).roundToLong()
        val sponsors = (g.facilities.marketing * 5L * CR * (0.5 + g.fanHappiness / 200.0)).roundToLong()
        val media = 60L * CR
        val prize = when { wonTitle -> 50L * CR; madePlayoffs -> 20L * CR; else -> 5L * CR }
        val income = gate + sponsors + media + prize

        val upkeep = facilities.sumOf { facilityLevel(g, it.id).toLong() } * 2L * CR
        val interest = (g.debt * 0.08).roundToLong()
        val expense = g.salaryBill + upkeep + interest
        val net = income - expense
        g.cash += net
        g.debt += interest // interest capitalises onto the loan too if unpaid? no — already charged as expense. keep simple: interest is an expense only.
        g.debt -= interest // cancel — interest handled as expense, don't grow principal

        // fan + board reaction
        val fanDelta = (wins - 6) * 3.0 + (if (wonTitle) 20 else if (madePlayoffs) 8 else -6)
        g.fanHappiness = (g.fanHappiness + fanDelta).coerceIn(0.0, 100.0)
        val boardDelta = (pos.let { 6 - it } * 4.0) + (if (net > 0) 6 else -6) + (if (g.debt < 50 * CR) 5 else 0)
        g.boardConfidence = (g.boardConfidence + boardDelta).coerceIn(0.0, 100.0)

        // squad development (training) + aging + academy
        for (p in g.squad) {
            val growth = if (p.age <= 24) (g.facilities.training * 0.4) else -(0.3 + (p.age - 30).coerceAtLeast(0) * 0.2)
            p.rating = (p.rating + growth).toInt().coerceIn(30, 99)
            p.form = (45 + rng.nextInt(30) + g.facilities.medical * 2).coerceAtMost(100)
        }
        if (g.facilities.academy >= 1 && rng.nextDouble() < 0.4 + g.facilities.academy * 0.1) {
            val prospect = genPlayer(rng, 55 + g.facilities.academy * 2, 68 + g.facilities.academy * 3)
                .let { it.copy(salary = it.salary / 3) }
            if (g.squad.size < 25) {
                g.squad.add(prospect)
                g.addNews("ACADEMY GRADUATE: young ${prospect.name} (${prospect.rating}) promoted to the senior squad!")
            } else {
                // squad full — the graduate replaces the weakest if better
                val weakest = g.squad.minByOrNull { it.rating }
                if (weakest != null && prospect.rating > weakest.rating) {
                    g.squad.remove(weakest); g.squad.add(prospect)
                    g.addNews("ACADEMY GRADUATE: ${prospect.name} (${prospect.rating}) forces out ${weakest.name}.")
                }
            }
        }

        g.seasonsRun++
        g.year++
        g.lastSeasonReport = "Season ${g.year - 1}: ${g.lastFinish}, $wins wins. " +
            "Net ${if (net >= 0) "+" else ""}${Money.fmt(net, "India")}."
        g.addNews("SEASON END — ${g.lastFinish}, $wins/14 wins. Books: ${if (net >= 0) "profit" else "loss"} of ${Money.fmt(net, "India")}.")
        if (wonTitle) g.addNews("🏆 CHAMPIONS! ${g.teamName} lift the trophy under your management!")

        // board targets for next year
        g.boardTarget = when {
            g.debt <= 0 && g.titles > 0 -> "Legacy: dominate — win another title, debt-free."
            g.debt <= 0 -> "Debt cleared! Now bring home silverware."
            g.debt < 60 * CR -> "Push for the playoffs and finish clearing the debt."
            else -> "Reduce the debt and climb the table."
        }

        // win / lose conditions
        if (g.debt <= 0 && g.titles > 0) { g.won = true; g.addNews("MISSION COMPLETE: debt-free AND champions. A turnaround for the ages!") }
        if (g.cash < -200 * CR) { g.bankrupt = true; g.addNews("BANKRUPT: the club has run out of money. Game over.") }
        if (g.boardConfidence <= 0) { g.sacked = true; g.addNews("SACKED: the board has lost all confidence and relieved you of duties.") }

        openAuction(g, rng)
    }

    fun squadByRole(g: FranchiseGame): List<SquadPlayer> =
        g.squad.sortedWith(compareByDescending<SquadPlayer> { it.rating })
}
