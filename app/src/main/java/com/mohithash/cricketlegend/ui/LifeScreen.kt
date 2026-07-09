package com.mohithash.cricketlegend.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.Game
import com.mohithash.cricketlegend.data.RealData
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import com.mohithash.cricketlegend.engine.Money
import com.mohithash.cricketlegend.model.GameState

@Composable
fun LifeScreen(s: GameState, modifier: Modifier = Modifier) {
    var tab by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Train", fontSize = 10.sp) })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Shop", fontSize = 10.sp) })
            Tab(selected = tab == 2, onClick = { tab = 2 }, text = { Text("Garage", fontSize = 10.sp) })
            Tab(selected = tab == 3, onClick = { tab = 3 }, text = { Text("Family", fontSize = 10.sp) })
            Tab(selected = tab == 4, onClick = { tab = 4 }, text = { Text("Empire", fontSize = 10.sp) })
        }
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            when (tab) {
                0 -> TrainingTab(s)
                1 -> ShopTab(s)
                2 -> GarageTab(s)
                3 -> FamilyTab(s)
                else -> EmpireTab(s)
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TrainingTab(s: GameState) {
    SectionHeader("Training Focus")
    InfoCard {
        Text("Where does the extra work go each week?", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        listOf("Balanced", "Batting", "Bowling", "Fitness", "Power-hitting", "Playing spin", "Playing pace").forEach { focus ->
            Button(
                onClick = { Game.setTrainingFocus(focus) },
                modifier = Modifier
                    .padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (s.trainingFocus == focus) PitchGreen else DeepNavy
                )
            ) { Text(focus, color = TextPrimary, fontSize = 13.sp) }
        }
    }

    SectionHeader("Playing Style")
    InfoCard {
        Text("Your identity at the crease.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("Aggressive", "Balanced", "Defensive").forEach { st ->
                Button(
                    onClick = { Game.setPlaystyle(st) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (s.playstyle == st) PitchGreen else DeepNavy
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(2.dp)
                ) { Text(st, color = TextPrimary, fontSize = 11.sp) }
            }
        }
        Text(
            when (s.playstyle) {
                "Aggressive" -> "Vaibhav mode: more boundaries, more dismissals. Big scores or bust."
                "Defensive" -> "Wall mode: fewer boundaries but you bat time. Averages over strike rate."
                else -> "A complete game with no glaring weakness."
            },
            color = TextDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)
        )
    }

    SectionHeader("Batting Position")
    InfoCard {
        Text("Openers face the most balls; finishers walk into chaos.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row {
            (1..8).forEach { p ->
                Button(
                    onClick = { Game.setBattingPosition(p) },
                    modifier = Modifier.weight(1f).padding(horizontal = 1.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (s.battingPosition == p) PitchGreen else DeepNavy
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) { Text("$p", color = TextPrimary, fontSize = 12.sp) }
            }
        }
    }

    SectionHeader("Invest In Yourself — Support Staff")
    for (opt in RealData.staffOptions) {
        val currentTier = s.staff[opt.id] ?: -1
        InfoCard {
            Text(opt.name, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(opt.description, color = TextDim, fontSize = 12.sp)
            if (currentTier >= 0) {
                KeyValueRow("Current", opt.tiers[currentTier], WinGreen)
            }
            Spacer(Modifier.height(6.dp))
            opt.tiers.forEachIndexed { i, tierName ->
                if (i > currentTier) {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Button(
                            onClick = { Game.hireStaff(opt.id, i) },
                            colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
                        ) {
                            Text(
                                "$tierName — ${Money.fmt(opt.weeklyCost[i], s.country)}/wk",
                                color = GoldAccent, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopTab(s: GameState) {
    val categories = RealData.lifestyleItems.groupBy { it.category }
    for ((cat, items) in categories) {
        SectionHeader(cat + "s")
        for (item in items) {
            if (item.id in s.ownedItems) continue
            InfoCard {
                Text(item.name, fontWeight = FontWeight.Bold, color = TextPrimary)
                KeyValueRow("Price", Money.fmt(item.price, s.country), GoldAccent)
                if (item.weeklyUpkeep > 0) KeyValueRow("Upkeep", Money.fmt(item.weeklyUpkeep, s.country) + "/wk", LossRed)
                if (item.fameBoost > 0) KeyValueRow("Fame", "+%.1f".format(item.fameBoost), WinGreen)
                KeyValueRow("Morale", "+%.1f".format(item.moraleBoost), WinGreen)
                Spacer(Modifier.height(6.dp))
                Button(
                    onClick = { Game.buyItem(item.id) },
                    enabled = s.money >= item.price,
                    colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
                ) { Text(if (s.money >= item.price) "Buy" else "Can't afford") }
            }
        }
    }
}

@Composable
private fun FamilyTab(s: GameState) {
    SectionHeader("Home Life")
    InfoCard {
        if (s.partner == null) {
            Text("Single and married to the game.", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("Life finds a way — keep an eye on events between matches.", color = TextDim, fontSize = 12.sp)
        } else {
            Text(
                (if (s.married) "Married to " else "Dating ") + s.partner!!,
                color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 15.sp
            )
            if (s.kids > 0) Text("Children: ${s.kids}", color = TextDim, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            SkillBar("Relationship", s.relationship,
                color = if (s.relationship > 40) WinGreen else LossRed)
            Text("Touring erodes the bond — invest time and thought.", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { Game.familyGift() },
                enabled = s.money >= 1_000_000,
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy)
            ) { Text("Send a gift — ${Money.fmt(1_000_000, s.country)} (+6 ❤)", color = GoldAccent, fontSize = 12.sp) }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { Game.familyVacation() },
                enabled = s.money >= 5_000_000,
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)
            ) { Text("Family holiday — ${Money.fmt(5_000_000, s.country)} (+15 ❤)", fontSize = 12.sp) }
        }
    }
    SectionHeader("Public Standing")
    InfoCard {
        KeyValueRow("Followers", fmtFollowers(s.followers), GoldAccent)
        KeyValueRow("Public image", imageLabel(s.publicImage))
        Text(
            "Image shapes endorsement value. Press conferences, charity and controversies all move it.",
            color = TextDim, fontSize = 11.sp
        )
    }
}

@Composable
private fun EmpireTab(s: GameState) {
    val toast = Game.toast
    if (toast != null) {
        InfoCard {
            Text(toast, color = GoldAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Button(onClick = { Game.clearToast() },
                colors = ButtonDefaults.buttonColors(containerColor = CardNavy)) {
                Text("OK", fontSize = 12.sp)
            }
        }
    }

    SectionHeader("Social Media — ${fmtFollowers(s.followers)} followers")
    InfoCard {
        Text("Post at most once every couple of weeks.", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Button(onClick = { Game.socialPost("training") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)) {
            Text("Post training clip (+followers)", fontSize = 12.sp)
        }
        Button(onClick = { Game.socialPost("brand") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CardNavy)) {
            Text("Paid brand promo (₹, -image)", color = GoldAccent, fontSize = 12.sp)
        }
        Button(onClick = { Game.socialPost("hottake") }, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BallRed)) {
            Text("Controversial hot take (risky)", fontSize = 12.sp)
        }
    }

    SectionHeader("Love Life")
    if (s.partner == null) {
        InfoCard {
            Text("Single. Who catches your eye?", color = TextPrimary, fontSize = 13.sp)
            com.mohithash.cricketlegend.engine.LifeSystems.dateOptions.forEach { opt ->
                val ok = s.fame >= opt.fameReq && s.money >= opt.cost
                Button(
                    onClick = { Game.startDating(opt.type) },
                    enabled = ok,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (ok) PitchGreen else DeepNavy)
                ) {
                    Text(
                        "${opt.type} — fame ${opt.fameReq}" +
                            (if (opt.cost > 0) ", ${Money.fmt(opt.cost, s.country)}" else "") +
                            " (+${opt.fameBoost.toInt()} fame)",
                        fontSize = 11.sp, color = TextPrimary
                    )
                }
            }
        }
    } else {
        InfoCard {
            Text("Dating ${s.partner} (${s.partnerType})", color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("A high-profile partner boosts fame but demands your time. See the Family tab to nurture it.",
                color = TextDim, fontSize = 11.sp)
        }
    }

    SectionHeader("Mentoring")
    InfoCard {
        Text("Senior pros can lift the next generation — good for your public image and legacy.",
            color = TextDim, fontSize = 12.sp)
        if (s.mentees.isNotEmpty())
            Text("Proteges: ${s.mentees.joinToString(", ")}", color = WinGreen, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Button(onClick = { Game.mentorPlayer() },
            colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)) {
            Text("Mentor a young prospect", fontSize = 12.sp)
        }
    }

    SectionHeader("Franchise Ownership")
    InfoCard {
        if (s.ownedFranchise != null) {
            Text("You own the ${s.ownedFranchise}!", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            KeyValueRow("Squad strength", "${s.ownedSquadStrength.toInt()}/99")
            KeyValueRow("Titles as owner", "${s.ownedTitles}", GoldAccent)
            if (s.ownedLastFinish.isNotEmpty()) KeyValueRow("Last season", s.ownedLastFinish)
            if (s.ownedSquad.isNotEmpty())
                Text("Squad: ${s.ownedSquad.joinToString(", ")}", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Button(onClick = { Game.signSquadPlayer() },
                enabled = s.money >= 400_000_000L,
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PitchGreen)) {
                Text("Sign a marquee player (${Money.fmt(400_000_000L, s.country)})", fontSize = 11.sp)
            }
            if (s.franchiseTeam != s.ownedFranchise) {
                Button(onClick = { Game.playForOwnFranchise() },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)) {
                    Text("Play for your own team next season", color = DeepNavy, fontSize = 11.sp)
                }
            } else {
                Text("You captain your own ${s.ownedFranchise}. Owner and star, all in one.",
                    color = WinGreen, fontSize = 11.sp)
            }
        } else {
            Text("Buy your own T20 franchise — ${Money.fmt(com.mohithash.cricketlegend.engine.LifeSystems.FRANCHISE_PRICE, s.country)}.",
                color = TextPrimary, fontSize = 13.sp)
            Text("Owning a title-winning franchise prints money. Losing seasons bleed it.", color = TextDim, fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Button(
                onClick = { Game.buyFranchise() },
                enabled = s.money >= com.mohithash.cricketlegend.engine.LifeSystems.FRANCHISE_PRICE,
                colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
            ) {
                Text(if (s.money >= com.mohithash.cricketlegend.engine.LifeSystems.FRANCHISE_PRICE)
                    "Buy a franchise" else "Not rich enough (yet)", color = DeepNavy, fontSize = 12.sp)
            }
        }
    }

    SectionHeader("The Dark Side")
    InfoCard {
        Text("A shady bookie keeps calling. Easy money — if you don't get caught.",
            color = TextDim, fontSize = 12.sp)
        if (s.banSeasons > 0) {
            Text("BANNED — ${s.banSeasons} season(s) remaining.", color = LossRed, fontWeight = FontWeight.Bold)
        } else {
            Spacer(Modifier.height(6.dp))
            Button(onClick = { Game.acceptFixing() },
                colors = ButtonDefaults.buttonColors(containerColor = BallRed)) {
                Text("Take the bookie's money (45% caught)", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun GarageTab(s: GameState) {
    SectionHeader("Your Collection")
    if (s.ownedItems.isEmpty()) {
        InfoCard { Text("Nothing here yet. Big innings buy big toys.", color = TextDim) }
    }
    for (id in s.ownedItems) {
        val item = RealData.lifestyleItems.firstOrNull { it.id == id } ?: continue
        InfoCard {
            Text(item.name, fontWeight = FontWeight.Bold, color = GoldAccent)
            Text(item.category, color = TextDim, fontSize = 12.sp)
            if (item.weeklyUpkeep > 0) KeyValueRow("Upkeep", Money.fmt(item.weeklyUpkeep, s.country) + "/wk", LossRed)
        }
    }
}
