package com.mohithash.cricketlegend.engine

import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.model.Business
import com.mohithash.cricketlegend.model.GameState
import com.mohithash.cricketlegend.model.Holding
import com.mohithash.cricketlegend.model.Instrument
import com.mohithash.cricketlegend.model.OwnedProperty
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

object Money {
    /** India shows ₹ with Lakh/Crore, everyone else USD (~₹83 = $1). */
    fun fmt(amount: Long, country: String): String {
        val neg = amount < 0
        val sign = if (neg) "-" else ""
        return if (country == "India") {
            val a = abs(amount)
            sign + when {
                a >= 1_00_00_000 -> "₹%.2f Cr".format(a / 1_00_00_000.0)
                a >= 1_00_000 -> "₹%.1f L".format(a / 1_00_000.0)
                a >= 1_000 -> "₹%.0fK".format(a / 1_000.0)
                else -> "₹$a"
            }
        } else {
            val usd = abs(amount) / 83.0
            sign + when {
                usd >= 1_000_000 -> "$%.2fM".format(usd / 1_000_000.0)
                usd >= 1_000 -> "$%.0fK".format(usd / 1_000.0)
                else -> "$%.0f".format(usd)
            }
        }
    }
}

object Finance {

    const val TAX_RATE = 0.30

    /** Credits cricket income, applying tax when applicable. */
    fun credit(s: GameState, label: String, gross: Long, taxable: Boolean) {
        if (gross <= 0) return
        if (taxable) {
            val net = (gross * (1.0 - TAX_RATE)).toLong()
            s.addLedger("$label (after tax)", net)
        } else {
            s.addLedger(label, gross)
        }
    }

    fun weeklyIncome(s: GameState): Long {
        var inc = ((s.contractYearly + s.endorsements.sumOf { it.yearlyValue }) * (1.0 - TAX_RATE)).toLong() / 52
        for (op in s.ownedProperties) {
            val market = s.propertyMarket.firstOrNull { it.id == op.id } ?: continue
            inc += (market.price * market.rentYieldPct / 100.0 / 52.0).toLong()
        }
        inc += s.businesses.sumOf { it.weeklyProfit }
        return inc
    }

    fun weeklyExpenses(s: GameState): Long {
        var exp = 0L
        for (opt in RealData.staffOptions) {
            val tier = s.staff[opt.id] ?: -1
            if (tier >= 0) exp += opt.weeklyCost[tier.coerceIn(0, opt.weeklyCost.size - 1)]
        }
        for (itemId in s.ownedItems) {
            exp += RealData.lifestyleItems.firstOrNull { it.id == itemId }?.weeklyUpkeep ?: 0
        }
        // minors live with family — negligible personal costs until they turn pro at 16
        if (s.age < 16) return exp
        exp += 25_000 + (s.fame * 2_000).toLong()          // celebrity cost of living
        exp += (s.kids * 40_000L) + (if (s.married) 30_000L else 0L)
        return exp
    }

    fun processWeeks(s: GameState, weeks: Int, rng: Random = Random.Default) {
        if (weeks <= 0) return
        val inc = weeklyIncome(s) * weeks
        val exp = weeklyExpenses(s) * weeks
        if (inc > 0) s.addLedger("Income x$weeks wk (net of tax)", inc)
        if (exp > 0) s.addLedger("Expenses x$weeks wk (staff, upkeep, living)", -exp)
        updateMarket(s, weeks, rng)
    }

    // ---------------- Investments ----------------

    fun initMarket(s: GameState) {
        if (s.marketInstruments.isNotEmpty()) return
        s.marketInstruments.addAll(RealData.instrumentDefs.map {
            Instrument(it.id, it.name, it.kind, it.startPrice, it.drift, it.vol)
        })
    }

    private fun gaussian(rng: Random): Double =
        (rng.nextDouble() + rng.nextDouble() + rng.nextDouble() - 1.5) * 2.0

    fun updateMarket(s: GameState, weeks: Int, rng: Random) {
        for (inst in s.marketInstruments) {
            repeat(weeks) {
                inst.price = (inst.price * (1.0 + inst.drift + inst.vol * gaussian(rng))).coerceAtLeast(0.5)
            }
            inst.history.add(inst.price)
            while (inst.history.size > 104) inst.history.removeAt(0)
        }
    }

    fun buyInstrument(s: GameState, id: String, amount: Long): Boolean {
        val inst = s.marketInstruments.firstOrNull { it.id == id } ?: return false
        if (amount <= 0 || s.money < amount) return false
        val h = s.holdings.firstOrNull { it.id == id }
            ?: Holding(id, 0.0, 0).also { s.holdings.add(it) }
        h.units += amount / inst.price
        h.invested += amount
        s.addLedger("Invested in ${inst.name}", -amount)
        return true
    }

    fun sellInstrument(s: GameState, id: String): Boolean {
        val inst = s.marketInstruments.firstOrNull { it.id == id } ?: return false
        val h = s.holdings.firstOrNull { it.id == id } ?: return false
        val proceeds = (h.units * inst.price).toLong()
        s.holdings.remove(h)
        val gain = proceeds - h.invested
        val net = if (gain > 0) proceeds - (gain * 0.10).toLong() else proceeds   // 10% capital gains
        s.addLedger("Sold ${inst.name}" + if (gain > 0) " (10% CGT on gains)" else "", net)
        return true
    }

    fun holdingValue(s: GameState, h: Holding): Long {
        val inst = s.marketInstruments.firstOrNull { it.id == h.id } ?: return 0
        return (h.units * inst.price).toLong()
    }

    fun portfolioValue(s: GameState): Long = s.holdings.sumOf { holdingValue(s, it) }

    // ---------------- Businesses ----------------

    fun startBusiness(s: GameState, defId: String, rng: Random = Random.Default): Boolean {
        val def = RealData.businessDefs.firstOrNull { it.id == defId } ?: return false
        if (s.businesses.any { it.id == defId } || s.money < def.cost || s.fame < def.minFame) return false
        val profit = (def.meanWeekly * (0.6 + rng.nextDouble() * 0.7)).toLong()
        s.businesses.add(Business(def.id, def.name, def.cost, profit, s.year))
        s.addLedger("Launched ${def.name}", -def.cost)
        s.addNews("Entrepreneur mode: ${s.playerName} launches ${def.name}!")
        s.fame = (s.fame + 1.5).coerceAtMost(100.0)
        return true
    }

    fun yearlyBusinessReview(s: GameState, rng: Random) {
        val iter = s.businesses.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            if (rng.nextDouble() < RealData.businessDefs.firstOrNull { it.id == b.id }?.flopChancePerYear ?: 0.1) {
                iter.remove()
                s.addNews("Business trouble: ${b.name} shuts down after a rough year.")
                s.morale = (s.morale - 5).coerceAtLeast(5.0)
            } else {
                b.weeklyProfit = (b.weeklyProfit * (0.9 + rng.nextDouble() * 0.28)).toLong()
            }
        }
    }

    // ---------------- Property ----------------

    fun buyProperty(s: GameState, propertyId: String): Boolean {
        val p = s.propertyMarket.firstOrNull { it.id == propertyId } ?: return false
        if (s.ownedProperties.any { it.id == propertyId } || s.money < p.price) return false
        s.addLedger("Bought ${p.name}, ${p.city}", -p.price)
        s.ownedProperties.add(OwnedProperty(p.id, p.price, s.year))
        s.addNews("New asset: ${p.name} in ${p.city}.")
        return true
    }

    fun sellProperty(s: GameState, propertyId: String): Boolean {
        val owned = s.ownedProperties.firstOrNull { it.id == propertyId } ?: return false
        val market = s.propertyMarket.firstOrNull { it.id == propertyId } ?: return false
        val proceeds = (market.price * 0.97).toLong()   // 3% transaction cost
        s.ownedProperties.remove(owned)
        s.addLedger("Sold ${market.name}, ${market.city}", proceeds)
        return true
    }

    // ---------------- Lifestyle & staff ----------------

    fun buyItem(s: GameState, itemId: String): Boolean {
        val item = RealData.lifestyleItems.firstOrNull { it.id == itemId } ?: return false
        if (itemId in s.ownedItems || s.money < item.price) return false
        s.addLedger("Bought ${item.name}", -item.price)
        s.ownedItems.add(itemId)
        s.fame = (s.fame + item.fameBoost).coerceAtMost(100.0)
        s.morale = (s.morale + item.moraleBoost).coerceAtMost(100.0)
        if (s.partner != null) s.relationship = (s.relationship + 1.5).coerceAtMost(100.0)
        return true
    }

    fun hireStaff(s: GameState, staffId: String, tier: Int): Boolean {
        val opt = RealData.staffOptions.firstOrNull { it.id == staffId } ?: return false
        if (tier !in opt.tiers.indices) return false
        s.staff[staffId] = tier
        s.addNews("Hired ${opt.tiers[tier]} as ${opt.name}.")
        return true
    }

    // ---------------- Family ----------------

    fun familyGift(s: GameState): Boolean {
        if (s.partner == null || s.money < 1_000_000) return false
        s.addLedger("Gift for ${s.partner}", -1_000_000)
        s.relationship = (s.relationship + 6).coerceAtMost(100.0)
        s.morale = (s.morale + 2).coerceAtMost(100.0)
        return true
    }

    fun familyVacation(s: GameState): Boolean {
        if (s.partner == null || s.money < 5_000_000) return false
        s.addLedger("Family holiday", -5_000_000)
        s.relationship = (s.relationship + 15).coerceAtMost(100.0)
        s.morale = (s.morale + 6).coerceAtMost(100.0)
        s.form = (s.form - 0.2).coerceAtLeast(-5.0)
        return true
    }
}
