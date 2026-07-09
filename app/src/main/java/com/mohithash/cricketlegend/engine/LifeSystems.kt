package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.GameState
import kotlin.math.max
import kotlin.random.Random

/**
 * Off-field life systems the player drives directly (not event-gated):
 * GOAT meter, arch-rival narrative, dating tiers, social media, mentoring,
 * match-fixing approaches and franchise ownership.
 */
object LifeSystems {

    // ---------------- GOAT / legacy meter ----------------

    /** Live GOAT score usable mid-career (mirrors the retirement legacy formula). */
    fun goatScore(s: GameState): Int {
        var score = 0.0
        score += s.intlRuns / 100.0
        score += s.intlWickets * 1.6
        score += s.intlHundreds * 6.0
        score += s.brokenRecords.size * 60.0
        score += s.trophies.size * 45.0
        score += s.awards.size * 12.0
        score += s.fame
        return score.toInt()
    }

    fun goatTier(score: Int): String = Progression.legacyTitle(score)

    /** Earns a media nickname once the player has an identity worth naming. */
    fun updateNickname(s: GameState) {
        if (s.nickname.isNotEmpty()) return
        val short = s.playerName.split(" ").firstOrNull()?.take(4) ?: "The Kid"
        val nick = when {
            s.brokenRecords.size >= 5 && s.playstyle == "Aggressive" -> "The Destroyer"
            s.brokenRecords.size >= 5 -> "The Run Machine"
            s.intlSixes >= 300 -> "$short the Big-Hitter"
            s.playstyle == "Aggressive" && s.intlMatches >= 30 -> "The Fearless One"
            s.playstyle == "Defensive" && s.intlMatches >= 30 -> "The Wall"
            s.stat(com.mohithash.cricketlegend.model.StatKey.INTL_TEST).hundreds >= 15 -> "The Colossus"
            s.age <= 17 && s.intlMatches >= 5 -> "The Boy Wonder"
            else -> return
        }
        s.nickname = nick
        s.addNews("The media have a name for ${s.playerName} now: \"$nick\".")
    }

    /** Fires the one-time "THE GOAT" declaration once the player is statistically the best. */
    fun checkGoat(s: GameState) {
        if (s.goatAnnounced) return
        if (goatScore(s) >= 1200 && s.brokenRecords.size >= 5 && s.trophies.size >= 5) {
            s.goatAnnounced = true
            s.fame = 100.0
            s.awards.add("${s.year} — Declared the Greatest Of All Time")
            s.addNews("IMMORTAL. The cricket world crowns ${s.playerName} the undisputed GOAT.")
        }
    }

    // ---------------- Arch-rival ----------------

    /** Picks a same-generation rival as the career-long nemesis narrative. */
    fun ensureArchRival(s: GameState, rng: Random) {
        if (s.archRival != null) return
        if (s.age < 20 || s.fame < 35) return
        val candidate = s.rivals.filter { !it.retired && !it.isBowler && it.country != s.country && it.age in (s.age - 3)..(s.age + 3) }
            .maxByOrNull { it.skill } ?: return
        s.archRival = candidate.name
        s.addNews("A RIVALRY IS BORN: the media pit ${s.playerName} against ${candidate.name} for the era's crown.")
    }

    fun archRivalScore(s: GameState): Pair<Int, Int>? {
        val rival = s.rivals.firstOrNull { it.name == s.archRival } ?: return null
        return goatScore(s) to (rival.intlRuns / 100 + rival.hundreds * 6 + (rival.skill * 2).toInt())
    }

    // ---------------- Social media ----------------

    fun canPost(s: GameState): Boolean = s.week - s.lastPostWeek >= 2

    /** kind: "training", "brand", "hottake" */
    fun post(s: GameState, kind: String, rng: Random): String {
        if (!canPost(s)) return "Post again in a couple of weeks."
        s.lastPostWeek = s.week
        return when (kind) {
            "training" -> {
                val gain = (s.followers * 0.02 + s.fame * 5000).toLong()
                s.followers += gain
                s.morale = (s.morale + 1).coerceAtMost(100.0)
                "Training clip posted — ${fmtNum(gain)} new followers."
            }
            "brand" -> {
                if (s.endorsements.isEmpty()) return "Sign an endorsement first to run a paid promo."
                val fee = (s.followers / 40 + 200_000).coerceAtMost(20_000_000)
                Finance.credit(s, "Paid brand promo post", fee, taxable = true)
                s.publicImage = (s.publicImage - 1).coerceIn(-50.0, 50.0)
                "Paid promo earned ${Money.fmt(fee, s.country)} (fans grumble about the sellout)."
            }
            else -> { // hot take
                val viral = rng.nextDouble() < 0.5
                if (viral) {
                    val gain = (s.followers * 0.06 + 500_000).toLong()
                    s.followers += gain
                    s.publicImage = (s.publicImage + 3).coerceIn(-50.0, 50.0)
                    "Hot take goes viral — ${fmtNum(gain)} followers and the internet agrees!"
                } else {
                    s.followers = (s.followers * 0.97).toLong()
                    s.publicImage = (s.publicImage - 6).coerceIn(-50.0, 50.0)
                    s.morale = (s.morale - 3).coerceAtLeast(5.0)
                    "Troll storm! The take backfired — image and morale take a hit."
                }
            }
        }
    }

    // ---------------- Dating tiers ----------------

    data class DateOption(val type: String, val fameReq: Int, val cost: Long, val fameBoost: Double, val drain: Double)
    val dateOptions = listOf(
        DateOption("College Sweetheart", 0, 0, 0.0, 0.10),
        DateOption("Fellow Athlete", 25, 0, 2.0, 0.18),
        DateOption("Fashion Model", 45, 2_000_000, 5.0, 0.30),
        DateOption("Movie Star", 70, 8_000_000, 10.0, 0.45)
    )

    fun startDating(s: GameState, type: String, rng: Random): Boolean {
        val opt = dateOptions.firstOrNull { it.type == type } ?: return false
        if (s.partner != null || s.fame < opt.fameReq || s.money < opt.cost) return false
        if (opt.cost > 0) s.addLedger("Courting a ${opt.type}", -opt.cost)
        s.partner = RealData.partnerNames.random(rng)
        s.partnerType = opt.type
        s.relationship = 65.0
        s.fame = (s.fame + opt.fameBoost).coerceAtMost(100.0)
        s.followers += (opt.fameBoost * 400_000).toLong()
        s.addNews("It's official: ${s.playerName} is dating ${s.partner}, a ${opt.type.lowercase()}.")
        return true
    }

    // ---------------- Match-fixing ----------------

    /** Bookie approach; accept for a payday but risk a career-ending ban. */
    fun acceptFixing(s: GameState, rng: Random): String {
        val bribe = 20_000_000L + (s.fame * 1_500_000L).toLong()
        s.addLedger("Untraceable cash (?!)", bribe)   // no tax on dirty money
        if (rng.nextDouble() < 0.45) {
            val ban = 2 + rng.nextInt(4)
            s.banSeasons = ban
            s.publicImage = (s.publicImage - 40).coerceAtLeast(-50.0)
            s.fame = (s.fame * 0.4)
            s.inNationalTest = false; s.inNationalODI = false; s.inNationalT20 = false
            s.captainNation = false
            s.addNews("SPOT-FIXING SCANDAL! ${s.playerName} BANNED for $ban seasons in disgrace.")
            return "Caught! Banned $ban seasons and a national villain."
        }
        s.publicImage = (s.publicImage - 3).coerceAtLeast(-50.0)
        s.addNews("A quiet, profitable arrangement... nobody suspects a thing.")
        return "You got away with it — ${Money.fmt(bribe, s.country)} richer."
    }

    // ---------------- Mentoring ----------------

    fun mentor(s: GameState, rng: Random): String {
        if (s.age < 30 || s.fame < 55) return "You need to be an established senior pro (30+, fame 55+) to mentor."
        val prospect = s.rivals.filter { !it.retired && it.age <= 22 && it.name !in s.mentees }
            .minByOrNull { it.skill }
            ?: return "No young prospect available to take under your wing right now."
        s.mentees.add(prospect.name)
        prospect.skill = (prospect.skill + 6).coerceAtMost(97.0)
        s.publicImage = (s.publicImage + 4).coerceIn(-50.0, 50.0)
        s.addNews("${s.playerName} takes young ${prospect.name} under their wing — a mentor is born.")
        return "You're now mentoring ${prospect.name}. Their game just levelled up."
    }

    // ---------------- Franchise ownership ----------------

    const val FRANCHISE_PRICE = 5_000_000_000L   // ₹500 Cr

    fun buyFranchise(s: GameState): String {
        if (s.ownedFranchise != null) return "You already own a franchise."
        if (s.money < FRANCHISE_PRICE) return "You need ${Money.fmt(FRANCHISE_PRICE, s.country)} to buy in."
        val available = RealData.franchiseTeams.filter { it != s.franchiseTeam }
        val team = available.random()
        s.addLedger("Bought franchise: $team", -FRANCHISE_PRICE)
        s.ownedFranchise = team
        s.ownedSquadStrength = RealData.franchise(team)?.strength?.toDouble() ?: 52.0
        s.ownedSquad.clear()
        s.ownedSquad.addAll(List(5) { RealData.randomName(s.country) })
        s.fame = (s.fame + 5).coerceAtMost(100.0)
        s.addNews("MOGUL! ${s.playerName} buys the $team franchise for ${Money.fmt(FRANCHISE_PRICE, s.country)}!")
        return "You now own the $team. Manage it from the Empire tab."
    }

    /** Owner spends to sign a marquee player — raises squad strength. */
    fun signSquadPlayer(s: GameState, rng: Random): String {
        if (s.ownedFranchise == null) return "You don't own a franchise."
        val cost = 400_000_000L
        if (s.money < cost) return "Signing a marquee player costs ${Money.fmt(cost, s.country)}."
        s.addLedger("${s.ownedFranchise} — marquee signing", -cost)
        val name = s.rivals.filter { !it.retired && it.name !in s.ownedSquad }.maxByOrNull { it.skill }?.name
            ?: RealData.randomName(s.country)
        s.ownedSquad.add(name)
        while (s.ownedSquad.size > 8) s.ownedSquad.removeAt(0)
        s.ownedSquadStrength = (s.ownedSquadStrength + 3 + rng.nextInt(3)).coerceAtMost(95.0)
        return "Signed $name! ${s.ownedFranchise} squad strength is now ${s.ownedSquadStrength.toInt()}."
    }

    /** Player suits up for the team they own (skips the auction next season). */
    fun playForOwnFranchise(s: GameState): String {
        val team = s.ownedFranchise ?: return "You don't own a franchise."
        if (s.franchiseTeam == team) return "You already play for your own $team."
        s.franchiseTeam = team
        s.captainFranchise = true
        s.addNews("OWNER-CAPTAIN! ${s.playerName} names themselves in the $team XI.")
        return "You'll turn out for your own $team next season — as captain, naturally."
    }

    /** Full franchise campaign each season: standings finish, title, and P&L. */
    fun franchiseYearlyPnL(s: GameState, rng: Random) {
        val team = s.ownedFranchise ?: return

        // simulate the owned club's league campaign from squad strength (+ owner-player boost)
        val playerBoost = if (s.franchiseTeam == team) (max(s.batting, s.bowling) - 55).coerceAtLeast(0.0) * 0.25 else 0.0
        val strength = (s.ownedSquadStrength + playerBoost).coerceIn(40.0, 99.0)
        val wins = (4 + ((strength - 50) / 6.0) + rng.nextInt(4)).toInt().coerceIn(2, 13)
        val madePlayoffs = wins >= 8
        val wonTitle = madePlayoffs && rng.nextDouble() < ((strength - 45) / 60.0).coerceIn(0.1, 0.85)
        s.ownedLastFinish = when {
            wonTitle -> "CHAMPIONS ($wins wins)"
            madePlayoffs -> "Playoffs ($wins wins)"
            else -> "Missed out ($wins wins)"
        }
        if (wonTitle) {
            s.ownedTitles++
            s.trophies.add("Premier T20 League ${s.year} (owner)")
            s.fame = (s.fame + 4).coerceAtMost(100.0)
            s.addNews("OWNER'S GLORY! ${s.playerName}'s $team are Premier League CHAMPIONS!")
        } else {
            s.addNews("$team finish the league season: ${s.ownedLastFinish}.")
        }
        // squad naturally ages/drifts a touch each year
        s.ownedSquadStrength = (s.ownedSquadStrength - 1.0 + rng.nextDouble() * 1.5).coerceIn(40.0, 95.0)

        val base = 300_000_000L + rng.nextLong(200_000_000L)
        val pnl = if (wonTitle) base + 300_000_000L else if (madePlayoffs) base else base - rng.nextLong(200_000_000L)
        Finance.credit(s, "$team franchise annual result", pnl.coerceAtLeast(-250_000_000L), taxable = pnl > 0)
        s.addNews("$team ${if (pnl >= 0) "post a profit" else "run a loss"} of ${Money.fmt(pnl, s.country)}.")
    }

    private fun fmtNum(n: Long): String = when {
        n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0)
        n >= 1_000 -> "%.0fK".format(n / 1_000.0)
        else -> "$n"
    }
}
