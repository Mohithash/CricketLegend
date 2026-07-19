package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.model.GameState
import kotlin.random.Random

/**
 * Continuous weekly-budget system. Instead of one-off "hire coach" / "post on
 * social" buttons, the player drags sliders that set an ongoing ₹/week spend per
 * category. Every simulated week the money is spent and the effect applied in
 * proportion to spend — a living dial that trades cash for performance.
 */
object Allocations {

    data class Cat(
        val id: String,
        val label: String,
        val blurb: String,
        val maxWeekly: Long,
        val emoji: String
    )

    val categories = listOf(
        Cat("coaching", "Coaching", "Specialist batting/bowling coaching accelerates skill growth", 400_000, "🎯"),
        Cat("fitness", "Fitness & Physio", "S&C and physios build fitness, cut injuries, sharpen you faster", 350_000, "💪"),
        Cat("nutrition", "Nutrition & Recovery", "Diet, sleep science and recovery keep you fresh and fit", 200_000, "🥗"),
        Cat("analytics", "Analytics & Prep", "Opposition analysis and match prep give you an on-field edge", 300_000, "📊"),
        Cat("pr", "PR & Social Media", "A media team grows your following, fame and public image", 500_000, "📱"),
        Cat("lifestyle", "Lifestyle & Downtime", "Comfort, travel and downtime keep morale high", 250_000, "🛋️")
    )

    fun get(s: GameState, id: String): Long = s.allocations[id] ?: 0L

    fun set(s: GameState, id: String, weekly: Long) {
        val cat = categories.firstOrNull { it.id == id } ?: return
        s.allocations[id] = weekly.coerceIn(0L, cat.maxWeekly)
    }

    /** Total committed weekly spend across all dials. */
    fun weeklyTotal(s: GameState): Long = categories.sumOf { get(s, it.id) }

    /** 0..1 intensity of a dial (spend / max). */
    fun level(s: GameState, id: String): Double {
        val cat = categories.firstOrNull { it.id == id } ?: return 0.0
        return (get(s, id).toDouble() / cat.maxWeekly).coerceIn(0.0, 1.0)
    }

    /**
     * Applies [weeks] of allocation effects. Called from Finance.processWeeks AFTER
     * the spend has been debited. Returns nothing; mutates state.
     */
    fun applyWeekly(s: GameState, weeks: Int, rng: Random) {
        if (weeks <= 0) return
        val w = weeks.toDouble()

        // PR & social — helps, but can't manufacture stardom; on-field fame drives most of it
        val pr = level(s, "pr")
        if (pr > 0) {
            // a media team amplifies a following you're already earning; modest on its own
            LifeSystems.gainFollowers(s, (pr * w * (800 + s.fame * 220)).toLong())
            s.fame = (s.fame + pr * w * 0.08).coerceAtMost(100.0)
            s.publicImage = (s.publicImage + pr * w * 0.12).coerceIn(-50.0, 50.0)
        } else {
            // neglect it and you slowly fade from the public eye
            s.followers = (s.followers * (1.0 - 0.003 * w)).toLong()
        }

        // lifestyle — morale upkeep
        val life = level(s, "lifestyle")
        s.morale = (s.morale + (life - 0.35) * w * 0.6).coerceIn(5.0, 100.0)

        // nutrition & fitness — fitness + faster sharpness recovery + relationship time cost is elsewhere
        val fit = level(s, "fitness")
        val nut = level(s, "nutrition")
        s.fitness = (s.fitness + (fit * 0.10 + nut * 0.06 - 0.02) * w).coerceIn(20.0, 99.0)
        s.sharpness = (s.sharpness + (fit * 2.0 + nut * 2.5) * w).coerceAtMost(100.0)
    }

    /** Coaching multiplier applied to skill growth (1.0 = no coaching, up to ~2.4). */
    fun coachingMult(s: GameState): Double = 1.0 + level(s, "coaching") * 1.4

    /** Injury-risk multiplier from fitness/nutrition spend (lower = safer). */
    fun injuryReduction(s: GameState): Double =
        (1.0 - level(s, "fitness") * 0.45 - level(s, "nutrition") * 0.20).coerceAtLeast(0.35)

    /** Analytics edge — shaves the opposition's effective strength in the match engine. */
    fun analyticsEdge(s: GameState): Double = level(s, "analytics") * 6.0

    /** Endorsement-value multiplier from a strong media presence. */
    fun prDealMult(s: GameState): Double = 1.0 + level(s, "pr") * 0.6

    /** Sensible starting dials for a new career (modest, affordable). */
    fun seedDefaults(s: GameState) {
        if (s.allocations.isNotEmpty()) return
        set(s, "coaching", 40_000)
        set(s, "fitness", 40_000)
        set(s, "nutrition", 20_000)
        set(s, "analytics", 0)
        set(s, "pr", 20_000)
        set(s, "lifestyle", 30_000)
    }
}
