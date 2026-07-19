package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.StatKey

/** Build-up context for the next fixture — turns a button-press into an occasion. */
object MatchPreview {

    data class Preview(
        val headline: String,
        val lines: List<String>,
        val stakes: String?,
        val warning: String?
    )

    fun build(s: GameState, fx: Fixture): Preview {
        val lines = ArrayList<String>()

        // opposition strength + a form read
        val oppTeam = RealData.teams.firstOrNull { it.name == fx.opponent }
        if (fx.level == Level.INTERNATIONAL && oppTeam != null) {
            val tier = when {
                oppTeam.strength >= 88 -> "a powerhouse"
                oppTeam.strength >= 80 -> "a strong side"
                oppTeam.strength >= 72 -> "a competitive outfit"
                else -> "beatable opponents"
            }
            lines += "${fx.opponent} are $tier (rating ${oppTeam.strength})."
            val danger = oppTeam.starBowlers.firstOrNull()
            if (danger != null) lines += "Watch for their spearhead, $danger."
        } else if (fx.level == Level.FRANCHISE) {
            RealData.franchise(fx.opponent)?.let { lines += "${fx.opponent} — captained by ${it.captain}, ${it.titles} title(s)." }
        }

        // pitch read
        val pitchRead = when (fx.pitch) {
            "PACE" -> "A quick, bouncy deck — the seamers will fancy it."
            "GREEN" -> "Grass on top; movement for the bowlers early."
            "SPIN" -> "Dry and dusty — the spinners will be licking their lips."
            "FLAT" -> "A road. Bat first, cash in."
            else -> "A balanced surface with something for everyone."
        }
        lines += "$pitchRead (${fx.venue})"

        // your form guide
        if (s.recentScores.isNotEmpty()) {
            val last = s.recentScores.takeLast(5).joinToString(", ")
            val note = when {
                s.form >= 2.5 -> "You're in the form of your life."
                s.form <= -2.5 -> "You're in a slump — a big one would silence the critics."
                else -> "Your recent form is steady."
            }
            lines += "Form guide: [$last]. $note"
        }
        if (s.sharpness < 45) lines += "You're under-cooked (sharpness ${s.sharpness.toInt()}%) — a risk of a rusty display."

        // nemesis warning
        val warning = if (fx.level == Level.INTERNATIONAL && oppTeam != null) {
            oppTeam.starBowlers.map { it to (s.dismissedBy[it] ?: 0) }
                .filter { it.second >= 3 }.maxByOrNull { it.second }
                ?.let { "☠ ${it.first} has dismissed you ${it.second} times. Your nemesis is waiting." }
        } else null

        // what's at stake
        val stakes = when {
            fx.stage?.name == "FINAL" -> "🏆 THE FINAL. Everything is on the line."
            fx.stage != null -> "Knockout cricket — win or go home."
            fx.opponent == s.derbyRival -> "🔥 DERBY DAY vs ${s.derbyRival}. Bragging rights await."
            else -> recordStake(s)
        }

        val headline = when {
            fx.stage?.name == "FINAL" -> "FINAL — ${fx.tournament}"
            fx.opponent == s.derbyRival -> "DERBY: ${s.franchiseTeam} vs ${fx.opponent}"
            else -> "${StatKey.label(fx.statKey)} vs ${fx.opponent}"
        }

        return Preview(headline, lines, stakes, warning)
    }

    /** Surfaces a record you could edge closer to in this match. */
    private fun recordStake(s: GameState): String? {
        val runsToRecord = WorldSim.dynValue(s, "intl_runs") - s.intlRuns
        if (runsToRecord in 1.0..300.0) return "📈 ${runsToRecord.toInt()} runs from the all-time international runs record."
        val tonsToRecord = WorldSim.dynValue(s, "intl_hundreds") - s.intlHundreds
        if (tonsToRecord in 1.0..3.0) return "📈 ${tonsToRecord.toInt()} century(ies) from the all-time record."
        return null
    }
}
