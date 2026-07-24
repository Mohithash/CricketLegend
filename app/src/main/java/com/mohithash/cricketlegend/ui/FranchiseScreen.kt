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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.engine.FranchiseEngine
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.model.FranchiseGame
import com.mohithash.cricketlegend.model.SquadPlayer

@Composable
fun FranchiseScreen(g: FranchiseGame) {
    val over = g.won || g.bankrupt || g.sacked
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
    ) {
        HeroHeader(
            name = g.teamName,
            subtitle = "${g.city} · Season ${g.year} · Str ${g.squadStrength()}",
            status = if (over) endLabel(g) else "Owner-Manager",
            chips = buildList {
                add((if (g.debt > 0) "DEBT " + Money.fmt(g.debt, "India") else "DEBT-FREE") to (if (g.debt > 0) BallRed else WinGreen))
                add("${g.titles}🏆" to GoldAccent)
            },
            rightValue = Money.fmt(g.cash, "India")
        )
        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            StatTileGrid(listOf(
                Triple("Cash", crShort(g.cash), if (g.cash >= 0) WinGreen else LossRed),
                Triple("Debt", crShort(g.debt), if (g.debt > 0) BallRed else WinGreen),
                Triple("Fans %", "${g.fanHappiness.toInt()}", if (g.fanHappiness > 45) WinGreen else LossRed),
                Triple("Board %", "${g.boardConfidence.toInt()}", if (g.boardConfidence > 40) WinGreen else LossRed),
                Triple("Wage bill", crShort(g.salaryBill), GoldAccent),
                Triple("Titles", "${g.titles}", GoldAccent)
            ))

            InfoCard {
                Text("BOARD MANDATE", color = TextDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(g.boardTarget, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (g.lastSeasonReport.isNotEmpty())
                    Text(g.lastSeasonReport, color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
            }

            if (over) {
                InfoCard {
                    Text(endLabel(g), color = if (g.won) GoldAccent else LossRed,
                        fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text(endBlurb(g), color = TextPrimary, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { Game.abandonFranchise() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Exit to Menu", fontWeight = FontWeight.Bold)
                    }
                }
                return@Column
            }

            when (g.phase) {
                "AUCTION" -> AuctionPhase(g)
                else -> ManagePhase(g)
            }

            SectionHeader("Facilities")
            FranchiseEngine.facilities.forEach { f ->
                val lvl = FranchiseEngine.facilityLevel(g, f.id)
                val cost = FranchiseEngine.upgradeCost(lvl)
                InfoCard {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${f.emoji} ${f.label} — Lv $lvl", color = TextPrimary,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(f.blurb, color = TextDim, fontSize = 10.sp)
                        }
                        if (lvl < 8) {
                            Button(
                                onClick = { Game.frUpgrade(f.id) },
                                enabled = g.cash >= cost,
                                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) { Text(crShort(cost), fontSize = 11.sp) }
                        } else Text("MAX", color = GoldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            SectionHeader("Club News")
            InfoCard {
                if (g.news.isEmpty()) Text("Quiet at the club.", color = TextDim)
                g.news.take(14).forEach {
                    Text("• $it", color = TextPrimary, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Game.abandonFranchise() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quit Franchise Career", color = TextDim, fontSize = 12.sp)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AuctionPhase(g: FranchiseGame) {
    SectionHeader("Auction — Purse ${Money.fmt(g.purse, "India")}")
    InfoCard {
        Text("Build your squad, then start the season. Marquee stars win titles but bloat the wage bill while you're in debt.",
            color = TextDim, fontSize = 11.sp)
    }
    g.auctionPool.sortedByDescending { it.rating }.forEach { p ->
        val idx = g.auctionPool.indexOf(p)
        val price = FranchiseEngine.playerPrice(p)
        InfoCard {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                PlayerLine(p, Modifier.weight(1f))
                Button(
                    onClick = { Game.frSign(idx) },
                    enabled = g.purse >= price && g.squad.size < 25,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) { Text(crShort(price), fontSize = 11.sp, color = DeepNavy, fontWeight = FontWeight.Bold) }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
    Button(
        onClick = { Game.frStartSeason() },
        modifier = Modifier.fillMaxWidth().height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PitchGreen),
        enabled = g.squad.size >= 11
    ) { Text("LOCK SQUAD & MANAGE ▶", fontWeight = FontWeight.Black, color = TextPrimary) }

    SquadList(g, releasable = true)
}

@Composable
private fun ManagePhase(g: FranchiseGame) {
    SectionHeader("Pre-Season")
    InfoCard {
        Text("Invest in facilities and repay debt, then simulate the campaign.", color = TextDim, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(10L, 40L, 100L).forEach { cr ->
                val amt = cr * 10_000_000L
                OutlinedButton(
                    onClick = { Game.frPayDebt(amt) },
                    enabled = g.cash > 0 && g.debt > 0,
                    modifier = Modifier.weight(1f),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                ) { Text("Repay ₹${cr}Cr", fontSize = 10.sp, color = GoldAccent) }
            }
        }
    }
    Spacer(Modifier.height(6.dp))
    Button(
        onClick = { Game.frReopenAuction() },
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
    ) { Text("↩ Back to Auction", color = GoldAccent, fontSize = 12.sp) }
    Spacer(Modifier.height(4.dp))
    Button(
        onClick = { Game.frSimSeason() },
        modifier = Modifier.fillMaxWidth().height(52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
    ) { Text("▶ SIMULATE SEASON", fontWeight = FontWeight.Black, color = DeepNavy, fontSize = 16.sp) }

    if (g.standings.isNotEmpty()) {
        SectionHeader("Last Table")
        InfoCard {
            g.standings.entries.sortedByDescending { it.value }.forEachIndexed { i, (team, w) ->
                val mine = team == g.teamName
                Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                    Text("${i + 1}. $team", color = if (mine) GoldAccent else TextPrimary,
                        fontSize = 11.sp, fontWeight = if (mine) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.weight(1f), maxLines = 1)
                    Text("$w W", color = TextDim, fontSize = 11.sp)
                }
            }
        }
    }

    SquadList(g, releasable = true)
}

@Composable
private fun SquadList(g: FranchiseGame, releasable: Boolean) {
    SectionHeader("Squad (${g.squad.size}/25) · Strength ${g.squadStrength()}")
    InfoCard {
        FranchiseEngine.squadByRole(g).forEach { p ->
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 1.dp)) {
                PlayerLine(p, Modifier.weight(1f))
                if (releasable && g.squad.size > 11) {
                    OutlinedButton(
                        onClick = { Game.frRelease(p.name) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) { Text("✕", color = LossRed, fontSize = 11.sp) }
                }
            }
        }
    }
}

@Composable
private fun PlayerLine(p: SquadPlayer, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        // rating badge
        Text("${p.rating}", color = ratingColor(p.rating), fontWeight = FontWeight.Black,
            fontSize = 14.sp, modifier = Modifier.width(30.dp))
        Column(Modifier.weight(1f)) {
            Text((if (p.overseas) "✈ " else "") + p.name, color = TextPrimary, fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text("${roleLabel(p.role)} · age ${p.age} · ${Money.fmt(p.salary, "India")}/yr",
                color = TextDim, fontSize = 9.sp, maxLines = 1)
        }
    }
}

private fun ratingColor(r: Int) = when {
    r >= 85 -> GoldAccent; r >= 72 -> WinGreen; r >= 60 -> Teal; r >= 50 -> TextPrimary; else -> TextDim
}
private fun roleLabel(r: String) = when (r) { "BAT" -> "Batter"; "BOWL" -> "Bowler"; "AR" -> "All-rounder"; else -> "Keeper" }
private fun crShort(v: Long): String {
    val cr = v / 10_000_000.0
    return (if (v < 0) "-" else "") + "₹%.0fCr".format(kotlin.math.abs(cr))
}
private fun endLabel(g: FranchiseGame) = when { g.won -> "TURNAROUND COMPLETE"; g.bankrupt -> "BANKRUPT"; else -> "SACKED" }
private fun endBlurb(g: FranchiseGame) = when {
    g.won -> "You cleared the debt and won ${g.titles} title(s). A legendary rescue job."
    g.bankrupt -> "The money ran out. The club couldn't be saved."
    else -> "The board lost patience. Someone else gets the rebuild."
}
