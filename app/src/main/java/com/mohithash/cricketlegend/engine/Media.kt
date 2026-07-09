package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Fixture
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Level
import com.mohithash.cricketlegend.model.MatchReport
import com.mohithash.cricketlegend.model.StatKey
import kotlin.random.Random

/**
 * Generates punditry, tabloid verdicts and record-chase alerts. Separate from the
 * plain news feed so the UI can show a richer "Media" wall.
 */
object Media {

    private val pundits = listOf("Harsha", "Ravi Shastri", "Sunny G", "Ian Bishop", "Nasser", "Isa Guha")

    fun afterMatch(s: GameState, fx: Fixture, report: MatchReport, rng: Random) {
        val runs = report.batting.sumOf { it.runs }
        val wkts = report.bowling.sumOf { it.wickets }
        val p = pundits.random(rng)

        // pundit verdict on the performance
        val verdict = when {
            report.rating >= 9.0 -> listOf(
                "\"Sensational. $runs and utterly in control — that's an all-time great at work.\" — $p",
                "\"I've run out of superlatives for ${s.playerName}. Pure genius.\" — $p"
            )
            report.rating >= 7.0 -> listOf(
                "\"A classy knock. ${s.playerName} is rounding into the complete player.\" — $p",
                "\"Match-defining stuff from ${s.playerName} today.\" — $p"
            )
            report.rating < 3.5 -> listOf(
                "\"Questions will be asked. ${s.playerName} looked out of sorts.\" — $p",
                "\"A day to forget — the critics are circling.\" — $p"
            )
            else -> listOf("\"A workmanlike effort from ${s.playerName}.\" — $p")
        }.random(rng)
        s.addNews(verdict)

        // opposition star reaction on a big performance
        if (fx.level == Level.INTERNATIONAL && report.rating >= 8.0) {
            val star = RealData.teams.firstOrNull { it.name == fx.opponent }?.starBowlers?.random(rng)
            if (star != null) s.addNews("$star: \"We tried everything. ${s.playerName} was on another level.\"")
        }

        // milestone / record chase ticker
        recordChase(s)
    }

    /** Surfaces the nearest cumulative record the player is closing in on. */
    fun recordChase(s: GameState) {
        data class Chase(val label: String, val cur: Double, val target: Double)
        val chases = listOf(
            Chase("Most international runs", s.intlRuns.toDouble(), WorldSim.dynValue(s, "intl_runs")),
            Chase("Most international centuries", s.intlHundreds.toDouble(), WorldSim.dynValue(s, "intl_hundreds")),
            Chase("Most international sixes", s.intlSixes.toDouble(), WorldSim.dynValue(s, "intl_sixes")),
            Chase("Most Test runs", s.stat(StatKey.INTL_TEST).runs.toDouble(), WorldSim.dynValue(s, "test_runs")),
            Chase("Most ODI runs", s.stat(StatKey.INTL_ODI).runs.toDouble(), WorldSim.dynValue(s, "odi_runs")),
            Chase("Most franchise-league runs", s.stat(StatKey.LEAGUE).runs.toDouble(), WorldSim.dynValue(s, "league_runs"))
        )
        val near = chases
            .filter { it.cur < it.target && it.cur > it.target * 0.80 }
            .minByOrNull { it.target - it.cur } ?: return
        val gap = (near.target - near.cur).toInt()
        if (gap in 1..400) {
            s.addNews("RECORD WATCH: just $gap short of the ${near.label} record (${WorldSim.dynHolder(s, chaseId(near.label))}).")
        }
    }

    private fun chaseId(label: String) = when (label) {
        "Most international runs" -> "intl_runs"
        "Most international centuries" -> "intl_hundreds"
        "Most international sixes" -> "intl_sixes"
        "Most Test runs" -> "test_runs"
        "Most ODI runs" -> "odi_runs"
        "Most franchise-league runs" -> "league_runs"
        else -> ""
    }

    /** Weekly life vignette between matches — flavour that makes the world feel alive. */
    fun weeklyBuzz(s: GameState, rng: Random) {
        if (rng.nextDouble() > 0.18) return
        val buzz = mutableListOf(
            "Fans spotted queuing overnight for ${s.playerName} jerseys.",
            "A street mural of ${s.playerName} appears in ${s.country}.",
            "${s.playerName} trends nationwide after a training-ground trick shot.",
            "Talk-show hosts debate whether ${s.playerName} is already a legend.",
            "A biopic on ${s.playerName} is reportedly in the works."
        )
        if (s.captainNation) buzz.add("Column inches question ${s.playerName}'s captaincy tactics.")
        if (s.publicImage < -10) buzz.add("Tabloids run another unflattering ${s.playerName} story.")
        if (s.married) buzz.add("${s.playerName} and ${s.partner} photographed on a rare date night.")
        if (s.ownedFranchise != null) buzz.add("${s.ownedFranchise} fans praise owner ${s.playerName}'s ambition.")
        s.addNews(buzz.random(rng))
    }
}
