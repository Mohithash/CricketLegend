package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.data.RealData
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.model.AuctionState
import com.mohithash.cricketlegend.model.GameState

@Composable
fun AuctionScreen(s: GameState, a: AuctionState) {
    var phase by remember { mutableStateOf(if (a.retentionTeam != null) "RETENTION" else "AUCTION") }
    var revealed by remember { mutableIntStateOf(0) }

    Column(
        Modifier
            .fillMaxSize()
            .background(DeepNavy)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            if (a.isMega) "MEGA AUCTION ${a.year}" else "PLAYER AUCTION ${a.year}",
            color = GoldAccent, fontSize = 24.sp, fontWeight = FontWeight.Black,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Text(
            "${s.playerName} — ${s.role.label} · Base price ${Money.fmt(a.basePrice, s.country)}",
            color = TextDim, fontSize = 13.sp,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        if (phase == "RETENTION") {
            InfoCard {
                Text("RETENTION OFFER", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("${a.retentionTeam} want to retain you.", color = TextPrimary,
                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(Money.fmt(a.retentionOffer, s.country) + " for the season",
                    color = GoldAccent, fontSize = 22.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { Game.resolveAuction(true) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                ) { Text("Accept — stay loyal", fontWeight = FontWeight.Bold) }
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { phase = "AUCTION" },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Reject — test the market", color = LossRed) }
            }
        } else {
            InfoCard {
                Text("LIVE FROM THE AUCTION TABLE", color = TextDim, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (a.bids.isEmpty()) {
                    Text("The auctioneer calls your name…", color = TextPrimary, fontSize = 14.sp)
                    Text("Silence in the room.", color = LossRed, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    a.bids.take(revealed).forEachIndexed { i, bid ->
                        val f = RealData.franchise(bid.team)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 3.dp)
                        ) {
                            Box(
                                Modifier
                                    .size(10.dp)
                                    .background(Color(f?.colorHex ?: 0xFF888888), CircleShape)
                            )
                            Spacer(Modifier.padding(horizontal = 4.dp))
                            Text(bid.team, color = TextPrimary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text(Money.fmt(bid.amount, s.country),
                                color = if (i == revealed - 1) GoldAccent else TextDim,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                when {
                    revealed < a.bids.size -> Button(
                        onClick = { revealed++ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                    ) {
                        Text(if (revealed == 0) "Open the bidding" else "Next bid…",
                            color = GoldAccent, fontWeight = FontWeight.Bold)
                    }
                    else -> {
                        val winner = a.bids.lastOrNull()
                        if (winner != null) {
                            Text("SOLD to ${winner.team}!", color = WinGreen,
                                fontSize = 18.sp, fontWeight = FontWeight.Black)
                            Text(Money.fmt(winner.amount, s.country), color = GoldAccent,
                                fontSize = 24.sp, fontWeight = FontWeight.Black)
                        } else {
                            Text("UNSOLD", color = LossRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Text("A long year of domestic cricket awaits.", color = TextDim, fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { Game.resolveAuction(false) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                        ) { Text("Continue", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        SectionHeader("The Franchises")
        for (f in RealData.franchises) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 3.dp)) {
                Box(Modifier.size(10.dp).background(Color(f.colorHex), CircleShape))
                Spacer(Modifier.padding(horizontal = 4.dp))
                Column(Modifier.weight(1f)) {
                    Text(f.name, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${f.city} · Capt: ${f.captain} · ${f.titles} titles", color = TextDim, fontSize = 11.sp)
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}
