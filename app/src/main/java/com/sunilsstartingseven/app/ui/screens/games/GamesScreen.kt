package com.sunilsstartingseven.app.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sunilsstartingseven.app.data.local.Game
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GamesScreen(games: List<Game>, onNew: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Game days", style = MaterialTheme.typography.headlineLarge)
                Text("Start with a round and a date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { PrimaryAction("New Game", onNew, Modifier.fillMaxWidth()) }
        if (games.isEmpty()) item { EmptyCard("No games yet", "Create your first draft. Your game details will be saved on this device.", "Create Game", onNew) }
        items(games, key = { it.id }) { GameCard(it) }
    }
}

@Composable
fun GameCard(game: Game) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Round ${game.roundNumber}", style = MaterialTheme.typography.titleLarge)
            Text(game.gameDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodyMedium)
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(game.status.label, Modifier.padding(horizontal = Space.Medium, vertical = Space.Small), style = MaterialTheme.typography.labelMedium)
            }
            if (game.notes.isNotBlank()) Text(game.notes, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GamePreview() { StartingSevenTheme { GameCard(Game(roundNumber = 1, gameDate = LocalDate.of(2026, 9, 12))) } }
