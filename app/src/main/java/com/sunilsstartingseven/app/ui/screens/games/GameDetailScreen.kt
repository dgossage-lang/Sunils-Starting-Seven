package com.sunilsstartingseven.app.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun GameDetailScreen(
    game: Game,
    attendance: List<AttendanceWithPlayer>,
    missingPlayers: List<Player>,
    busy: Boolean,
    onInitialize: () -> Unit,
    onAttendance: (Long, AttendanceStatus) -> Unit,
    onAddMissing: () -> Unit,
    onEdit: () -> Unit,
    onLineup: () -> Unit
) {
    LaunchedEffect(game.id) { onInitialize() }
    val summary = attendanceSummary(attendance.map { it.attendance.status })
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Round ${game.roundNumber}", style = MaterialTheme.typography.headlineLarge)
                        Text(game.gameDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (game.status == GameStatus.DRAFT) IconButton(onEdit, enabled = !busy) { Icon(Icons.Outlined.Edit, "Edit game details") }
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(game.status.label, Modifier.padding(horizontal = Space.Medium, vertical = Space.Small), style = MaterialTheme.typography.labelMedium)
                }
                if (game.notes.isNotBlank()) Text(game.notes, style = MaterialTheme.typography.bodyMedium)
            }
        }
        item { AttendanceSummaryRow(summary) }
        if (summary.neededForFullSeven > 0) {
            item {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text("${summary.neededForFullSeven} more ${if (summary.neededForFullSeven == 1) "player" else "players"} needed for a full Starting Seven.", Modifier.padding(Space.Normal), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                AssistChip(onClick = {}, enabled = false, label = { Text("Attendance") })
                AssistChip(onClick = onLineup, enabled = true, label = { Text("Lineup") })
            }
        }
        if (missingPlayers.isNotEmpty() && game.status == GameStatus.DRAFT) {
            item {
                OutlinedButton(onAddMissing, Modifier.fillMaxWidth(), enabled = !busy) {
                    Text("Add Missing Players (${missingPlayers.size})")
                }
            }
        }
        if (attendance.isEmpty()) {
            item { EmptyCard("No attendance yet", "Add active players to this draft game.", "Initialize Attendance", onInitialize) }
        } else {
            items(attendance, key = { it.player.id }) { row ->
                AttendanceRow(row.player, row.attendance.status, busy) { onAttendance(row.player.id, it) }
            }
        }
        item { PrimaryAction("Open Lineup", onLineup, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun AttendanceRow(player: Player, status: AttendanceStatus, busy: Boolean, onChange: (AttendanceStatus) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(Space.Normal), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
            InitialsAvatar(player)
            Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
                Text(
                    player.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
                Text(status.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SingleChoiceSegmentedButtonRow(Modifier.widthIn(max = 168.dp)) {
                AttendanceStatus.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = status == option,
                        onClick = { onChange(option) },
                        enabled = !busy,
                        modifier = Modifier.semantics { contentDescription = "Mark ${player.displayName} ${option.label}" },
                        shape = SegmentedButtonDefaults.itemShape(index, AttendanceStatus.entries.size),
                        label = { Text(option.label) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailPreview() {
    StartingSevenTheme {
        GameDetailScreen(Game(roundNumber = 4, gameDate = LocalDate.now()), emptyList(), emptyList(), false, {}, { _, _ -> }, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun AttendanceRowCompactPreview() {
    StartingSevenTheme {
        AttendanceRow(Player(firstName = "Mason-Junior", lastInitial = "G"), AttendanceStatus.AVAILABLE, false) {}
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun AttendanceRowNormalPreview() {
    StartingSevenTheme {
        AttendanceRow(Player(firstName = "Christopher", lastInitial = "W"), AttendanceStatus.AWAY, false) {}
    }
}
