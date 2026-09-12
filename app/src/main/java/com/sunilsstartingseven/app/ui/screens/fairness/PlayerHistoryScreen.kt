package com.sunilsstartingseven.app.ui.screens.fairness

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.domain.history.GameHistoryEntry
import com.sunilsstartingseven.app.domain.history.PlayerHistorySummary
import com.sunilsstartingseven.app.domain.model.AttendanceStatus
import com.sunilsstartingseven.app.domain.model.PlayingPosition
import com.sunilsstartingseven.app.domain.model.PlayingState
import com.sunilsstartingseven.app.domain.model.PositionFamily
import com.sunilsstartingseven.app.domain.rotation.FieldPositions
import com.sunilsstartingseven.app.domain.rotation.ROTATION_COUNT
import com.sunilsstartingseven.app.ui.components.PositionColor
import com.sunilsstartingseven.app.ui.components.PhotoAvatar
import com.sunilsstartingseven.app.ui.components.displayInitials
import com.sunilsstartingseven.app.ui.theme.Space
import java.time.format.DateTimeFormatter

private val GameDateFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

@Composable
fun PlayerHistoryScreen(history: PlayerHistorySummary?, onGame: (Long) -> Unit) {
    if (history == null) {
        Box(Modifier.fillMaxSize().padding(Space.Large), contentAlignment = Alignment.Center) {
            Text("This player is no longer available.")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item { PlayerHeader(history) }
        item { ParticipationCard(history) }
        if (history.gamesAttended == 0 && history.gamesAway == 0) {
            item { NoPlayerHistoryCard() }
        } else {
            item { PositionDistributionCard(history) }
            item { RoleGroupsCard(history) }
            item { RoleCoverageCard(history) }
            item { NextOpportunitiesCard(history) }
            item { Text("Game History", style = MaterialTheme.typography.titleLarge) }
            history.games.forEach { game ->
                item { GameHistoryCard(game, onGame) }
            }
        }
    }
}

@Composable
private fun PlayerHeader(history: PlayerHistorySummary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Normal)) {
        PhotoAvatar(history.player.photoPath, displayInitials(history.player.displayName), history.player.displayName, size = 64.dp)
        Column(Modifier.weight(1f)) {
            Text(history.player.displayName, style = MaterialTheme.typography.headlineMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(if (history.player.isActive) "Active player" else "Inactive player", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        StatusPill(history.fairnessStatus)
    }
}


@Composable
private fun ParticipationCard(history: PlayerHistorySummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Medium)) {
            Text("Participation", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                StatBlock("Games", "${history.gamesAttended} played\n${history.gamesAway} away", Modifier.weight(1f))
                StatBlock("Rotations", "${history.activeRotations} playing\n${history.benchRotations} bench", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatBlock(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Color.White, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun NoPlayerHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("No played-game history yet.", style = MaterialTheme.typography.titleMedium)
            Text("Their position history will appear after a game is marked Played.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PositionDistributionCard(history: PlayerHistorySummary) {
    val max = maxOf(1, history.exactPositions.values.maxOrNull() ?: 0)
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Position Distribution", style = MaterialTheme.typography.titleMedium)
            FieldPositions.forEach { position ->
                CountBar(position.shortLabel, history.exactPositions[position] ?: 0, max, PositionColor(position.family))
            }
        }
    }
}

@Composable
private fun RoleGroupsCard(history: PlayerHistorySummary) {
    val max = maxOf(1, history.families.values.maxOrNull() ?: 0)
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Role Groups", style = MaterialTheme.typography.titleMedium)
            PositionFamily.entries.forEach { family ->
                CountBar(family.label(), history.families[family] ?: 0, max, familyColor(family), labelMinWidth = 92.dp)
            }
        }
    }
}

@Composable
private fun RoleCoverageCard(history: PlayerHistorySummary) {
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
            Text("Roles experienced", style = MaterialTheme.typography.titleMedium)
            Text("${history.distinctExactPositions} / ${FieldPositions.size} positions", style = MaterialTheme.typography.headlineSmall)
            Text("${history.distinctFamilies} / ${PositionFamily.entries.size} role groups", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun NextOpportunitiesCard(history: PlayerHistorySummary) {
    if (history.nextOpportunities.isEmpty()) {
        if (history.activeRotations < 8) {
            Text("Still building position history.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Likely Next Opportunities", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                history.nextOpportunities.forEach { position -> PositionChip(position.shortLabel, PositionColor(position.family)) }
            }
        }
    }
}

@Composable
private fun GameHistoryCard(game: GameHistoryEntry, onGame: (Long) -> Unit) {
    Card(onClick = { onGame(game.gameId) }) {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Round ${game.roundNumber}", style = MaterialTheme.typography.titleMedium)
                Text(game.gameDate.format(GameDateFormat), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (game.attendance == AttendanceStatus.AWAY) {
                PositionChip("Away", MaterialTheme.colorScheme.surfaceContainerHigh)
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    (0 until ROTATION_COUNT).forEach { rotation ->
                        val row = game.rotations.find { it.rotationIndex == rotation }
                        val label = if (row?.playingState == PlayingState.BENCH) "BENCH" else row?.position?.shortLabel ?: "-"
                        val color = row?.position?.family?.let { PositionColor(it) } ?: MaterialTheme.colorScheme.surfaceContainerHigh
                        PositionChip(label, color)
                    }
                }
            }
        }
    }
}

@Composable
private fun PositionChip(label: String, color: Color) {
    Surface(color = color, shape = MaterialTheme.shapes.medium) {
        Text(label, Modifier.padding(horizontal = Space.Small, vertical = Space.Tiny), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun familyColor(family: PositionFamily): Color = when (family) {
    PositionFamily.GK -> PositionColor(PlayingPosition.GOALKEEPER.family)
    PositionFamily.DEF -> PositionColor(PlayingPosition.DEFENDER_1.family)
    PositionFamily.WING -> PositionColor(PlayingPosition.LEFT_WING.family)
    PositionFamily.MID -> PositionColor(PlayingPosition.MID.family)
    PositionFamily.ATT -> PositionColor(PlayingPosition.STRIKER.family)
}
