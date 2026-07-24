package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.GameState
import kotlin.random.Random

/**
 * Simulates the REST of world cricket each season — bilateral tours, World Cup
 * qualifiers and (when you're not in them) the World Cups themselves.
 * Team strength is computed from the actual skills of the players in the living
 * database, so squads with better players genuinely win more.
 */
object WorldFixtures {

    /** Player-skill-driven team strength: the average of the nation's best 15. */
    fun teamStrength(s: GameState, country: String): Double {
        val best = s.rivals.filter { !it.retired && it.country == country }
            .sortedByDescending { it.skill }.take(15)
        val base = RealData.teams.firstOrNull { it.name == country }?.strength?.toDouble() ?: 60.0
        if (best.isEmpty()) return base
        // players are decisive: 70% squad skill, 30% institutional strength
        return best.map { it.skill }.average() * 0.7 + base * 0.3
    }

    private fun seriesResult(a: String, sa: Double, b: String, sb: Double, games: Int, rng: Random): Triple<Int, Int, String> {
        var wa = 0; var wb = 0
        repeat(games) {
            val p = (0.5 + (sa - sb) / 60.0).coerceIn(0.12, 0.88)
            if (rng.nextDouble() < p) wa++ else wb++
        }
        val txt = if (wa == wb) "$a $wa–$wb $b (drawn series)"
        else "${if (wa > wb) a else b} won ${maxOf(wa, wb)}–${minOf(wa, wb)}"
        return Triple(wa, wb, txt)
    }

    /** Called at season end: fills s.worldLog with the season's world results. */
    fun simulateWorldSeason(s: GameState, rng: Random) {
        s.worldLog.clear()
        val nations = RealData.teams.map { it.name }
        val str = nations.associateWith { teamStrength(s, it) }

        // ── bilateral tours: everyone plays; player skills decide ──
        val shuffled = nations.shuffled(rng)
        var i = 0
        while (i + 1 < shuffled.size) {
            val a = shuffled[i]; val b = shuffled[i + 1]
            val fmt = listOf("T20I", "ODI", "Test").random(rng)
            val games = if (fmt == "Test") 3 else 3 + rng.nextInt(3)
            val (_, _, txt) = seriesResult(a, str[a]!!, b, str[b]!!, games, rng)
            s.worldLog.add("$fmt tour: $a v $b — $txt")
            i += 2
        }

        // ── World Cup qualifiers (WC years): 8 strongest auto-qualify, rest fight for 2 spots ──
        val t20WcYear = s.year % 2 == 0
        val odiWcYear = (s.year - 2027) % 4 == 0 && s.year >= 2027
        if (t20WcYear || odiWcYear) {
            val cup = if (t20WcYear) "T20 World Cup" else "ODI World Cup"
            val ranked = nations.sortedByDescending { str[it]!! }
            val auto = ranked.take(8)
            val contenders = ranked.drop(8)
            if (contenders.size >= 2) {
                // round-robin qualifier — again, squad skill decides
                val pts = contenders.associateWith { 0 }.toMutableMap()
                for (x in contenders.indices) for (y in x + 1 until contenders.size) {
                    val a = contenders[x]; val b = contenders[y]
                    val p = (0.5 + (str[a]!! - str[b]!!) / 60.0).coerceIn(0.15, 0.85)
                    if (rng.nextDouble() < p) pts[a] = pts[a]!! + 2 else pts[b] = pts[b]!! + 2
                }
                val q = pts.entries.sortedByDescending { it.value }.take(2).map { it.key }
                s.worldLog.add("$cup Qualifier: ${q[0]} and ${q[1]} qualify! " +
                    "(${contenders.size}-team round robin; heartbreak for ${pts.entries.minByOrNull { it.value }?.key})")
                // full WC sim only when the player isn't playing in it (avoid contradicting their run)
                val playerIn = (t20WcYear && s.inNationalT20) || (odiWcYear && s.inNationalODI)
                if (!playerIn) {
                    val field = (auto + q)
                    val table = field.associateWith { 0 }.toMutableMap()
                    for (x in field.indices) for (y in x + 1 until field.size) {
                        val a = field[x]; val b = field[y]
                        val p = (0.5 + (str[a]!! - str[b]!!) / 60.0).coerceIn(0.12, 0.88)
                        if (rng.nextDouble() < p) table[a] = table[a]!! + 2 else table[b] = table[b]!! + 2
                    }
                    val semis = table.entries.sortedByDescending { it.value }.take(4).map { it.key }
                    fun ko(a: String, b: String): String {
                        val p = (0.5 + (str[a]!! - str[b]!!) / 60.0).coerceIn(0.2, 0.8)
                        return if (rng.nextDouble() < p) a else b
                    }
                    val f1 = ko(semis[0], semis[3]); val f2 = ko(semis[1], semis[2])
                    val champ = ko(f1, f2)
                    s.worldLog.add("$cup ${s.year}: $champ beat ${if (champ == f1) f2 else f1} in the final. " +
                        "Semis: ${semis.joinToString(", ")}.")
                    s.addNews("$cup: $champ are world champions.")
                }
            }
        }

        // ── continental / A-team tours for flavour ──
        val aTour = nations.random(rng)
        s.worldLog.add("$aTour A toured with a squad of rising stars — " +
            (s.rivals.filter { it.country == aTour && it.age <= 23 }.maxByOrNull { it.skill }?.name
                ?: "a local prodigy") + " impressed the scouts.")
        s.worldLog.add("World Test Championship table reshuffled after the season's tours.")
    }
}
