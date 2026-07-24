package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.FranchiseGame
import com.mohithash.cricketlegend.model.FrFixture
import com.mohithash.cricketlegend.model.SquadPlayer
import kotlin.math.roundToLong
import kotlin.random.Random

/**
 * Franchise-management simulation on the unified living world: every club has a
 * persistent, evolving squad of named players, and every fixture is a real
 * TeamMatchSim between XIs — stats accrue from actual matches, skills decisive.
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
        val salary = ((rating - 40).coerceAtLeast(1).toDouble().let { it * it } * 90_000L).roundToLong()
        return SquadPlayer(name, role, rating, age, salary, 45 + rng.nextInt(25), overseas)
    }

    /** Creates the takeover: broke club, weak squad, and REAL rival squads for all 9 others. */
    fun newRuinedFranchise(teamName: String, rng: Random,
                           myName: String = "You", myRole: String = "AR", playAsPlayer: Boolean = true): FranchiseGame {
        val fr = RealData.franchise(teamName) ?: RealData.franchises.random(rng)
        val g = FranchiseGame(
            teamName = fr.name, city = fr.city, colorHex = fr.colorHex,
            cash = -50 * CR, debt = 150 * CR, fanHappiness = 32.0, boardConfidence = 45.0,
            playAsPlayer = playAsPlayer, myName = myName.ifBlank { "You" }, myRole = myRole
        )
        if (playAsPlayer) {
            g.squad.add(SquadPlayer("★ ${g.myName}", myRole, 52, 22, 5_000_000, 55, false, isManager = true))
        }
        repeat(if (playAsPlayer) 12 else 13) { g.squad.add(genPlayer(rng, 38, 58, overseas = it >= 11)) }

        // the living world: rival clubs get persistent squads scaled to their strength
        RealData.franchises.filter { it.name != g.teamName }.forEach { rival ->
            val base = rival.strength   // 48..58
            val squad = MutableList(15) { i ->
                genPlayer(rng, base + 2, base + 28, overseas = i >= 11)
            }
            g.rivalSquads[rival.name] = squad
        }

        g.boardTarget = "Survive: avoid finishing last and don't go bankrupt."
        g.addNews("TAKEOVER: You inherit ${fr.name} — ${Money.fmt(g.debt, "India")} in debt and a bottom-of-the-table squad.")
        g.addNews("The board gives you a season to show progress. Fans are furious.")
        openAuction(g, rng)
        return g
    }

    // ---- Auction ----

    fun openAuction(g: FranchiseGame, rng: Random) {
        g.auctionPool.clear()
        g.purse = (60 * CR + (g.netWorth.coerceAtLeast(-200 * CR) / 4)).coerceIn(20 * CR, 120 * CR)
        repeat(6) { g.auctionPool.add(genPlayer(rng, 78, 92, overseas = rng.nextDouble() < 0.5)) }
        repeat(10) { g.auctionPool.add(genPlayer(rng, 62, 78, overseas = rng.nextDouble() < 0.35)) }
        repeat(12) { g.auctionPool.add(genPlayer(rng, 45, 62)) }
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
        g.purse -= price
        g.squad.add(p)
        g.auctionPool.removeAt(index)
        g.addNews("SIGNED: ${p.name} (${p.role} · ${p.rating}) for ${Money.fmt(price, "India")}.")
        return "Signed ${p.name}!"
    }

    fun releasePlayer(g: FranchiseGame, name: String): String {
        val p = g.squad.firstOrNull { it.name == name } ?: return "Not in squad."
        if (p.isManager) return "You can't release yourself!"
        if (g.squad.size <= 11) return "Can't go below 11 players."
        g.squad.remove(p)
        g.cash += (p.salary * 0.6).roundToLong()
        g.addNews("Released ${p.name}, freeing ${Money.fmt(p.salary, "India")}/season in wages.")
        return "Released ${p.name}."
    }

    // ---- Facilities & debt ----

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

    fun upgradeCost(level: Int): Long = (level * level * 8L * CR)

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

    // ---- Match-by-match season on the living world ----

    /** Locks the squad and builds the 14-game fixture list. */
    fun startSeason(g: FranchiseGame, rng: Random) {
        g.fixtures.clear()
        val others = g.rivalSquads.keys.shuffled(rng)
        val slate = (others + others.shuffled(rng).take(5)).take(14)
        slate.forEach { g.fixtures.add(FrFixture(it)) }
        g.standings.clear()
        (g.rivalSquads.keys + g.teamName).forEach { g.standings[it] = 0 }
        // fresh season tallies for every player in the world
        (g.squad + g.rivalSquads.values.flatten()).forEach { it.seasonRuns = 0; it.seasonWkts = 0 }
        g.phase = "SEASON"
        g.addNews("Season ${g.year} begins — 14 league games. Every run is real now.")
    }

    /** Plays YOUR next fixture as a full 11v11 sim, plus a round of rival matches. */
    fun playNextFixture(g: FranchiseGame, rng: Random): Boolean {
        if (g.phase != "SEASON") return false
        val fx = g.fixtures.firstOrNull { !it.played } ?: return false
        val oppSquad = g.rivalSquads[fx.opponent] ?: return false

        val homeFirst = rng.nextBoolean()
        val res = if (homeFirst)
            TeamMatchSim.play(g.teamName, g.squad, fx.opponent, oppSquad, rng)
        else TeamMatchSim.play(fx.opponent, oppSquad, g.teamName, g.squad, rng)
        val won = if (homeFirst) res.teamAWon else !res.teamAWon

        fx.played = true; fx.won = won
        g.lastScorecard = res.card
        g.lastMatchTitle = "${fx.stage.lowercase().replaceFirstChar { it.uppercase() }}: " +
            "${res.card.playerTeam} ${res.card.first.total}/${res.card.first.wickets} v " +
            "${res.card.opponent} ${res.card.second.total}/${res.card.second.wickets} — " +
            (if (won) "WON" else "LOST") + " · MoM ${res.motm}"
        fx.summary = g.lastMatchTitle
        if (fx.stage == "LEAGUE" && won) g.standings[g.teamName] = (g.standings[g.teamName] ?: 0) + 1

        // fans and board live on results
        g.fanHappiness = (g.fanHappiness + if (won) 2.5 else -2.0).coerceIn(0.0, 100.0)
        g.boardConfidence = (g.boardConfidence + if (won) 1.5 else -1.2).coerceIn(0.0, 100.0)
        g.addNews((if (won) "WIN" else "LOSS") + " vs ${fx.opponent}. MoM: ${res.motm}.")

        // a round of rival fixtures — the rest of the league plays real matches too
        if (fx.stage == "LEAGUE") {
            val others = g.rivalSquads.keys.filter { it != fx.opponent }.shuffled(rng)
            var i = 0
            while (i + 1 < others.size) {
                val a = others[i]; val b = others[i + 1]
                val r = TeamMatchSim.play(a, g.rivalSquads[a]!!, b, g.rivalSquads[b]!!, rng)
                val w = if (r.teamAWon) a else b
                g.standings[w] = (g.standings[w] ?: 0) + 1
                i += 2
            }
        }

        // knockout ladder
        when (fx.stage) {
            "SEMI" -> if (won) g.fixtures.add(FrFixture(finalOpponent(g, rng), "FINAL")) else return finishSeason(g, rng).let { true }
            "FINAL" -> return finishSeason(g, rng, wonFinal = won).let { true }
        }

        if (fx.stage == "LEAGUE" && g.fixtures.none { !it.played }) {
            val order = g.standings.entries.sortedByDescending { it.value }.map { it.key }
            val pos = order.indexOf(g.teamName) + 1
            if (pos <= 4) {
                g.fixtures.add(FrFixture(order[if (pos == 1) 3 else if (pos == 4) 0 else (4 - pos)], "SEMI"))
                g.addNews("PLAYOFFS! Finished #$pos — semi-final awaits.")
            } else {
                finishSeason(g, rng)
            }
        }
        return true
    }

    private fun finalOpponent(g: FranchiseGame, rng: Random): String =
        g.standings.entries.sortedByDescending { it.value }.map { it.key }
            .filter { it != g.teamName }.take(3).random(rng)

    /** Convenience: fast-forward the remaining fixtures (auto-plays each one for real). */
    fun simulateSeason(g: FranchiseGame, rng: Random) {
        if (g.phase == "AUCTION") startSeason(g, rng)
        var guard = 0
        while (g.phase == "SEASON" && guard++ < 40) {
            if (!playNextFixture(g, rng)) break
        }
    }

    /** End-of-season: finances, development, world evolution, verdicts. */
    fun finishSeason(g: FranchiseGame, rng: Random, wonFinal: Boolean = false) {
        val wins = g.standings[g.teamName] ?: 0
        val order = g.standings.entries.sortedByDescending { it.value }.map { it.key }
        val pos = order.indexOf(g.teamName) + 1
        val madePlayoffs = g.fixtures.any { it.stage != "LEAGUE" }
        g.lastFinish = when {
            wonFinal -> "CHAMPIONS"
            madePlayoffs -> "Playoffs (#$pos)"
            else -> "#$pos of 10"
        }
        if (pos < g.bestFinish) g.bestFinish = pos
        if (wonFinal) g.titles++

        // manager's personal season (accrued from the real sims)
        g.squad.firstOrNull { it.isManager }?.let { me ->
            val games = g.fixtures.count { it.played }
            g.myMatches += games; g.myRuns += me.seasonRuns; g.myWkts += me.seasonWkts
            g.my50s += me.seasonRuns / 160
            if (me.seasonRuns > 450) g.my100s += rng.nextInt(2)
            val hs = (me.seasonRuns / games.coerceAtLeast(1) * (1.5 + rng.nextDouble())).toInt()
            if (hs > g.myHighScore) g.myHighScore = hs
            if (me.seasonWkts / 3 > g.myBestBowlW) g.myBestBowlW = (me.seasonWkts / 3).coerceAtMost(6)
            g.runsBySeason.add(me.seasonRuns)
            while (g.runsBySeason.size > 30) g.runsBySeason.removeAt(0)
            g.mySeasonLine = "You: ${me.seasonRuns} runs" +
                (if (me.seasonWkts > 0) ", ${me.seasonWkts} wkts" else "") + " (rating ${me.rating})"
        }
        val topBat = (g.squad + g.rivalSquads.values.flatten()).maxByOrNull { it.seasonRuns }
        if (topBat != null) g.addNews("ORANGE CAP: ${topBat.name} — ${topBat.seasonRuns} runs.")

        // ---- finances ----
        val gate = (g.facilities.stadium * 6L * CR * (g.fanHappiness / 100.0) * 7).roundToLong()
        val sponsors = (g.facilities.marketing * 5L * CR * (0.5 + g.fanHappiness / 200.0)).roundToLong()
        val prize = when { wonFinal -> 50L * CR; madePlayoffs -> 20L * CR; else -> 5L * CR }
        val income = gate + sponsors + 60L * CR + prize
        val upkeep = facilities.sumOf { facilityLevel(g, it.id).toLong() } * 2L * CR
        val interest = (g.debt * 0.08).roundToLong()
        val net = income - (g.salaryBill + upkeep + interest)
        g.cash += net

        val fanDelta = (wins - 6) * 3.0 + (if (wonFinal) 20 else if (madePlayoffs) 8 else -6)
        g.fanHappiness = (g.fanHappiness + fanDelta).coerceIn(0.0, 100.0)
        val boardDelta = ((6 - pos) * 4.0) + (if (net > 0) 6 else -6) + (if (g.debt < 50 * CR) 5 else 0)
        g.boardConfidence = (g.boardConfidence + boardDelta).coerceIn(0.0, 100.0)

        // ---- development: your squad AND every rival squad evolves ----
        fun develop(list: MutableList<SquadPlayer>, trainingLvl: Int) {
            for (p in list) {
                val growth = if (p.age <= 26) (trainingLvl * 0.7 + p.seasonRuns / 250.0 + p.seasonWkts / 12.0)
                    else -(0.3 + (p.age - 30).coerceAtLeast(0) * 0.2)
                p.rating = kotlin.math.round(p.rating + growth).toInt().coerceIn(30, 99)
                p.age += 1
                p.form = (45 + rng.nextInt(30)).coerceAtMost(100)
            }
        }
        develop(g.squad, g.facilities.training)
        g.rivalSquads.values.forEach { develop(it, 2) }
        // rival clubs run their own mini-auction: drop the weakest, sign fresh talent
        g.rivalSquads.forEach { (team, squad) ->
            squad.minByOrNull { it.rating }?.let { squad.remove(it) }
            squad.add(genPlayer(rng, 60, 88, overseas = rng.nextDouble() < 0.3))
            if (rng.nextDouble() < 0.2) g.addNews("$team make a marquee signing in the off-season.")
        }

        if (g.facilities.academy >= 1 && rng.nextDouble() < 0.4 + g.facilities.academy * 0.1) {
            val prospect = genPlayer(rng, 55 + g.facilities.academy * 2, 68 + g.facilities.academy * 3)
                .let { it.copy(salary = it.salary / 3) }
            if (g.squad.size < 25) {
                g.squad.add(prospect)
                g.addNews("ACADEMY GRADUATE: young ${prospect.name} (${prospect.rating}) promoted!")
            }
        }

        g.seasonsRun++; g.year++
        g.lastSeasonReport = "Season ${g.year - 1}: ${g.lastFinish}, $wins league wins. " +
            "Net ${if (net >= 0) "+" else ""}${Money.fmt(net, "India")}."
        g.addNews("SEASON END — ${g.lastFinish}. Books: ${if (net >= 0) "profit" else "loss"} of ${Money.fmt(net, "India")}.")
        if (wonFinal) g.addNews("🏆 CHAMPIONS! ${g.teamName} lift the trophy!")

        g.boardTarget = when {
            g.debt <= 0 && g.titles > 0 -> "Legacy: dominate — win another title, debt-free."
            g.debt <= 0 -> "Debt cleared! Now bring home silverware."
            g.debt < 60 * CR -> "Push for the playoffs and finish clearing the debt."
            else -> "Reduce the debt and climb the table."
        }
        if (g.debt <= 0 && g.titles > 0) { g.won = true; g.addNews("MISSION COMPLETE: debt-free AND champions!") }
        if (g.cash < -200 * CR) { g.bankrupt = true; g.addNews("BANKRUPT: the club has run out of money.") }
        if (g.boardConfidence <= 0) { g.sacked = true; g.addNews("SACKED: the board has lost patience.") }

        openAuction(g, rng)
    }

    fun squadByRole(g: FranchiseGame): List<SquadPlayer> =
        g.squad.sortedWith(compareByDescending<SquadPlayer> { it.rating })

    /** League-wide leaders across all 10 real squads — the Orange/Purple cap race, for real. */
    fun leagueTopBats(g: FranchiseGame, n: Int): List<SquadPlayer> =
        (g.squad + g.rivalSquads.values.flatten()).sortedByDescending { it.seasonRuns }.take(n)

    fun leagueTopBowls(g: FranchiseGame, n: Int): List<SquadPlayer> =
        (g.squad + g.rivalSquads.values.flatten()).sortedByDescending { it.seasonWkts }.take(n)
}
