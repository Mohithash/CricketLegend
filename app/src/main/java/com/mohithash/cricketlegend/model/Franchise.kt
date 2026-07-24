package com.mohithash.cricketlegend.model

import kotlinx.serialization.Serializable

@Serializable
data class SquadPlayer(
    val name: String,
    val role: String,      // BAT, BOWL, AR, WK
    var rating: Int,       // 1..99
    var age: Int,
    var salary: Long,      // per season
    var form: Int = 55,    // 0..100
    val overseas: Boolean = false,
    val isManager: Boolean = false,   // YOU — the player-owner
    var seasonRuns: Int = 0,          // current-season tally (accrued match by match)
    var seasonWkts: Int = 0,
    var careerRuns: Int = 0,          // accrued from every simulated match, forever
    var careerWkts: Int = 0,
    var careerMatches: Int = 0
)

@Serializable
data class FrFixture(
    val opponent: String,
    val stage: String = "LEAGUE",     // LEAGUE, SEMI, FINAL
    var played: Boolean = false,
    var won: Boolean = false,
    var summary: String = ""
)

@Serializable
data class FacilityLevels(
    var stadium: Int = 1,   // gate revenue + capacity
    var academy: Int = 1,   // produces young talent each year
    var training: Int = 1,  // squad rating growth
    var medical: Int = 1,   // fewer injuries, better recovery
    var marketing: Int = 1  // sponsors + fan happiness
)

@Serializable
data class FranchiseGame(
    val teamName: String,
    val city: String,
    val colorHex: Long,
    var year: Int = 2026,
    var cash: Long = -350_000_000,          // starting in the red
    var debt: Long = 900_000_000,           // loan owed to the board/bank
    var fanHappiness: Double = 35.0,        // 0..100
    var boardConfidence: Double = 45.0,     // 0..100 — hit 0 and you're sacked
    val squad: MutableList<SquadPlayer> = mutableListOf(),
    val facilities: FacilityLevels = FacilityLevels(),
    var seasonsRun: Int = 0,
    var lastFinish: String = "",
    var titles: Int = 0,
    var bestFinish: Int = 10,
    val auctionPool: MutableList<SquadPlayer> = mutableListOf(),
    var purse: Long = 0,                    // auction budget for the current window
    val standings: MutableMap<String, Int> = mutableMapOf(),
    val news: MutableList<String> = mutableListOf(),
    var phase: String = "AUCTION",          // AUCTION -> SEASON (match-by-match) -> back to AUCTION
    // ── unified living world: every rival club has a persistent, evolving squad ──
    val rivalSquads: MutableMap<String, MutableList<SquadPlayer>> = mutableMapOf(),
    val fixtures: MutableList<FrFixture> = mutableListOf(),
    var lastScorecard: FullScorecard? = null,
    var lastMatchTitle: String = "",
    var boardTarget: String = "",
    var lastSeasonReport: String = "",
    var sacked: Boolean = false,
    var bankrupt: Boolean = false,
    var won: Boolean = false,                // escaped debt + won a title
    // ── player-manager: you also turn out for the club ──
    var playAsPlayer: Boolean = true,
    var myName: String = "You",
    var myRole: String = "AR",
    var myRuns: Int = 0,
    var myWkts: Int = 0,
    var myMatches: Int = 0,
    var my100s: Int = 0,
    var my50s: Int = 0,
    var myHighScore: Int = 0,
    var myBestBowlW: Int = 0,
    var mySeasonLine: String = "",
    val runsBySeason: MutableList<Int> = mutableListOf()
) {
    val myAvg: Double get() {
        val dismissals = (myMatches * 0.7).toInt().coerceAtLeast(1)
        return if (myMatches > 0) myRuns.toDouble() / dismissals else 0.0
    }
    fun addNews(line: String) {
        news.add(0, "[$year] $line")
        while (news.size > 40) news.removeAt(news.size - 1)
    }

    /** Best-XI average rating — the squad's competitive strength. */
    fun squadStrength(): Int {
        if (squad.isEmpty()) return 30
        val xi = squad.sortedByDescending { it.rating }.take(11)
        return (xi.sumOf { it.rating } / xi.size)
    }

    val salaryBill: Long get() = squad.sumOf { it.salary }
    val netWorth: Long get() = cash - debt
}
