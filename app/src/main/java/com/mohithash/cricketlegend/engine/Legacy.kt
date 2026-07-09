package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

/**
 * The "experience of success" layer: an escalating icon status beyond GOAT,
 * grand ceremonial life milestones, and a forward-looking record projection so
 * you always know what greatness is coming next.
 */
object Legacy {

    // ---------------- Icon status ----------------

    /** Icon tier climbs forever past the GOAT threshold — the fantasy has no ceiling. */
    fun iconStatus(s: GameState): String {
        val g = LifeSystems.goatScore(s)
        return when {
            g >= 15000 -> "The Cricketing Deity"
            g >= 9000 -> "Transcendent Immortal"
            g >= 6000 -> "Immortal of the Game"
            g >= 4000 -> "Global Sporting Icon"
            g >= 2500 -> "Living Legend"
            g >= 1400 -> "National Treasure"
            g >= 700 -> "Superstar"
            g >= 300 -> "Established International"
            else -> "Rising Talent"
        }
    }

    /** Progress toward the next icon tier (0..1) for a bar. */
    fun iconProgress(s: GameState): Pair<Double, String> {
        val g = LifeSystems.goatScore(s).toDouble()
        val bands = listOf(0, 300, 700, 1400, 2500, 4000, 6000, 9000, 15000, 25000)
        val next = bands.firstOrNull { it > g } ?: return 1.0 to "Maxed"
        val prev = bands.last { it <= g }
        return ((g - prev) / (next - prev)).coerceIn(0.0, 1.0) to "$prev → $next"
    }

    // ---------------- Grand life ceremonies ----------------

    private data class Milestone(val id: String, val text: (GameState) -> String, val cond: (GameState) -> Boolean)

    private val milestones = listOf(
        Milestone("first_crore", { "💰 First ₹1 crore in the bank — the boy from the maidan has arrived." }) { it.money >= 10_000_000 },
        Milestone("hundred_cr", { "🏦 Net worth crosses ₹100 crore. Generational wealth secured." }) { it.money >= 1_000_000_000 },
        Milestone("billionaire", { "🪙 You are officially a BILLIONAIRE (₹1,000 Cr+). Cricket built an empire." }) { it.money >= 10_000_000_000 },
        Milestone("wax_figure", { "🕯️ A wax figure of ${it.playerName} is unveiled at the famous museum." }) { it.fame >= 75 },
        Milestone("biopic", { "🎬 The authorised biopic of ${it.playerName} premieres to a standing ovation." }) { it.fame >= 85 && it.brokenRecords.isNotEmpty() },
        Milestone("statue", { "🗿 A bronze statue of ${it.playerName} is unveiled outside the home stadium." }) { it.brokenRecords.size >= 2 && it.fame >= 90 },
        Milestone("stadium", { "🏟️ The stadium is renamed the ${it.playerName} Stadium. You ARE the ground now." }) { it.brokenRecords.size >= 4 },
        Milestone("national_honour", { "🎖️ The nation confers its highest civilian honour upon ${it.playerName}." }) { it.awards.size >= 6 && it.fame >= 92 },
        Milestone("knighthood", { "🤴 ${it.playerName} is conferred an honorary knighthood for services to cricket." }) { it.trophies.size >= 6 && it.fame >= 95 },
        Milestone("island", { "🏝️ ${it.playerName} buys a private island. Off-season paradise, secured." }) { it.money >= 5_000_000_000 },
        Milestone("head_of_state", { "🏛️ The head of state hosts a banquet in honour of ${it.playerName}." }) { it.awards.size >= 10 },
        Milestone("goat_debate_over", { "🐐 Pundits close the GOAT debate — it's simply ${it.playerName}, forever." }) { it.goatAnnounced },
        Milestone("global_brand", { "🌍 The ${it.playerName} name becomes a global brand worth more than most clubs." }) { it.followers >= 300_000_000 && it.endorsements.size >= 3 },
        Milestone("jersey_retired", { "👕 ${it.playerName}'s jersey number is retired across every team." }) { it.retired }
    )

    fun checkLifeMilestones(s: GameState) {
        for (m in milestones) {
            if (m.id in s.lifeAchievements) continue
            if (m.cond(s)) {
                s.lifeAchievements.add(m.id)
                s.morale = (s.morale + 6).coerceAtMost(100.0)
                s.addNews("LIFE MILESTONE — ${m.text(s)}")
            }
        }
    }

    fun achievementText(s: GameState, id: String): String =
        milestones.firstOrNull { it.id == id }?.text?.invoke(s) ?: id

    // ---------------- Record projection ----------------

    data class Projection(val label: String, val current: Int, val target: Int, val etaMatches: Int)

    /** What greatness is coming next — records you'll break and roughly when. */
    fun recordProjection(s: GameState): List<Projection> {
        val perMatchRuns = ratePerMatch(s) { it.intlRuns }
        val out = ArrayList<Projection>()

        fun add(label: String, cur: Int, target: Int, perMatch: Double) {
            if (cur >= target || perMatch <= 0.0) return
            out.add(Projection(label, cur, target, Math.ceil((target - cur) / perMatch).toInt()))
        }

        add("Most international runs", s.intlRuns, WorldSim.dynValue(s, "intl_runs").toInt() + 1, perMatchRuns)
        add("Most international centuries", s.intlHundreds,
            WorldSim.dynValue(s, "intl_hundreds").toInt() + 1, ratePerMatch(s) { it.intlHundreds })
        add("Most international sixes", s.intlSixes,
            WorldSim.dynValue(s, "intl_sixes").toInt() + 1, ratePerMatch(s) { it.intlSixes })
        add("Most Test runs", s.stat(StatKey.INTL_TEST).runs,
            WorldSim.dynValue(s, "test_runs").toInt() + 1, ratePerMatch(s) { it.stat(StatKey.INTL_TEST).runs })
        add("Most ODI runs", s.stat(StatKey.INTL_ODI).runs,
            WorldSim.dynValue(s, "odi_runs").toInt() + 1, ratePerMatch(s) { it.stat(StatKey.INTL_ODI).runs })
        // fantasy horizon goals
        add("50,000 international runs (fantasy)", s.intlRuns, 50_000, perMatchRuns)
        add("500 international centuries (fantasy)", s.intlHundreds, 500, ratePerMatch(s) { it.intlHundreds })

        return out.sortedBy { it.etaMatches }.take(6)
    }

    private inline fun ratePerMatch(s: GameState, sel: (GameState) -> Int): Double {
        val m = s.intlMatches
        return if (m > 0) sel(s).toDouble() / m else 0.0
    }

    // ---------------- Post-retirement second career ----------------

    val secondCareers = listOf(
        "Head Coach" to "Coach the national team — chase trophies from the dugout.",
        "Commentator" to "The voice of the game — fame stays high, easy money.",
        "Administrator" to "Rise through the board toward ICC Chairman.",
        "Politician" to "Trade the pitch for parliament on your popularity.",
        "Business Mogul" to "Go full-time on the empire — franchises and brands."
    )

    fun chooseSecondCareer(s: GameState, path: String) {
        s.secondCareer = path
        s.postRetireYear = s.year
        s.addNews("NEXT INNINGS: ${s.playerName} begins life as a $path.")
    }

    /** Simulate one year of post-playing life. */
    fun advanceRetiredYear(s: GameState, rng: Random) {
        s.age++
        s.year++
        val path = s.secondCareer ?: return
        when (path) {
            "Head Coach" -> {
                val won = rng.nextDouble() < (0.35 + s.fame / 300.0)
                Finance.credit(s, "Head coach salary", 120_000_000, taxable = true)
                if (won) {
                    s.trophies.add("${s.year - 1} — coached to a title")
                    s.addNews("As coach, ${s.playerName} guides ${s.country} to more silverware!")
                } else s.addNews("A building year in the coaching role for ${s.playerName}.")
            }
            "Commentator" -> {
                Finance.credit(s, "Broadcasting deal", 80_000_000, taxable = true)
                s.fame = (s.fame - 1).coerceAtLeast(40.0)
                s.addNews("${s.playerName}'s commentary is must-listen radio and TV.")
            }
            "Administrator" -> {
                Finance.credit(s, "Board stipend", 40_000_000, taxable = true)
                if (rng.nextDouble() < 0.25 && "ICC Chairman" !in s.awards) {
                    s.awards.add("ICC Chairman")
                    s.addNews("POWER MOVE: ${s.playerName} is elected ICC Chairman!")
                } else s.addNews("${s.playerName} climbs cricket's corridors of power.")
            }
            "Politician" -> {
                if (rng.nextDouble() < (0.3 + s.fame / 250.0) && "Elected to Office" !in s.awards) {
                    s.awards.add("Elected to Office")
                    s.addNews("LANDSLIDE: ${s.playerName} wins a seat on cricketing fame alone!")
                } else s.addNews("${s.playerName} works the campaign trail.")
            }
            else -> { // Business Mogul
                LifeSystems.franchiseYearlyPnL(s, rng)
                Finance.processWeeks(s, 52, rng)
                s.addNews("Mogul life: ${s.playerName}'s empire keeps compounding.")
            }
        }
        checkLifeMilestones(s)
        Media.weeklyBuzz(s, rng)
    }
}
