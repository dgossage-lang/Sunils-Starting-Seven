package com.sunilsstartingseven.app.ui.screens.games

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.sunilsstartingseven.app.data.local.Game
import com.sunilsstartingseven.app.domain.model.AttendanceSummary
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GamesScreen(games: List<Game>, summaries: Map<Long, AttendanceSummary>, onNew: () -> Unit, onOpen: (Game) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Game days", style = MaterialTheme.typography.headlineLarge)
                Text("Start with a round and a date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { PrimaryAction("New Game", onNew, Modifier.fillMaxWidth()) }
        if (games.isEmpty()) item { EmptyCard("No games yet", "Create your first draft. Your game details will be saved on this device.", "Create Game", onNew) }
        items(games, key = { it.id }) { GameCard(it, summaries[it.id]) { onOpen(it) } }
    }
}

@Composable
fun GameCard(game: Game, summary: AttendanceSummary? = null, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val bitmap = remember(game.photoPath) {
        game.photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(java.io.File(context.filesDir, path).absolutePath)?.asImageBitmap() }.getOrNull() }
    }
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Open Round ${game.roundNumber}" },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (bitmap != null) {
            Row(
                Modifier.fillMaxWidth().padding(Space.Large),
                horizontalArrangement = Arrangement.spacedBy(Space.Normal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap,
                    "Round ${game.roundNumber} game day photo",
                    Modifier.size(width = 82.dp, height = 72.dp).clip(MaterialTheme.shapes.medium),
                    contentScale = ContentScale.Crop
                )
                GameCardDetails(game, summary, Modifier.weight(1f), stackedSummary = true)
            }
        } else {
            GameCardDetails(game, summary, Modifier.padding(Space.Large), stackedSummary = false)
        }
    }
}

@Composable
private fun GameCardDetails(game: Game, summary: AttendanceSummary?, modifier: Modifier = Modifier, stackedSummary: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Space.Small)) {
        Text("Round ${game.roundNumber}", style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(game.gameDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val summaryContent: @Composable () -> Unit = {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                Text(game.status.label, Modifier.padding(horizontal = Space.Medium, vertical = Space.Small), style = MaterialTheme.typography.labelMedium, maxLines = 1)
            }
            summary?.let {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                    Text("${it.available} Playing · ${it.away} Away", Modifier.padding(horizontal = Space.Medium, vertical = Space.Small), style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        if (stackedSummary) Column(verticalArrangement = Arrangement.spacedBy(Space.Tiny)) { summaryContent() }
        else Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) { summaryContent() }
        if (game.notes.isNotBlank()) Text(game.notes, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Preview(showBackground = true)
@Composable
private fun GamePreview() { StartingSevenTheme { GameCard(Game(roundNumber = 1, gameDate = LocalDate.of(2026, 9, 12)), AttendanceSummary(8, 1)) } }
