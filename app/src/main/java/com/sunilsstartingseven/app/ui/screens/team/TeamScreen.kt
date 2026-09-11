package com.sunilsstartingseven.app.ui.screens.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.sunilsstartingseven.app.data.local.Player
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*

@Composable
fun TeamScreen(players: List<Player>, busy: Boolean, onAdd: () -> Unit, onHistory: (Player) -> Unit, onEdit: (Player) -> Unit, onActive: (Player, Boolean) -> Unit) {
    var showInactive by rememberSaveable { mutableStateOf(false) }
    val visible = players.filter { it.isActive != showInactive }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Your team", style = MaterialTheme.typography.headlineLarge)
                Text("Every player belongs.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { PrimaryAction("Add Player", onAdd, Modifier.fillMaxWidth(), !busy) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                FilterChip(!showInactive, { showInactive = false }, { Text("Active (${players.count { it.isActive }})") })
                FilterChip(showInactive, { showInactive = true }, { Text("Inactive (${players.count { !it.isActive }})") })
            }
        }
        if (visible.isEmpty()) item {
            if (showInactive) Text("No inactive players. Players you deactivate will stay safely here.", style = MaterialTheme.typography.bodyMedium)
            else EmptyCard("No players yet", "Add your first player to get the team started.", "Add Player", onAdd)
        }
        items(visible, key = { it.id }) { player -> PlayerCard(player, busy, { onHistory(player) }, { onEdit(player) }, { onActive(player, !player.isActive) }) }
    }
}

@Composable
fun PlayerCard(player: Player, busy: Boolean, onOpenHistory: () -> Unit, onEdit: () -> Unit, onToggle: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onOpenHistory).semantics { contentDescription = "Open ${player.displayName} history" }) {
        Column(Modifier.padding(Space.Normal)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(player.displayName, style = MaterialTheme.typography.titleLarge)
                    Text(if (player.isActive) "Active player" else "Inactive player", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onEdit, enabled = !busy) { Icon(Icons.Outlined.Edit, "Edit ${player.displayName}") }
            }
            TextButton(onToggle, enabled = !busy, modifier = Modifier.semantics { contentDescription = "${if (player.isActive) "Deactivate" else "Reactivate"} ${player.displayName}" }) {
                Text(if (player.isActive) "Deactivate" else "Reactivate")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TeamPreview() { StartingSevenTheme { TeamScreen(emptyList(), false, {}, {}, {}, { _, _ -> }) } }

@Preview(showBackground = true)
@Composable
private fun PlayerPreview() { StartingSevenTheme { PlayerCard(Player(firstName = "Mason", lastInitial = "G"), false, {}, {}, {}) } }
