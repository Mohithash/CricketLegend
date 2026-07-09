package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Finance
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.model.GameState

@Composable
fun MoneyScreen(s: GameState, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Wallet", fontSize = 11.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Invest", fontSize = 11.sp) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Property", fontSize = 11.sp) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Deals", fontSize = 11.sp) })
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> WalletTab(s)
                1 -> InvestTab(s)
                2 -> PropertyTab(s)
                else -> DealsTab(s)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun WalletTab(s: GameState) {
    InfoCard {
        Text("Balance", color = TextDim, fontSize = 12.sp)
        Text(Money.fmt(s.money, s.country), color = GoldAccent, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        KeyValueRow("Weekly income", "+" + Money.fmt(Finance.weeklyIncome(s), s.country), WinGreen)
        KeyValueRow("Weekly expenses", "-" + Money.fmt(Finance.weeklyExpenses(s), s.country), LossRed)
        s.contractGrade?.let {
            KeyValueRow("Central contract (Grade $it)", Money.fmt(s.contractYearly, s.country) + "/yr")
        }
        s.franchiseTeam?.let {
            KeyValueRow("League deal — $it", Money.fmt(s.leagueSalary, s.country) + "/season")
        }
    }
    SectionHeader("Transactions")
    InfoCard {
        if (s.ledger.isEmpty()) Text("No transactions yet.", color = TextDim)
        s.ledger.take(30).forEach { e ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(e.label, color = TextPrimary, fontSize = 12.sp)
                    Text("${e.year} · Week ${e.week}", color = TextDim, fontSize = 10.sp)
                }
                Text(
                    (if (e.amount >= 0) "+" else "") + Money.fmt(e.amount, s.country),
                    color = if (e.amount >= 0) WinGreen else LossRed,
                    fontSize = 12.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun InvestTab(s: GameState) {
    SectionHeader("Your Portfolio")
    InfoCard {
        val total = Finance.portfolioValue(s)
        KeyValueRow("Portfolio value", Money.fmt(total, s.country), GoldAccent)
        if (s.holdings.isEmpty()) {
            Text("Money in the bank earns nothing. Put it to work.", color = TextDim, fontSize = 12.sp)
        }
    }
    for (h in s.holdings) {
        val inst = s.marketInstruments.firstOrNull { it.id == h.id } ?: continue
        val value = Finance.holdingValue(s, h)
        val gain = value - h.invested
        InfoCard {
            Text(inst.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            KeyValueRow("Invested", Money.fmt(h.invested, s.country))
            KeyValueRow("Current value", Money.fmt(value, s.country), if (gain >= 0) WinGreen else LossRed)
            KeyValueRow("P/L", (if (gain >= 0) "+" else "") + Money.fmt(gain, s.country),
                if (gain >= 0) WinGreen else LossRed)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { Game.sellInstrument(h.id) },
                colors = ButtonDefaults.buttonColors(containerColor = BallRed)
            ) { Text("Sell all (10% CGT on gains)", fontSize = 12.sp) }
        }
    }

    SectionHeader("The Market")
    for (inst in s.marketInstruments) {
        val prev = inst.history.dropLast(1).lastOrNull() ?: inst.price
        val weekChange = if (prev > 0) (inst.price - prev) / prev * 100 else 0.0
        InfoCard {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(inst.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Text(inst.kind, color = TextDim, fontSize = 11.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("₹%.2f".format(inst.price), color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text("%+.1f%%".format(weekChange),
                        color = if (weekChange >= 0) WinGreen else LossRed, fontSize = 11.sp)
                }
            }
            if (inst.history.size >= 2) {
                LineChart(
                    inst.history.map { it.toInt() },
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    color = if (weekChange >= 0) WinGreen else LossRed
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (amt in listOf(1_000_000L, 10_000_000L, 100_000_000L)) {
                    Button(
                        onClick = { Game.buyInstrument(inst.id, amt) },
                        enabled = s.money >= amt,
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                    ) { Text("+" + Money.fmt(amt, s.country), color = GoldAccent, fontSize = 11.sp) }
                }
            }
        }
    }

    SectionHeader("Businesses")
    for (b in s.businesses) {
        InfoCard {
            Text(b.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            KeyValueRow("Invested (${b.startYear})", Money.fmt(b.invested, s.country))
            KeyValueRow("Weekly profit", "+" + Money.fmt(b.weeklyProfit, s.country), WinGreen)
        }
    }
    for (def in RealData.businessDefs) {
        if (s.businesses.any { it.id == def.id }) continue
        InfoCard {
            Text(def.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            KeyValueRow("Cost", Money.fmt(def.cost, s.country), GoldAccent)
            KeyValueRow("Est. weekly profit", "~" + Money.fmt(def.meanWeekly, s.country))
            KeyValueRow("Fame required", "${def.minFame}",
                if (s.fame >= def.minFame) WinGreen else LossRed)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { Game.startBusiness(def.id) },
                enabled = s.money >= def.cost && s.fame >= def.minFame,
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
            ) { Text("Launch", fontSize = 12.sp) }
        }
    }
}

@Composable
private fun PropertyTab(s: GameState) {
    SectionHeader("Your Portfolio")
    if (s.ownedProperties.isEmpty()) {
        InfoCard { Text("You don't own any property yet. Rent flows weekly once you do.", color = TextDim, fontSize = 13.sp) }
    }
    for (op in s.ownedProperties) {
        val market = s.propertyMarket.firstOrNull { it.id == op.id } ?: continue
        val gain = market.price - op.boughtPrice
        InfoCard {
            Text("${market.name} · ${market.city}", fontWeight = FontWeight.Bold, color = TextPrimary)
            KeyValueRow("Bought (${op.boughtYear})", Money.fmt(op.boughtPrice, s.country))
            KeyValueRow("Market value", Money.fmt(market.price, s.country),
                if (gain >= 0) WinGreen else LossRed)
            KeyValueRow("Rent yield", "%.1f%%/yr".format(market.rentYieldPct))
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { Game.sellProperty(op.id) },
                colors = ButtonDefaults.buttonColors(containerColor = BallRed)
            ) { Text("Sell (3% fee)") }
        }
    }
    SectionHeader("On The Market")
    for (p in s.propertyMarket) {
        if (s.ownedProperties.any { it.id == p.id }) continue
        InfoCard {
            Text("${p.name} · ${p.city}", fontWeight = FontWeight.Bold, color = TextPrimary)
            KeyValueRow("Price", Money.fmt(p.price, s.country), GoldAccent)
            KeyValueRow("Rent yield", "%.1f%%/yr".format(p.rentYieldPct))
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { Game.buyProperty(p) },
                enabled = s.money >= p.price,
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
            ) { Text(if (s.money >= p.price) "Buy" else "Can't afford") }
        }
    }
}

@Composable
private fun DealsTab(s: GameState) {
    SectionHeader("Endorsements")
    if (s.endorsements.isEmpty()) {
        InfoCard {
            Text("No brand deals yet.", color = TextDim)
            Text("Grow your fame — offers arrive as events between matches. A better agent gets bigger deals.",
                color = TextDim, fontSize = 12.sp)
        }
    }
    for (deal in s.endorsements) {
        InfoCard {
            Text(deal.brand, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(deal.category, color = TextDim, fontSize = 12.sp)
            KeyValueRow("Value", Money.fmt(deal.yearlyValue, s.country) + "/yr", GoldAccent)
            KeyValueRow("Years left", "${deal.yearsLeft}")
        }
    }
}
