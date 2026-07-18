package com.mohithash.cricketlegend.model

import kotlinx.serialization.Serializable

@Serializable
enum class Role(val label: String) {
    BATTER("Batter"),
    BOWLER("Bowler"),
    ALL_ROUNDER("All-Rounder"),
    WICKET_KEEPER("WK-Batter")
}

@Serializable
enum class Format(val label: String) {
    T20("T20"),
    ODI("One-Day"),
    FIRST_CLASS("First-Class / Test")
}

@Serializable
enum class Level(val label: String) {
    DOMESTIC("Domestic"),
    FRANCHISE("Franchise"),
    INTERNATIONAL("International")
}

@Serializable
enum class Stage(val label: String) {
    GROUP("Group"),
    QUALIFIER1("Qualifier 1"),
    ELIMINATOR("Eliminator"),
    QUALIFIER2("Qualifier 2"),
    SEMI_FINAL("Semi-Final"),
    FINAL("FINAL")
}

/** Keys into GameState.stats */
object StatKey {
    const val YOUTH = "YOUTH"
    const val DOM_FC = "DOM_FC"
    const val DOM_LIST_A = "DOM_LIST_A"
    const val DOM_T20 = "DOM_T20"
    const val LEAGUE = "LEAGUE"
    const val INTL_TEST = "INTL_TEST"
    const val INTL_ODI = "INTL_ODI"
    const val INTL_T20 = "INTL_T20"

    val ALL = listOf(YOUTH, DOM_FC, DOM_LIST_A, DOM_T20, LEAGUE, INTL_TEST, INTL_ODI, INTL_T20)
    val INTL = listOf(INTL_TEST, INTL_ODI, INTL_T20)

    fun label(key: String): String = when (key) {
        YOUTH -> "Age-Group"
        DOM_FC -> "First-Class"
        DOM_LIST_A -> "List-A"
        DOM_T20 -> "Domestic T20"
        LEAGUE -> "Franchise T20"
        INTL_TEST -> "Test"
        INTL_ODI -> "ODI"
        INTL_T20 -> "T20I"
        else -> key
    }
}

@Serializable
data class FormatStats(
    var matches: Int = 0,
    var innings: Int = 0,
    var notOuts: Int = 0,
    var runs: Int = 0,
    var ballsFaced: Int = 0,
    var highest: Int = 0,
    var highestNotOut: Boolean = false,
    var fifties: Int = 0,
    var hundreds: Int = 0,
    var doubleHundreds: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var ballsBowled: Int = 0,
    var runsConceded: Int = 0,
    var wickets: Int = 0,
    var fiveWicketHauls: Int = 0,
    var bestBowlingWkts: Int = 0,
    var bestBowlingRuns: Int = 0,
    var catches: Int = 0,
    var manOfTheMatch: Int = 0
) {
    val battingAverage: Double
        get() = if (innings - notOuts > 0) runs.toDouble() / (innings - notOuts) else runs.toDouble()
    val strikeRate: Double
        get() = if (ballsFaced > 0) runs * 100.0 / ballsFaced else 0.0
    val bowlingAverage: Double
        get() = if (wickets > 0) runsConceded.toDouble() / wickets else 0.0
    val economy: Double
        get() = if (ballsBowled > 0) runsConceded * 6.0 / ballsBowled else 0.0
}

@Serializable
data class Fixture(
    val id: Int,
    val week: Int,
    val format: Format,
    val level: Level,
    val statKey: String,
    val opponent: String,
    val tournament: String? = null,
    val stage: Stage? = null,
    var played: Boolean = false,
    var missed: Boolean = false,
    var won: Boolean? = null,
    val venue: String = "",
    val pitch: String = "BALANCED"   // PACE, SPIN, GREEN, FLAT, BALANCED
)

@Serializable
data class TeamRecord(
    var played: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var points: Int = 0,
    var netRunRate: Double = 0.0
)

@Serializable
data class TournamentState(
    val name: String,
    val isLeague: Boolean,
    val playerTeam: String,
    val teams: MutableList<String> = mutableListOf(),
    val standings: MutableMap<String, TeamRecord> = mutableMapOf(),
    val runScorers: MutableMap<String, Int> = mutableMapOf(),
    val wicketTakers: MutableMap<String, Int> = mutableMapOf(),
    var playerRuns: Int = 0,
    var playerWkts: Int = 0,
    var playerRatingSum: Double = 0.0,
    var playerMatches: Int = 0,
    var completed: Boolean = false,
    var champion: String? = null
)

@Serializable
data class AuctionBid(val team: String, val amount: Long)

@Serializable
data class AuctionState(
    val year: Int,
    val isMega: Boolean,
    val basePrice: Long,
    val bids: List<AuctionBid>,
    val soldTo: String?,
    val finalPrice: Long,
    val retentionTeam: String? = null,
    val retentionOffer: Long = 0
)

@Serializable
data class Instrument(
    val id: String,
    val name: String,
    val kind: String,        // Index, Stock, Commodity, Debt, Crypto
    var price: Double,
    val drift: Double,
    val vol: Double,
    val history: MutableList<Double> = mutableListOf()
)

@Serializable
data class Holding(
    val id: String,
    var units: Double,
    var invested: Long
)

@Serializable
data class Business(
    val id: String,
    val name: String,
    val invested: Long,
    var weeklyProfit: Long,
    val startYear: Int
)

@Serializable
data class ArcState(
    val id: String,
    var step: Int = 0,
    var matchesUntilNext: Int = 0,
    var flag: Int = 0
)

@Serializable
data class RivalPlayer(
    val name: String,
    val country: String,
    val isBowler: Boolean,
    var skill: Double,
    var age: Int,
    var testRuns: Int = 0,
    var odiRuns: Int = 0,
    var t20iRuns: Int = 0,
    var testWkts: Int = 0,
    var odiWkts: Int = 0,
    var t20iWkts: Int = 0,
    var hundreds: Int = 0,
    var odiHundreds: Int = 0,
    var sixes: Int = 0,
    var matches: Int = 0,
    var leagueRuns: Int = 0,
    var retired: Boolean = false
) {
    val intlRuns: Int get() = testRuns + odiRuns + t20iRuns
    val intlWkts: Int get() = testWkts + odiWkts + t20iWkts
}

@Serializable
data class DynRecord(var holder: String, var value: Double)

@Serializable
data class SeriesState(
    val opponent: String,
    val statKey: String,
    val length: Int,
    var played: Int = 0,
    var wins: Int = 0,
    var losses: Int = 0,
    var ratingSum: Double = 0.0
)

@Serializable
data class Objective(
    val id: String,
    val label: String,
    val target: Int,
    val baseline: Int,
    val rewardMoney: Long,
    val rewardFame: Double
)

@Serializable
data class BattingLine(
    val runs: Int,
    val balls: Int,
    val fours: Int,
    val sixes: Int,
    val out: Boolean,
    val dismissal: String,
    val ballsAtHundred: Int = 0
)

@Serializable
data class BowlingLine(
    val balls: Int,
    val runsConceded: Int,
    val wickets: Int
) {
    val oversText: String get() = "${balls / 6}.${balls % 6}"
}

@Serializable
data class MatchReport(
    val fixtureId: Int,
    val title: String,
    val formatLabel: String,
    val opponent: String,
    val tournament: String?,
    val batting: List<BattingLine>,
    val bowling: List<BowlingLine>,
    val catches: Int,
    val teamScoreText: String,
    val oppScoreText: String,
    val won: Boolean,
    val resultText: String,
    val rating: Double,
    val manOfTheMatch: Boolean,
    val matchFee: Long,
    val commentary: List<String>,
    val recordsBroken: List<String> = emptyList(),
    val venue: String = "",
    val pitch: String = "BALANCED",
    val tossText: String = "",
    val headline: String? = null,
    val keyMoment: List<String> = emptyList()
)

@Serializable
data class LedgerEntry(
    val week: Int,
    val year: Int,
    val label: String,
    val amount: Long
)

@Serializable
data class EndorsementDeal(
    val brand: String,
    val category: String,
    val yearlyValue: Long,
    var yearsLeft: Int
)

@Serializable
data class Property(
    val id: String,
    val name: String,
    val city: String,
    var price: Long,
    val rentYieldPct: Double
)

@Serializable
data class OwnedProperty(
    val id: String,
    val boughtPrice: Long,
    val boughtYear: Int
)

@Serializable
data class EventEffect(
    val money: Long = 0,
    val fame: Double = 0.0,
    val form: Double = 0.0,
    val morale: Double = 0.0,
    val battingDelta: Double = 0.0,
    val bowlingDelta: Double = 0.0,
    val fitnessDelta: Double = 0.0,
    val injuryWeeks: Int = 0,
    val image: Double = 0.0,
    val followersGain: Long = 0,
    val relationshipDelta: Double = 0.0,
    val resultNews: String
)

@Serializable
data class EventChoice(
    val label: String,
    val effect: EventEffect
)

@Serializable
data class GameEvent(
    val id: String,
    val title: String,
    val description: String,
    val choices: List<EventChoice>
)

@Serializable
data class SeasonSummary(
    val year: Int,
    val headline: String
)

@Serializable
data class HofEntry(
    val name: String,
    val country: String,
    val role: String,
    val retiredYear: Int,
    val legacyScore: Int,
    val legacyTitle: String,
    val intlRuns: Int,
    val intlWickets: Int,
    val intlHundreds: Int,
    val recordsBroken: Int,
    val trophies: Int,
    val netWorth: Long
)

@Serializable
data class GameState(
    // identity
    val playerName: String = "New Player",
    val country: String = "India",
    val role: Role = Role.BATTER,
    val battingHand: String = "Right-hand bat",
    val bowlingStyle: String = "Right-arm medium",

    // time
    var age: Int = 18,
    var year: Int = 2026,
    var week: Int = 1,

    // attributes (1..99)
    var batting: Double = 50.0,
    var bowling: Double = 30.0,
    var fielding: Double = 50.0,
    var fitness: Double = 70.0,
    var vsPace: Double = 50.0,
    var vsSpin: Double = 50.0,
    var power: Double = 50.0,
    var control: Double = 50.0,   // bowling accuracy
    var form: Double = 0.0,        // -5 .. +5
    var morale: Double = 70.0,     // 0 .. 100
    var fame: Double = 5.0,        // 0 .. 100

    // economy (stored in rupees)
    var money: Long = 500_000,
    val ledger: MutableList<LedgerEntry> = mutableListOf(),
    val endorsements: MutableList<EndorsementDeal> = mutableListOf(),
    var contractGrade: String? = null,
    var contractYearly: Long = 0,
    var franchiseTeam: String? = null,
    var leagueSalary: Long = 0,

    // assets
    val ownedProperties: MutableList<OwnedProperty> = mutableListOf(),
    val propertyMarket: MutableList<Property> = mutableListOf(),
    val ownedItems: MutableList<String> = mutableListOf(),
    val staff: MutableMap<String, Int> = mutableMapOf(),
    var trainingFocus: String = "Balanced",

    // career
    val stats: MutableMap<String, FormatStats> = mutableMapOf(),
    val fixtures: MutableList<Fixture> = mutableListOf(),
    var inNationalT20: Boolean = false,
    var inNationalODI: Boolean = false,
    var inNationalTest: Boolean = false,
    var injuryWeeksLeft: Int = 0,
    val seasonRatings: MutableList<Double> = mutableListOf(),

    // achievements
    val brokenRecords: MutableList<String> = mutableListOf(),
    val trophies: MutableList<String> = mutableListOf(),
    val news: MutableList<String> = mutableListOf(),
    val history: MutableList<SeasonSummary> = mutableListOf(),

    // fame economy
    var followers: Long = 5_000,
    var publicImage: Double = 0.0,     // -50 (villain) .. +50 (national treasure)

    // leadership & rankings
    var captainNation: Boolean = false,
    var captainFranchise: Boolean = false,
    val rankPoints: MutableMap<String, Double> = mutableMapOf(),
    val reachedNo1: MutableList<String> = mutableListOf(),

    // rivalries
    val dismissedBy: MutableMap<String, Int> = mutableMapOf(),

    // tournaments & auction
    var tournament: TournamentState? = null,
    var pendingAuction: AuctionState? = null,
    var lastMegaAuctionYear: Int = 0,

    // investments & businesses
    val marketInstruments: MutableList<Instrument> = mutableListOf(),
    val holdings: MutableList<Holding> = mutableListOf(),
    val businesses: MutableList<Business> = mutableListOf(),

    // family
    var partner: String? = null,
    var married: Boolean = false,
    var kids: Int = 0,
    var relationship: Double = 0.0,    // 0..100 when partnered

    // living world
    val rivals: MutableList<RivalPlayer> = mutableListOf(),
    val dynamicRecords: MutableMap<String, DynRecord> = mutableMapOf(),

    // sim depth
    var battingPosition: Int = 3,
    val recentScores: MutableList<Int> = mutableListOf(),
    val retiredFormats: MutableList<String> = mutableListOf(),
    var playstyle: String = "Balanced",       // Aggressive, Balanced, Defensive
    var formatFocus: String = "All",           // All, WhiteBall, T20Only
    var difficulty: String = "Realistic",      // Easy, Realistic, Hardcore
    var sharpness: Double = 100.0,             // match freshness; drops with play, restored by rest
    val allocations: MutableMap<String, Long> = mutableMapOf(),  // weekly ₹ spend per category
    var nickname: String = "",
    val leagueHistory: MutableList<String> = mutableListOf(),   // "2027: Mumbai Mavericks (you: 3rd, 520 runs)"
    var derbyRival: String? = null,
    var series: SeriesState? = null,
    val seasonObjectives: MutableList<Objective> = mutableListOf(),
    var archRival: String? = null,
    var partnerType: String = "",
    var banSeasons: Int = 0,
    var ownedFranchise: String? = null,
    var ownedSquadStrength: Double = 52.0,
    var ownedTitles: Int = 0,
    var ownedLastFinish: String = "",
    val ownedSquad: MutableList<String> = mutableListOf(),
    val mentees: MutableList<String> = mutableListOf(),
    var lastPostWeek: Int = -10,
    var goatAnnounced: Boolean = false,

    // awards, arcs, milestones
    val awards: MutableList<String> = mutableListOf(),
    var activeArc: ArcState? = null,
    var overseasOfferYear: Int = 0,
    val milestonesSeen: MutableList<String> = mutableListOf(),

    // legacy / life of success
    val lifeAchievements: MutableList<String> = mutableListOf(),   // ceremonial milestones (id|year|text stored as text)
    var secondCareer: String? = null,
    var postRetireYear: Int = 0,
    val childNames: MutableList<String> = mutableListOf(),
    var generation: Int = 1,
    var inheritedLegacy: Int = 0,
    var dynastyName: String = "",
    val careerRunsBySeason: MutableList<Int> = mutableListOf(),
    val legacyBySeason: MutableList<Int> = mutableListOf(),

    // flow
    var pendingEvent: GameEvent? = null,
    var lastReport: MatchReport? = null,
    var seasonOver: Boolean = false,
    var retired: Boolean = false,
    var legacyScore: Int = 0
) {
    fun stat(key: String): FormatStats = stats.getOrPut(key) { FormatStats() }

    val intlRuns: Int get() = StatKey.INTL.sumOf { stat(it).runs }
    val intlHundreds: Int get() = StatKey.INTL.sumOf { stat(it).hundreds }
    val intlWickets: Int get() = StatKey.INTL.sumOf { stat(it).wickets }
    val intlSixes: Int get() = StatKey.INTL.sumOf { stat(it).sixes }
    val intlMatches: Int get() = StatKey.INTL.sumOf { stat(it).matches }

    fun nextFixture(): Fixture? = fixtures.filter { !it.played && !it.missed }
        .minWithOrNull(compareBy({ it.week }, { it.id }))

    fun addNews(line: String) {
        news.add(0, "[$year W$week] $line")
        while (news.size > 40) news.removeAt(news.size - 1)
    }

    fun addLedger(label: String, amount: Long) {
        money += amount
        ledger.add(0, LedgerEntry(week, year, label, amount))
        while (ledger.size > 120) ledger.removeAt(ledger.size - 1)
    }
}
