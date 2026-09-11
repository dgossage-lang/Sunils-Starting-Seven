package com.sunilsstartingseven.app.ui.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.R
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.screens.games.GameCard
import com.sunilsstartingseven.app.ui.theme.*
import com.sunilsstartingseven.app.viewmodel.TeamState

@Composable
fun HomeScreen(state: TeamState, onTeam: () -> Unit, onCreateGame: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Large)) {
        if (state.games.isEmpty()) {
            Image(painterResource(R.drawable.logo_with_text), "Sunil’s Starting Seven. Play, rotate, grow.", Modifier.size(176.dp).align(Alignment.CenterHorizontally))
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Big smiles.\nTeam spirit.", style = MaterialTheme.typography.headlineLarge)
            Text("A little planning. More time to play.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (state.players.none { it.isActive }) {
            EmptyCard("Let’s build your team", "Add your players to get the team started. Just a first name and last initial.", "Add Players", onTeam)
        } else {
            Card(onClick = onTeam, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                    Text("Your team", style = MaterialTheme.typography.titleMedium)
                    val count = state.players.count { it.isActive }
                    Text("$count active ${if (count == 1) "player" else "players"}", style = MaterialTheme.typography.headlineSmall)
                    Text("View and manage players →", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.Medium)) {
            Text("Game day starts here", style = MaterialTheme.typography.titleLarge)
            if (state.games.isEmpty()) Text("No games yet. Save a draft with your round and date.", style = MaterialTheme.typography.bodyMedium)
            else GameCard(state.games.first())
            PrimaryAction("Create Game", onCreateGame, Modifier.fillMaxWidth())
        }
        SoccerPitch(Modifier.fillMaxWidth())
        Text("PLAY • ROTATE • GROW", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Preview(showBackground = true)
@Composable
private fun HomePreview() { StartingSevenTheme { HomeScreen(TeamState(loading = false), {}, {}) } }
