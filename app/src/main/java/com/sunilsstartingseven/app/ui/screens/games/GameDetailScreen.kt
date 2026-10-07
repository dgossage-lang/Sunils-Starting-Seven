package com.sunilsstartingseven.app.ui.screens.games

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    onLineup: () -> Unit,
    onGamePhoto: () -> Unit,
    onViewPhoto: () -> Unit,
    onLayout: () -> Unit = {},
    seasonName: String = ""
) {
    LaunchedEffect(game.id) { onInitialize() }
    val summary = attendanceSummary(attendance.map { it.attendance.status }, game.fieldPositions.size)
    val editableAttendance = game.status != GameStatus.PLAYED
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        if (seasonName.isNotBlank()) Text(seasonName, style = MaterialTheme.typography.labelLarge)
                        Text("Round ${game.roundNumber}", style = MaterialTheme.typography.headlineLarge)
                        Text(game.gameDate.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (game.status == GameStatus.DRAFT) IconButton(onEdit, enabled = !busy) { Icon(Icons.Outlined.Edit, "Edit game details") }
                }
                Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.small) {
                    Text(game.status.label, Modifier.padding(horizontal = Space.Medium, vertical = Space.Small), style = MaterialTheme.typography.labelMedium)
                }
                if (game.notes.isNotBlank()) Text(game.notes, style = MaterialTheme.typography.bodyMedium)
                if (game.status == GameStatus.PLAYED) {
                    Text("This game is marked played and now counts toward future fairness.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item { GameDayPhotoCard(game, busy, onGamePhoto, onViewPhoto) }
        item { OutlinedButton(onLayout, Modifier.fillMaxWidth(), enabled = !busy) { Text("Pitch Layout · ${game.fieldPositions.size} positions") } }
        item { AttendanceSummaryRow(summary) }
        if (summary.neededForLayout > 0) {
            item {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.medium) {
                    Text("${summary.neededForLayout} more ${if (summary.neededForLayout == 1) "player" else "players"} needed for this ${game.fieldPositions.size}-player layout.", Modifier.padding(Space.Normal), style = MaterialTheme.typography.bodyMedium)
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
                AttendanceRow(row.player, row.attendance.status, busy || !editableAttendance) { onAttendance(row.player.id, it) }
            }
        }
        item { PrimaryAction("Open Lineup", onLineup, Modifier.fillMaxWidth()) }
    }
}

@Composable
private fun GameDayPhotoCard(game: Game, busy: Boolean, onPhoto: () -> Unit, onViewPhoto: () -> Unit) {
    val context = LocalContext.current
    val bitmap = remember(game.photoPath) {
        game.photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(java.io.File(context.filesDir, path).absolutePath)?.asImageBitmap() }.getOrNull() }
    }
    Card {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Text("Game Day Photo", style = MaterialTheme.typography.titleMedium)
            if (bitmap != null) {
                Image(bitmap, "Round ${game.roundNumber} game day photo", Modifier.fillMaxWidth().height(170.dp).clip(MaterialTheme.shapes.large).clickable { onViewPhoto() }, contentScale = ContentScale.Crop)
                Text("A local memory from this game.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("Add one optional local photo as a memory of the day.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                OutlinedButton(onPhoto, enabled = !busy) { Text(if (game.photoPath == null) "Add Photo" else "Photo Options") }
                if (bitmap != null) TextButton(onViewPhoto, enabled = !busy) { Text("View") }
            }
        }
    }
}

@Composable
private fun AttendanceRow(player: Player, status: AttendanceStatus, busy: Boolean, onChange: (AttendanceStatus) -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                PlayerAvatar(player)
                Text(
                    player.displayName,
                    modifier = Modifier.weight(1f).widthIn(min = 0.dp),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }
            AttendanceToggle(status, enabled = !busy, playerName = player.displayName, onChange = onChange)
        }
    }
}

@Composable
private fun AttendanceToggle(status: AttendanceStatus, enabled: Boolean, playerName: String, onChange: (AttendanceStatus) -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        Modifier.fillMaxWidth(),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
        border = ButtonDefaults.outlinedButtonBorder(enabled)
    ) {
        Row(Modifier.fillMaxWidth().padding(2.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            AttendanceToggleSegment(
                label = AttendanceStatus.AVAILABLE.label,
                selected = status == AttendanceStatus.AVAILABLE,
                selectedColor = MaterialTheme.colorScheme.primary,
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                contentDescription = "Mark $playerName Playing"
            ) { onChange(AttendanceStatus.AVAILABLE) }
            AttendanceToggleSegment(
                label = AttendanceStatus.AWAY.label,
                selected = status == AttendanceStatus.AWAY,
                selectedColor = MaterialTheme.colorScheme.secondary,
                selectedContentColor = MaterialTheme.colorScheme.onSecondary,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                contentDescription = "Mark $playerName Away"
            ) { onChange(AttendanceStatus.AWAY) }
        }
    }
}

@Composable
private fun AttendanceToggleSegment(
    label: String,
    selected: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color,
    selectedContentColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    contentDescription: String,
    onClick: () -> Unit
) {
    val background = if (selected) selectedColor else MaterialTheme.colorScheme.surface
    val content = if (selected) selectedContentColor else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier
            .heightIn(min = 48.dp)
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled) { onClick() }
            .semantics { this.contentDescription = contentDescription },
        shape = MaterialTheme.shapes.medium,
        color = if (enabled) background else background.copy(alpha = .58f)
    ) {
        Box(Modifier.fillMaxWidth().padding(horizontal = Space.Medium, vertical = Space.Small), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1, softWrap = false)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun GameDetailPreview() {
    StartingSevenTheme {
        GameDetailScreen(Game(roundNumber = 4, gameDate = LocalDate.now()), emptyList(), emptyList(), false, {}, { _, _ -> }, {}, {}, {}, {}, {})
    }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun AttendanceRowCompactPreview() {
    StartingSevenTheme {
        AttendanceRow(Player(firstName = "Samrith", lastInitial = ""), AttendanceStatus.AVAILABLE, false) {}
    }
}

@Preview(showBackground = true, widthDp = 400)
@Composable
private fun AttendanceRowNormalPreview() {
    StartingSevenTheme {
        AttendanceRow(Player(firstName = "Cameron", lastInitial = ""), AttendanceStatus.AWAY, false) {}
    }
}
