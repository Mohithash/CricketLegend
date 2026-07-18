package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.model.GameState

/**
 * Central difficulty scaling. Realistic is the intended balance; Easy is forgiving,
 * Hardcore is unforgiving. All multipliers are relative to Realistic = 1.0.
 */
object Difficulty {

    // higher = you get out more often
    fun dismissalMult(s: GameState): Double = when (s.difficulty) {
        "Easy" -> 0.78
        "Hardcore" -> 1.28
        else -> 1.0
    }

    // how much of the old "elite dominance" curve survives (0 = flat realism)
    fun eliteMult(s: GameState): Double = when (s.difficulty) {
        "Easy" -> 0.45
        "Hardcore" -> 0.08
        else -> 0.18
    }

    // skill growth per match
    fun growthMult(s: GameState): Double = when (s.difficulty) {
        "Easy" -> 1.35
        "Hardcore" -> 0.72
        else -> 1.0
    }

    // injury frequency
    fun injuryMult(s: GameState): Double = when (s.difficulty) {
        "Easy" -> 0.6
        "Hardcore" -> 1.5
        else -> 1.0
    }

    // fame/skill needed for national call-ups
    fun selectionBar(s: GameState): Double = when (s.difficulty) {
        "Easy" -> -6.0
        "Hardcore" -> 8.0
        else -> 0.0
    }

    // opposition strength adjustment
    fun oppBump(s: GameState): Int = when (s.difficulty) {
        "Easy" -> -5
        "Hardcore" -> 6
        else -> 0
    }

    // chance of being dropped mid-season after a poor run (Hardcore only, really bites)
    fun midSeasonDropChance(s: GameState): Double = when (s.difficulty) {
        "Easy" -> 0.0
        "Hardcore" -> 0.5
        else -> 0.18
    }

    fun label(d: String): String = when (d) {
        "Easy" -> "Easy — forgiving, fast progress"
        "Hardcore" -> "Hardcore — brutal, one bad run and you're out"
        else -> "Realistic — a true cricketing life"
    }
}
