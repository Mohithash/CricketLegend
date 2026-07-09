package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.AuctionBid
import com.mohithash.cricketlegend.model.AuctionState
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.StatKey
import kotlin.math.max
import kotlin.random.Random

/**
 * Generates a bid-by-bid auction the UI can replay live. Mega auction every
 * three years puts everyone back in the pool; other years bring a retention
 * offer the player may accept or reject.
 */
object AuctionEngine {

    /** Prepares this year's auction (or retention). Sets s.pendingAuction, or leaves it null. */
    fun createSeasonAuction(s: GameState, rng: Random) {
        val t20 = s.stat(StatKey.DOM_T20)
        val league = s.stat(StatKey.LEAGUE)
        // owner-players are locked into the team they own — no auction for them
        if (s.ownedFranchise != null && s.franchiseTeam == s.ownedFranchise) {
            s.leagueSalary = 0
            return
        }
        // a teenage prodigy who dominates youth cricket earns an early auction shot (Vaibhav path)
        val prodigy = s.age in 13..16 && max(s.batting, s.bowling) >= 60
        val eligible = s.fame >= 15 || t20.matches >= 8 || league.matches > 0 || prodigy
        if (!eligible) return

        val isMega = s.year - s.lastMegaAuctionYear >= 3
        val value = playerValue(s)
        val basePrice = max(2_000_000L, value / 6)

        // retention path (non-mega years, currently contracted)
        if (!isMega && s.franchiseTeam != null) {
            val offer = (s.leagueSalary * (0.95 + rng.nextDouble() * 0.25)).toLong()
                .coerceIn(basePrice, 240_000_000L)
            s.pendingAuction = AuctionState(
                year = s.year, isMega = false, basePrice = basePrice,
                bids = generateBids(s, basePrice, value, rng),
                soldTo = null, finalPrice = 0,
                retentionTeam = s.franchiseTeam, retentionOffer = offer
            )
            return
        }

        // full auction
        val bids = generateBids(s, basePrice, value, rng)
        val soldTo = bids.lastOrNull()?.team
        val finalPrice = bids.lastOrNull()?.amount ?: 0
        s.pendingAuction = AuctionState(
            year = s.year, isMega = isMega, basePrice = basePrice,
            bids = bids, soldTo = soldTo, finalPrice = finalPrice
        )
    }

    private fun playerValue(s: GameState): Long {
        val skill = max(s.batting, s.bowling)
        val leagueStat = s.stat(StatKey.LEAGUE)
        val perfBonus = if (leagueStat.matches > 0)
            (leagueStat.strikeRate * 120_000L).toLong() + leagueStat.wickets * 900_000L else 0L
        val captainBonus = if (s.captainFranchise) 20_000_000L else 0L
        return (5_000_000L + (s.fame * 1_800_000L).toLong() + (skill * 450_000L).toLong() +
                perfBonus + captainBonus + s.followers / 300)
            .coerceAtMost(300_000_000L)
    }

    private fun generateBids(s: GameState, basePrice: Long, value: Long, rng: Random): List<AuctionBid> {
        // how many teams raise a paddle
        val heat = when {
            s.fame >= 70 -> 5 + rng.nextInt(3)
            s.fame >= 40 -> 3 + rng.nextInt(3)
            s.fame >= 15 -> 1 + rng.nextInt(3)
            else -> rng.nextInt(2)
        }
        if (heat == 0) return emptyList()

        val interested = RealData.franchises.shuffled(rng).take(heat)
        // each team quits above its own ceiling
        val ceilings = interested.associateWith {
            (value * (0.65 + rng.nextDouble() * 0.75) * (it.strength / 52.0)).toLong()
        }
        val bids = ArrayList<AuctionBid>()
        var price = basePrice
        var active = interested.toMutableList()
        var turn = rng.nextInt(active.size)
        var rounds = 0
        while (active.isNotEmpty() && rounds < 40) {
            rounds++
            val team = active[turn % active.size]
            if (ceilings.getValue(team) >= price) {
                bids.add(AuctionBid(team.name, price))
                price = (price * (1.10 + rng.nextDouble() * 0.15)).toLong()
            } else {
                active.remove(team)
                if (active.isEmpty()) break
            }
            turn++
            if (active.size == 1 && bids.isNotEmpty() && bids.last().team == active[0].name) break
        }
        return bids.coerceSalaries()
    }

    private fun List<AuctionBid>.coerceSalaries(): List<AuctionBid> =
        map { AuctionBid(it.team, it.amount.coerceAtMost(240_000_000L)) }

    /**
     * Applies the auction outcome. acceptRetention only meaningful when a
     * retention offer exists; otherwise the generated bids decide.
     */
    fun resolve(s: GameState, acceptRetention: Boolean, rng: Random) {
        val a = s.pendingAuction ?: return
        val winningBid = a.bids.lastOrNull()
        if (a.retentionTeam != null && acceptRetention) {
            s.franchiseTeam = a.retentionTeam
            s.leagueSalary = a.retentionOffer
            Finance.credit(s, "League retention — ${a.retentionTeam}", a.retentionOffer, taxable = true)
            s.addNews("${a.retentionTeam} retain ${s.playerName} (${Money.fmt(a.retentionOffer, s.country)}).")
        } else if (winningBid != null) {
            s.franchiseTeam = winningBid.team
            s.leagueSalary = winningBid.amount
            Finance.credit(s, "League auction — ${winningBid.team}", winningBid.amount, taxable = true)
            s.addNews("SOLD! ${winningBid.team} sign ${s.playerName} for ${Money.fmt(winningBid.amount, s.country)}!")
            s.captainFranchise = false // new team, back in the ranks
        } else {
            s.franchiseTeam = null
            s.leagueSalary = 0
            s.captainFranchise = false
            s.addNews("UNSOLD. No franchise raised a paddle for ${s.playerName} this year.")
            s.morale = (s.morale - 8).coerceAtLeast(5.0)
        }
        if (a.isMega) s.lastMegaAuctionYear = a.year
        s.pendingAuction = null
    }
}
