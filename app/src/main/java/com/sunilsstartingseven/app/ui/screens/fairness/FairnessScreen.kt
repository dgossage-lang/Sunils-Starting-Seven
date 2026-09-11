package com.sunilsstartingseven.app.ui.screens.fairness

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.domain.history.FairnessStatus
import com.sunilsstartingseven.app.domain.history.PlayerHistorySummary
import com.sunilsstartingseven.app.domain.history.TeamFairnessSummary
import com.sunilsstartingseven.app.domain.model.PositionFamily
import com.sunilsstartingseven.app.ui.components.EmptyCard
import com.sunilsstartingseven.app.ui.theme.Brand
import com.sunilsstartingseven.app.ui.theme.Space

@Composable
fun FairnessScreen(summary: TeamFairnessSummary, onPlayer: (Long) -> Unit) {
    var showInactive by rememberSaveable { mutableStateOf(false) }
    val players = if (showInactive) summary.inactivePlayers else summary.activePlayers
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Fairness", style = MaterialTheme.typography.headlineLarge)
                Text("Everyone gets a fair go.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { TeamBalanceCard(summary) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Player balance", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    FilterChip(!showInactive, { showInactive = false }, { Text("Active (${summary.activePlayers.size})") })
                    FilterChip(showInactive, { showInactive = true }, { Text("Inactive (${summary.inactivePlayers.size})") })
                }
            }
        }
        if (!summary.hasPlayedHistory) {
            item { EmptyNoHistoryCard() }
        } else if (players.isEmpty()) {
            item { Text(if (showInactive) "No inactive players have history yet." else "No active players yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(players, key = { it.player.id }) { player ->
                PlayerBalanceRow(player, onClick = { onPlayer(player.player.id) })
            }
        }
        if (summary.hasPlayedHistory) {
            item { TeamPositionBalanceCard(summary) }
            item { BenchBalanceCard(summary) }
        }
    }
}

@Composable
private fun TeamBalanceCard(summary: TeamFairnessSummary) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Team Balance", style = MaterialTheme.typography.titleMedium)
            if (!summary.hasPlayedHistory) {
                Text("No played games yet", style = MaterialTheme.typography.headlineSmall)
                Text("Fairness history will appear once games are marked Played.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Text("${summary.playedGames} Played ${if (summary.playedGames == 1) "Game" else "Games"}", style = MaterialTheme.typography.headlineSmall)
                Text("Playing time: ${summary.playingTimeLabel}", style = MaterialTheme.typography.bodyMedium)
                Text("Positions: ${summary.positionLabel}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun EmptyNoHistoryCard() {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("No fairness history yet", style = MaterialTheme.typography.titleMedium)
            Text("Once games are marked Played, participation and position balance will appear here.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PlayerBalanceRow(player: PlayerHistorySummary, onClick: () -> Unit) {
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "Open ${player.player.displayName} history" }
    ) {
        Row(Modifier.fillMaxWidth().padding(Space.Normal), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Normal)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
                Text(player.player.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${player.gamesAttended} games - ${player.activeRotations} playing rotations - ${player.benchRotations} bench",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            StatusPill(player.fairnessStatus)
            Text(">", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatusPill(status: FairnessStatus, modifier: Modifier = Modifier) {
    Surface(modifier, color = statusColor(status), shape = MaterialTheme.shapes.medium) {
        Text(status.label, Modifier.padding(horizontal = Space.Small, vertical = Space.Tiny), style = MaterialTheme.typography.labelMedium, color = Brand.Navy)
    }
}

@Composable
private fun TeamPositionBalanceCard(summary: TeamFairnessSummary) {
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Team Position Balance", style = MaterialTheme.typography.titleMedium)
            summary.positionBalance.forEach { item ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.family.label(), style = MaterialTheme.typography.bodyMedium)
                    Text("range ${item.min}-${item.max} - ${item.label}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun BenchBalanceCard(summary: TeamFairnessSummary) {
    val balance = summary.benchBalance ?: return
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Bench Balance", style = MaterialTheme.typography.titleMedium)
            Text("Bench range: ${balance.minBench}-${balance.maxBench}", style = MaterialTheme.typography.bodyMedium)
            Text(balance.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun statusColor(status: FairnessStatus): Color = when (status) {
    FairnessStatus.NO_HISTORY -> MaterialTheme.colorScheme.surfaceContainerHigh
    FairnessStatus.BUILDING_HISTORY -> MaterialTheme.colorScheme.secondaryContainer
    FairnessStatus.BALANCED -> MaterialTheme.colorScheme.primaryContainer
    FairnessStatus.NEEDS_OPPORTUNITY -> Brand.Sky
    FairnessStatus.SLIGHTLY_AHEAD -> Brand.Highlight.copy(alpha = .75f)
}

fun PositionFamily.label(): String = when (this) {
    PositionFamily.GK -> "Goalkeeper"
    PositionFamily.DEF -> "Defence"
    PositionFamily.WING -> "Wing"
    PositionFamily.MID -> "Midfield"
    PositionFamily.ATT -> "Attack"
}

@Composable
fun CountBar(
    label: String,
    count: Int,
    max: Int,
    color: Color,
    modifier: Modifier = Modifier,
    labelMinWidth: androidx.compose.ui.unit.Dp = 48.dp
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
        Text(
            label,
            Modifier.widthIn(min = labelMinWidth),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Box(Modifier.weight(1f).height(10.dp).background(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.shapes.small)) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(if (max <= 0) 0f else count.toFloat() / max)
                    .background(color, MaterialTheme.shapes.small)
            )
        }
        Text("$count", Modifier.width(32.dp), style = MaterialTheme.typography.labelMedium, textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}
