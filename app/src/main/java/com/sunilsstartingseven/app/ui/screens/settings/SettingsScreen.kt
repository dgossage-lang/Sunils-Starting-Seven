package com.sunilsstartingseven.app.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.BuildConfig
import com.sunilsstartingseven.app.data.backup.BackupPreview
import com.sunilsstartingseven.app.ui.components.PrimaryAction
import com.sunilsstartingseven.app.ui.theme.Space
import com.sunilsstartingseven.app.viewmodel.TeamState
import java.time.format.DateTimeFormatter

@Composable
fun SettingsScreen(
    state: TeamState,
    onCreateBackup: () -> Unit,
    onRestoreBackup: () -> Unit,
    onConfirmRestore: () -> Unit,
    onCancelRestore: () -> Unit
) {
    var confirmRestore by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Large)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Settings", style = MaterialTheme.typography.headlineLarge)
            Text("App version ${BuildConfig.VERSION_NAME}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Medium)) {
                Text("Data & Backup", style = MaterialTheme.typography.titleLarge)
                Text("Keep a copy of your players, games, photos and rotation history.", style = MaterialTheme.typography.bodyMedium)
                PrimaryAction("Create Backup", onCreateBackup, Modifier.fillMaxWidth(), !state.saving)
                OutlinedButton(onRestoreBackup, Modifier.fillMaxWidth().heightIn(min = 52.dp), enabled = !state.saving, shape = MaterialTheme.shapes.medium) {
                    Text("Restore Backup")
                }
                Text("Backup files can contain team and game photos. Store them somewhere you trust.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        state.restorePreview?.let { validated ->
            RestorePreviewCard(
                backup = validated.preview,
                currentPlayers = state.players.size,
                currentGames = state.games.size,
                busy = state.saving,
                onCancel = onCancelRestore,
                onRestore = { confirmRestore = true }
            )
        }
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { if (!state.saving) confirmRestore = false },
            title = { Text("Restore this backup?") },
            text = { Text("Your current players, games, photos and rotation history will be replaced by the selected backup.") },
            confirmButton = {
                Button({
                    confirmRestore = false
                    onConfirmRestore()
                }, enabled = !state.saving) { Text("Restore") }
            },
            dismissButton = { TextButton({ confirmRestore = false }, enabled = !state.saving) { Text("Cancel") } }
        )
    }
}

@Composable
private fun RestorePreviewCard(
    backup: BackupPreview,
    currentPlayers: Int,
    currentGames: Int,
    busy: Boolean,
    onCancel: () -> Unit,
    onRestore: () -> Unit
) {
    val date = backup.createdAt?.format(DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a")) ?: "Unknown"
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Restore Backup", style = MaterialTheme.typography.titleLarge)
            Text("Created: $date", style = MaterialTheme.typography.bodyMedium)
            Text("Current device: $currentPlayers players - $currentGames games", style = MaterialTheme.typography.bodyMedium)
            Text("Backup: ${backup.playerCount} players - ${backup.gameCount} games", style = MaterialTheme.typography.bodyMedium)
            Text("Played: ${backup.playedCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Draft / Ready: ${backup.draftCount + backup.readyCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Cancelled: ${backup.cancelledCount}", style = MaterialTheme.typography.bodyMedium)
            Text("Photos: ${backup.playerPhotoCount} player - ${backup.gamePhotoCount} game", style = MaterialTheme.typography.bodyMedium)
            Text("This will replace the data currently stored on this device.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                OutlinedButton(onCancel, Modifier.weight(1f), enabled = !busy) { Text("Cancel") }
                Button(onRestore, Modifier.weight(1f), enabled = !busy) { Text("Restore Backup") }
            }
        }
    }
}
