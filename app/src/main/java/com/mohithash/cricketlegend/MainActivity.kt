package com.mohithash.cricketlegend

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohithash.cricketlegend.ui.CricketLegendTheme
import com.mohithash.cricketlegend.ui.GoldAccent
import com.mohithash.cricketlegend.ui.HomeScreen
import com.mohithash.cricketlegend.ui.LifeScreen
import com.mohithash.cricketlegend.ui.MatchScreen
import com.mohithash.cricketlegend.ui.MoneyScreen
import com.mohithash.cricketlegend.ui.NewGameScreen
import com.mohithash.cricketlegend.ui.StatsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Game.init(applicationContext)
        setContent {
            CricketLegendTheme {
                com.mohithash.cricketlegend.ui.ScreenBackground {
                    Root()
                }
            }
        }
    }
}

private data class Tab(val label: String, val icon: ImageVector)

@Composable
private fun Root() {
    // reading Game.version subscribes the whole tree to state mutations
    @Suppress("UNUSED_VARIABLE") val v = Game.version
    val state = Game.state

    // Franchise-manager mode takes over the screen while active
    Game.franchise?.let { fr ->
        com.mohithash.cricketlegend.ui.FranchiseScreen(fr)
        return
    }

    if (state == null) {
        NewGameScreen()
        return
    }

    state.pendingAuction?.let { auction ->
        com.mohithash.cricketlegend.ui.AuctionScreen(state, auction)
        return
    }

    Game.live?.let { lm ->
        com.mohithash.cricketlegend.ui.LiveMatchScreen(state, lm)
        return
    }

    val tabs = listOf(
        Tab("Home", Icons.Filled.Home),
        Tab("Match", Icons.Filled.PlayArrow),
        Tab("Career", Icons.Filled.Star),
        Tab("Money", Icons.Filled.ShoppingCart),
        Tab("Life", Icons.Filled.Person)
    )

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        bottomBar = {
            NavigationBar(
                containerColor = com.mohithash.cricketlegend.ui.CardNavy,
                tonalElevation = 0.dp
            ) {
                tabs.forEachIndexed { i, tab ->
                    NavigationBarItem(
                        selected = Game.selectedTab == i,
                        onClick = { Game.selectedTab = i },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 11.sp) },
                        colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                            selectedIconColor = com.mohithash.cricketlegend.ui.DeepNavy,
                            selectedTextColor = com.mohithash.cricketlegend.ui.GoldAccent,
                            indicatorColor = com.mohithash.cricketlegend.ui.GoldAccent,
                            unselectedIconColor = com.mohithash.cricketlegend.ui.TextDim,
                            unselectedTextColor = com.mohithash.cricketlegend.ui.TextDim
                        )
                    )
                }
            }
        }
    ) { padding ->
        val mod = Modifier.padding(padding)
        // IMPORTANT: read Game.version INSIDE this (the Scaffold content) scope, not just at
        // the top of Root. State is mutated in place, so the only recompose trigger is this
        // counter. Reading it here subscribes this scope so actions that keep the same tab
        // (e.g. Play Next Match on the Match tab) still re-render. Do NOT wrap in Crossfade.
        val ver = Game.version
        if (ver >= 0) when (Game.selectedTab) {
            0 -> HomeScreen(state, mod)
            1 -> MatchScreen(state, mod)
            2 -> StatsScreen(state, mod)
            3 -> MoneyScreen(state, mod)
            else -> LifeScreen(state, mod)
        }

        // Global action feedback toast — every tap answers back
        Game.toast?.let { msg ->
            androidx.compose.runtime.LaunchedEffect(msg) {
                kotlinx.coroutines.delay(2500)
                Game.clearToast()
            }
            androidx.compose.foundation.layout.Box(
                Modifier
                    .padding(padding)
                    .fillMaxSize()
            ) {
                androidx.compose.material3.Surface(
                    color = com.mohithash.cricketlegend.ui.GoldAccent,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(16.dp)
                        .clickable { Game.clearToast() }
                ) {
                    Text(
                        msg,
                        color = com.mohithash.cricketlegend.ui.DeepNavy,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
            }
        }

        state.pendingEvent?.let { ev ->
            AlertDialog(
                onDismissRequest = { /* must choose */ },
                title = { Text(ev.title, fontWeight = FontWeight.Bold, color = GoldAccent) },
                text = {
                    androidx.compose.foundation.layout.Column {
                        Text(ev.description)
                        androidx.compose.foundation.layout.Spacer(
                            androidx.compose.ui.Modifier.padding(vertical = 6.dp))
                        ev.choices.forEachIndexed { i, choice ->
                            Button(
                                onClick = { Game.resolveEvent(i) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                            ) { Text(choice.label, fontSize = 13.sp) }
                        }
                    }
                },
                confirmButton = {}
            )
        }
    }
}
