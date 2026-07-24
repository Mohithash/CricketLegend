package com.mohithash.cricketlegend

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Events
import com.mohithash.cricketlegend.engine.Finance
import com.mohithash.cricketlegend.engine.MatchEngine
import com.mohithash.cricketlegend.engine.Progression
import com.mohithash.cricketlegend.engine.Scheduler
import com.mohithash.cricketlegend.engine.AuctionEngine
import com.mohithash.cricketlegend.engine.LifeSystems
import com.mohithash.cricketlegend.engine.Tournaments
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.HofEntry
import com.mohithash.cricketlegend.model.Property
import com.mohithash.cricketlegend.model.Role
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Global game controller. UI reads [version] to recompose after every mutation
 * (GameState is mutated in place; bumping version invalidates readers).
 */
object Game {
    var version by mutableIntStateOf(0)
        private set
    var selectedTab by mutableIntStateOf(0)
    var almanacOpen by androidx.compose.runtime.mutableStateOf(false)

    var state: GameState? = null
        private set

    private var saveFile: File? = null
    private var hofFile: File? = null
    private var frFile: File? = null
    private var hofRecorded = false
    var hallOfFame: List<HofEntry> = emptyList()
        private set

    /** Franchise-manager mode state (independent of the player career). */
    var franchise: com.mohithash.cricketlegend.model.FranchiseGame? = null
        private set

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    fun init(context: Context) {
        if (saveFile != null) return
        val f = File(context.filesDir, "save.json")
        saveFile = f
        hofFile = File(context.filesDir, "hof.json")
        frFile = File(context.filesDir, "franchise.json")
        hofFile?.takeIf { it.exists() }?.let { hf ->
            runCatching { hallOfFame = json.decodeFromString<List<HofEntry>>(hf.readText()) }
        }
        frFile?.takeIf { it.exists() }?.let { ff ->
            runCatching { franchise = json.decodeFromString<com.mohithash.cricketlegend.model.FranchiseGame>(ff.readText()) }
                .onFailure { ff.delete() }
        }
        if (f.exists()) {
            runCatching { state = json.decodeFromString<GameState>(f.readText()) }
                .onFailure { f.delete() }
            hofRecorded = state?.retired == true
        }
        version++
    }

    private fun persistFranchise() {
        val g = franchise ?: return
        frFile?.writeText(json.encodeToString(g))
    }

    private inline fun mutateFr(block: (com.mohithash.cricketlegend.model.FranchiseGame) -> Unit) {
        val g = franchise ?: return
        block(g); version++; persistFranchise()
    }

    fun newFranchiseGame(teamName: String, myName: String = "You", myRole: String = "AR", playAsPlayer: Boolean = true) {
        franchise = com.mohithash.cricketlegend.engine.FranchiseEngine.newRuinedFranchise(
            teamName, kotlin.random.Random.Default, myName, myRole, playAsPlayer)
        version++; persistFranchise()
    }

    fun abandonFranchise() { franchise = null; frFile?.delete(); version++ }

    fun frSign(index: Int) = mutateFr { toast = com.mohithash.cricketlegend.engine.FranchiseEngine.signPlayer(it, index) }
    fun frRelease(name: String) = mutateFr { toast = com.mohithash.cricketlegend.engine.FranchiseEngine.releasePlayer(it, name) }
    fun frUpgrade(id: String) = mutateFr { toast = com.mohithash.cricketlegend.engine.FranchiseEngine.upgrade(it, id) }
    fun frPayDebt(amount: Long) = mutateFr { toast = com.mohithash.cricketlegend.engine.FranchiseEngine.payDebt(it, amount) }
    fun frStartSeason() = mutateFr { com.mohithash.cricketlegend.engine.FranchiseEngine.startSeason(it, kotlin.random.Random.Default) }
    fun frPlayMatch() = mutateFr { com.mohithash.cricketlegend.engine.FranchiseEngine.playNextFixture(it, kotlin.random.Random.Default) }
    fun frSimSeason() = mutateFr { com.mohithash.cricketlegend.engine.FranchiseEngine.simulateSeason(it, kotlin.random.Random.Default) }

    private fun recordHallOfFame(s: GameState) {
        if (hofRecorded) return
        hofRecorded = true
        val entry = HofEntry(
            s.playerName, s.country, s.role.label, s.year, s.legacyScore,
            Progression.legacyTitle(s.legacyScore), s.intlRuns, s.intlWickets,
            s.intlHundreds, s.brokenRecords.size, s.trophies.size, s.money
        )
        hallOfFame = hallOfFame + entry
        hofFile?.writeText(json.encodeToString(hallOfFame))
    }

    private fun persist() {
        val s = state ?: return
        saveFile?.writeText(json.encodeToString(s))
    }

    private inline fun mutate(block: (GameState) -> Unit) {
        val s = state ?: return
        block(s)
        if (s.retired) recordHallOfFame(s)
        version++
        persist()
    }

    fun newGame(name: String, country: String, role: Role, startAge: Int = 18,
                playstyle: String = "Balanced", statTier: String = "Talented",
                formatFocus: String = "All", difficulty: String = "Realistic") {
        // younger starts begin with rawer skills — the prodigy has upside, not polish
        val ageGap = (18 - startAge).coerceAtLeast(0)
        val (bat0, bowl0) = when (role) {
            Role.BATTER -> 55.0 to 22.0
            Role.BOWLER -> 32.0 to 55.0
            Role.ALL_ROUNDER -> 48.0 to 46.0
            Role.WICKET_KEEPER -> 53.0 to 12.0
        }
        val tierBump = when (statTier) {
            "Rookie" -> -12.0
            "Superstar" -> 22.0
            "GOD MODE" -> 60.0
            else -> 0.0     // Talented
        }
        val god = statTier == "GOD MODE"
        val bat = (bat0 + tierBump - ageGap * 2.2).coerceIn(20.0, 99.0)
        val bowl = (bowl0 + tierBump - ageGap * 2.2).coerceIn(12.0, 99.0)
        val bowlingStyle = when (role) {
            Role.BOWLER -> listOf("Right-arm fast", "Left-arm fast", "Off-spin", "Leg-spin").random()
            Role.ALL_ROUNDER -> listOf("Right-arm fast-medium", "Left-arm orthodox", "Off-spin").random()
            else -> "Part-time medium"
        }
        val s = GameState(
            playerName = name.ifBlank { RealData.randomName(country) },
            country = country,
            role = role,
            battingHand = if (Math.random() < 0.7) "Right-hand bat" else "Left-hand bat",
            bowlingStyle = bowlingStyle,
            age = startAge,
            batting = bat,
            bowling = bowl,
            vsPace = if (god) 99.0 else bat - 4 + (Math.random() * 8),
            vsSpin = if (god) 99.0 else bat - 4 + (Math.random() * 8),
            power = if (god) 99.0 else bat - 10 + (Math.random() * 14),
            control = if (god) 99.0 else bowl - 4 + (Math.random() * 8),
            fitness = if (god) 99.0 else 70.0,
            fame = if (god) 40.0 else 5.0,
            playstyle = playstyle,
            formatFocus = formatFocus,
            difficulty = difficulty
        )
        s.propertyMarket.addAll(RealData.baseProperties.map { it.copy() })
        Finance.initMarket(s)
        com.mohithash.cricketlegend.engine.Allocations.seedDefaults(s)
        com.mohithash.cricketlegend.engine.WorldSim.seedRivals(s, kotlin.random.Random.Default)
        s.battingPosition = when (role) {
            Role.BATTER -> 3; Role.WICKET_KEEPER -> 4; Role.ALL_ROUNDER -> 5; Role.BOWLER -> 8
        }
        Scheduler.buildSeason(s)
        com.mohithash.cricketlegend.engine.Series.setSeasonObjectives(s, kotlin.random.Random.Default)
        s.addNews(
            if (startAge < 14) "A once-in-a-generation talent emerges: ${s.playerName}, age $startAge, tearing up age-group cricket."
            else "A new star rises: ${s.playerName}, ${role.label}, age $startAge."
        )
        state = s
        selectedTab = 0
        hofRecorded = false
        version++
        persist()
    }

    fun abandonCareer() {
        state = null
        saveFile?.delete()
        version++
    }

    // ---------------- Core loop ----------------

    /** Transient ball-by-ball match; not saved — leaving mid-innings replays the fixture. */
    var live: com.mohithash.cricketlegend.engine.LiveMatchState? = null
        private set

    /** Advances time to the next fixture. Returns it, or null (and flags season end). */
    private fun advanceToNextFixture(s: GameState): com.mohithash.cricketlegend.model.Fixture? {
        if (s.injuryWeeksLeft > 0) {
            val healWeek = s.week + s.injuryWeeksLeft
            s.fixtures.filter { !it.played && !it.missed && it.week < healWeek }
                .forEach { it.missed = true }
            Finance.processWeeks(s, s.injuryWeeksLeft)
            s.week = healWeek.coerceAtMost(52)
            s.injuryWeeksLeft = 0
            s.addNews("${s.playerName} is back to full fitness.")
        }
        val fx = s.nextFixture()
        if (fx == null) { s.seasonOver = true; return null }
        Finance.processWeeks(s, (fx.week - s.week).coerceAtLeast(0))
        s.week = fx.week
        // rain washes out the odd bilateral game
        if (fx.tournament == null && Math.random() < 0.04) {
            fx.played = true
            s.addNews("Washed out! Rain ruins the ${fx.format.label} vs ${fx.opponent}. Half fee paid.")
            Finance.credit(s, "Washout appearance fee", MatchEngine.matchFee(fx.statKey) / 2, taxable = true)
            if (s.nextFixture() == null) s.seasonOver = true
            return advanceToNextFixture(s)
        }
        Tournaments.startFor(s, fx, kotlin.random.Random.Default)
        return fx
    }

    private fun finishMatch(s: GameState, fx: com.mohithash.cricketlegend.model.Fixture,
                            report: com.mohithash.cricketlegend.model.MatchReport) {
        val broken = com.mohithash.cricketlegend.engine.Progression.applyReport(s, fx, report)
        s.lastReport = report.copy(recordsBroken = broken)
        s.pendingEvent = Events.maybeEvent(s)
        if (s.nextFixture() == null) s.seasonOver = true
        selectedTab = 1
    }

    /** Starts a ball-by-ball innings for white-ball games; falls back to quick sim otherwise. */
    fun playNextMatchLive() {
        val s = state ?: return
        if (s.retired || s.pendingEvent != null || live != null) return
        val fx = advanceToNextFixture(s) ?: run { version++; persist(); return }
        if (!com.mohithash.cricketlegend.engine.LiveMatch.available(fx)) {
            val report = MatchEngine.simulate(s, fx)
            finishMatch(s, fx, report)
            version++; persist(); return
        }
        live = com.mohithash.cricketlegend.engine.LiveMatch.start(s, fx)
        version++
        persist()
    }

    fun liveBall(aggression: Int) {
        val s = state ?: return
        val lm = live ?: return
        com.mohithash.cricketlegend.engine.LiveMatch.playBall(s, lm, aggression)
        version++
    }

    fun finishLiveMatch() = mutate { s ->
        val lm = live ?: return@mutate
        if (!lm.inningsOver && !lm.playerOut) return@mutate
        val report = MatchEngine.simulateLive(s, lm.fixture, lm)
        finishMatch(s, lm.fixture, report)
        live = null
    }

    fun playNextMatch() = mutate { s ->
        if (s.retired || s.pendingEvent != null || live != null) return@mutate
        val fx = advanceToNextFixture(s) ?: return@mutate
        val report = MatchEngine.simulate(s, fx)
        finishMatch(s, fx, report)
    }

    /** Fast-forwards the season, stopping when an event/auction needs a decision. */
    fun simRestOfSeason() = mutate { s ->
        var guard = 0
        while (guard++ < 60 && !s.retired && !s.seasonOver &&
            s.pendingEvent == null && s.pendingAuction == null && live == null) {
            val fx = advanceToNextFixture(s) ?: break
            val report = MatchEngine.simulate(s, fx)
            finishMatch(s, fx, report)
        }
    }

    fun retireFromFormat(format: String) = mutate { s ->
        if (format !in listOf("TEST", "ODI", "T20I") || format in s.retiredFormats) return@mutate
        s.retiredFormats.add(format)
        when (format) {
            "TEST" -> s.inNationalTest = false
            "ODI" -> s.inNationalODI = false
            "T20I" -> s.inNationalT20 = false
        }
        s.addNews("${s.playerName} announces retirement from $format cricket to prolong the career.")
        s.fame = (s.fame + 1).coerceAtMost(100.0)
    }

    fun setBattingPosition(pos: Int) = mutate { it.battingPosition = pos.coerceIn(1, 8) }

    fun resolveEvent(choiceIndex: Int) = mutate { s ->
        val ev = s.pendingEvent ?: return@mutate
        Events.resolve(s, ev, choiceIndex)
    }

    fun startNewSeason() = mutate { s ->
        if (!s.seasonOver || s.retired) return@mutate
        val remaining = (52 - s.week).coerceAtLeast(0)
        Finance.processWeeks(s, remaining)
        Progression.endSeason(s)
        selectedTab = 0
    }

    fun retire() = mutate { s ->
        if (!s.retired) {
            Progression.retire(s)
            com.mohithash.cricketlegend.engine.Legacy.checkLifeMilestones(s)
        }
    }

    fun chooseSecondCareer(path: String) = mutate { s ->
        com.mohithash.cricketlegend.engine.Legacy.chooseSecondCareer(s, path)
    }

    fun advanceRetiredYear() = mutate { s ->
        if (s.retired && s.secondCareer != null && s.age < 75) {
            com.mohithash.cricketlegend.engine.Legacy.advanceRetiredYear(s, kotlin.random.Random.Default)
        }
    }

    /** Dynasty: continue as your grown child, inheriting name, wealth and a talent boost. */
    fun continueAsChild(role: Role, playstyle: String, formatFocus: String) {
        val parent = state ?: return
        if (!parent.retired) return
        recordHallOfFame(parent)
        val child = parent.childNames.firstOrNull()
            ?: (RealData.firstNames[parent.country]?.random() ?: "Junior")
        val surname = parent.playerName.split(" ").lastOrNull() ?: parent.playerName
        val fullName = "$child $surname"
        val inheritedWealth = (parent.money / 4).coerceIn(5_000_000, 5_000_000_000)
        newGame(fullName, parent.country, role, 14, playstyle, "Superstar", formatFocus)
        state?.let { c ->
            c.generation = parent.generation + 1
            c.inheritedLegacy = parent.legacyScore
            c.dynastyName = parent.dynastyName.ifBlank { surname }
            c.money = inheritedWealth
            // second-generation talent bonus — the bloodline runs deep
            c.batting = (c.batting + 6).coerceAtMost(99.0)
            c.power = (c.power + 6).coerceAtMost(99.0)
            c.addNews("DYNASTY: $fullName (gen ${c.generation}) follows the legendary $surname bloodline into cricket.")
            version++; persist()
        }
    }

    fun resolveAuction(acceptRetention: Boolean) = mutate { s ->
        if (s.pendingAuction == null) return@mutate
        AuctionEngine.resolve(s, acceptRetention, kotlin.random.Random.Default)
        Scheduler.buildSeason(s)
    }

    fun setAllocation(id: String, weekly: Long) = mutate { com.mohithash.cricketlegend.engine.Allocations.set(it, id, weekly) }
    fun setTrainingFocus(focus: String) = mutate { it.trainingFocus = focus; toast = "Training focus: $focus" }
    fun setPlaystyle(style: String) = mutate { it.playstyle = style; toast = "Playing style set to $style" }
    fun setBattingPositionFeedback(pos: Int) = mutate { it.battingPosition = pos.coerceIn(1, 8); toast = "You'll bat at #${it.battingPosition}" }
    fun buyProperty(p: Property) = mutate {
        toast = if (Finance.buyProperty(it, p.id)) "Bought ${p.name}, ${p.city} ✔" else "Can't afford ${p.name}"
    }
    fun sellProperty(id: String) = mutate {
        toast = if (Finance.sellProperty(it, id)) "Property sold ✔" else "Sale failed"
    }
    fun buyItem(id: String) = mutate { s ->
        val name = com.mohithash.cricketlegend.data.RealData.lifestyleItems.firstOrNull { it.id == id }?.name ?: "item"
        toast = if (Finance.buyItem(s, id)) "$name is yours ✔" else "Can't afford $name"
    }
    fun hireStaff(id: String, tier: Int) = mutate {
        toast = if (Finance.hireStaff(it, id, tier)) "Staff hired ✔" else "Hire failed"
    }
    fun buyInstrument(id: String, amount: Long) = mutate {
        toast = if (Finance.buyInstrument(it, id, amount))
            "Invested ${com.mohithash.cricketlegend.engine.Money.fmt(amount, it.country)} ✔" else "Not enough cash"
    }
    fun sellInstrument(id: String) = mutate {
        toast = if (Finance.sellInstrument(it, id)) "Position sold ✔" else "Nothing to sell"
    }
    fun startBusiness(id: String) = mutate {
        toast = if (Finance.startBusiness(it, id)) "Business launched ✔" else "Requirements not met"
    }
    fun familyGift() = mutate {
        toast = if (Finance.familyGift(it)) "Gift sent — ${it.partner} loved it ❤" else "No partner / not enough cash"
    }
    fun familyVacation() = mutate {
        toast = if (Finance.familyVacation(it)) "Holiday booked — relationship +15 ❤" else "No partner / not enough cash"
    }

    // ---- life systems (return a toast message for the UI) ----
    var toast: String? = null
        private set
    fun clearToast() { toast = null; version++ }
    private fun action(block: (GameState) -> String) = mutate { toast = block(it) }

    fun socialPost(kind: String) = action { LifeSystems.post(it, kind, kotlin.random.Random.Default) }
    fun startDating(type: String) = mutate {
        toast = if (LifeSystems.startDating(it, type, kotlin.random.Random.Default))
            "You're now dating ${it.partner} ❤" else "That match isn't happening yet"
    }
    fun acceptFixing() = action { LifeSystems.acceptFixing(it, kotlin.random.Random.Default) }
    fun mentorPlayer() = action { LifeSystems.mentor(it, kotlin.random.Random.Default) }
    fun buyFranchise() = action { LifeSystems.buyFranchise(it) }
    fun signSquadPlayer() = action { LifeSystems.signSquadPlayer(it, kotlin.random.Random.Default) }
    fun playForOwnFranchise() = action { LifeSystems.playForOwnFranchise(it) }
}
