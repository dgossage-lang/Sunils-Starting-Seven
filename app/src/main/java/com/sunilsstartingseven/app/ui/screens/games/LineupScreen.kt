package com.sunilsstartingseven.app.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*
import java.time.LocalDate

@Composable
fun LineupScreen(game: Game, attendance: List<AttendanceWithPlayer>, assignments: List<AssignmentWithPlayer>, busy: Boolean = false, onGenerate: () -> Unit = {}, onMarkPlayed: () -> Unit = {}, onSaveManualLineup: (List<RotationAssignment>, () -> Unit) -> Unit = { _, _ -> }) {
    var selectedRotation by rememberSaveable(game.id) { mutableIntStateOf(0) }
    var confirmPlayed by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable(game.id) { mutableStateOf(false) }
    var selectedPlayerId by rememberSaveable(game.id) { mutableStateOf<Long?>(null) }
    var undoStack by remember(game.id) { mutableStateOf<List<List<RotationAssignment>>>(emptyList()) }
    val persisted = remember(assignments) { assignments.map { it.assignment.toRotationAssignment() } }
    var pending by remember(game.id, assignments) { mutableStateOf(persisted) }
    var confirmDiscard by rememberSaveable(game.id) { mutableStateOf(false) }
    var showNotes by rememberSaveable(game.id) { mutableStateOf(false) }
    var confirmFairSave by rememberSaveable(game.id) { mutableStateOf(false) }
    val players = remember(assignments) { assignments.associate { it.player.id to it.player } }
    val names = remember(players) { players.mapValues { it.value.displayName } }
    val summary = attendanceSummary(attendance.map { it.attendance.status })
    val shown = if (editing) pending else persisted
    val rotation = RotationPeriods[selectedRotation]
    val rotationAssignments = shown.filter { it.rotationIndex == selectedRotation }
    val playersByPosition = rotationAssignments.filter { it.playingState == PlayingState.ON_FIELD && it.position != null }.associate { it.position!! to players[it.playerId]!! }
    val benchPlayers = rotationAssignments.filter { it.playingState == PlayingState.BENCH }.mapNotNull { players[it.playerId] }
    val hasLineup = persisted.isNotEmpty()
    val canGenerate = !busy && summary.readyForLineup && game.status != GameStatus.PLAYED && !editing
    val canMarkPlayed = !busy && game.status == GameStatus.READY && hasLineup && !editing
    val changed = pending != persisted
    val assessment = remember(pending, names) { ManualLineupAssessment.assess(pending, names) }

    fun exitEditing() { editing = false; selectedPlayerId = null; undoStack = emptyList(); pending = persisted }
    fun swapOrSelect(playerId: Long) {
        val selected = selectedPlayerId
        if (selected == null) selectedPlayerId = playerId
        else if (selected == playerId) selectedPlayerId = null
        else {
            val next = LineupSwaps.swap(pending, selectedRotation, selected, playerId)
            if (next != pending) { undoStack = undoStack + listOf(pending); pending = next }
            selectedPlayerId = null
        }
    }
    fun requestExit() { if (changed) confirmDiscard = true else exitEditing() }
    BackHandler(editing) { requestExit() }

    if (confirmPlayed) AlertDialog(onDismissRequest = { confirmPlayed = false }, title = { Text("Mark Round ${game.roundNumber} as played?") }, text = { Text("This saved roster will be included in future fairness calculations.") }, confirmButton = { TextButton({ confirmPlayed = false; onMarkPlayed() }) { Text("Mark Played") } }, dismissButton = { TextButton({ confirmPlayed = false }) { Text("Cancel") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard lineup changes?") }, text = { Text("Your unsaved swaps will be lost.") }, confirmButton = { TextButton({ confirmDiscard = false; exitEditing() }) { Text("Discard") } }, dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep Editing") } })
    if (showNotes) AlertDialog(onDismissRequest = { showNotes = false }, title = { Text(assessment.label) }, text = { Column(verticalArrangement = Arrangement.spacedBy(Space.Small)) { assessment.notes.forEach { Text(it.message) } } }, confirmButton = { TextButton({ showNotes = false }) { Text("Done") } })
    if (confirmFairSave) AlertDialog(onDismissRequest = { confirmFairSave = false }, title = { Text("This lineup has ${assessment.notes.size} fairness notes.") }, text = { Text("The lineup is valid. You can still save the coach’s changes.") }, confirmButton = { TextButton({ confirmFairSave = false; onSaveManualLineup(pending) { exitEditing() } }) { Text("Save Lineup") } }, dismissButton = { TextButton({ confirmFairSave = false }) { Text("Keep Editing") } })

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
            Text("Round ${game.roundNumber} Lineup", style = MaterialTheme.typography.headlineLarge)
            Text("${rotation.title} · ${rotation.subtitle}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RotationSelector(selectedRotation, { index -> selectedRotation = index; selectedPlayerId = null })
        if (editing) Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Editing lineup", style = MaterialTheme.typography.titleMedium)
                Text(if (selectedPlayerId == null) "Tap a player, then tap another player to swap their places." else "Choose a highlighted player to swap, or tap the selected player again to cancel.")
                Row(horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    AssistChip(onClick = { if (assessment.notes.isNotEmpty()) showNotes = true }, label = { Text(assessment.label) }, enabled = assessment.notes.isNotEmpty())
                    if (undoStack.isNotEmpty()) AssistChip(onClick = { pending = undoStack.last(); undoStack = undoStack.dropLast(1); selectedPlayerId = null }, label = { Text("Undo") })
                }
            }
        }
        PitchLineup(playersByPosition, Modifier.fillMaxWidth(), selectedPlayerId, if (editing) ::swapOrSelect else null)
        if (!summary.readyForLineup) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large) { Text("${summary.neededForFullSeven} more ${if (summary.neededForFullSeven == 1) "player" else "players"} needed before a lineup can be generated.", Modifier.fillMaxWidth().padding(Space.Normal)) }
        } else if (!hasLineup) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) { Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Lineup not generated yet", style = MaterialTheme.typography.titleMedium)
                Text("Generate four fair rotations from attendance and played history.", style = MaterialTheme.typography.bodyMedium)
                PrimaryAction(if (busy) "Generating…" else "Generate Fair Rotations", onGenerate, Modifier.fillMaxWidth(), canGenerate)
            } }
        } else if (!editing) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) { Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                Text("Fair rotation generated from attendance and playing history.", style = MaterialTheme.typography.bodyMedium)
                if (game.status == GameStatus.READY) OutlinedButton({ editing = true; pending = persisted }, Modifier.fillMaxWidth(), enabled = !busy) { Text("Edit Lineup") }
                if (game.status != GameStatus.PLAYED) PrimaryAction(if (busy) "Regenerating…" else "Regenerate Rotations", onGenerate, Modifier.fillMaxWidth(), canGenerate)
                if (canMarkPlayed) OutlinedButton({ confirmPlayed = true }, Modifier.fillMaxWidth(), enabled = !busy, contentPadding = PaddingValues(Space.Normal)) { Text("Mark as Played") }
                if (game.status == GameStatus.PLAYED) Text("Played games are locked as historical records.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        BenchStrip(benchPlayers, summary.expectedBench, selectedPlayerId = if (editing) selectedPlayerId else null, onPlayerClick = if (editing) ::swapOrSelect else null)
        if (editing) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
            OutlinedButton(::requestExit, Modifier.weight(1f), enabled = !busy) { Text("Cancel") }
            Button(onClick = { if (assessment.notes.isEmpty()) onSaveManualLineup(pending) { exitEditing() } else confirmFairSave = true }, modifier = Modifier.weight(1f), enabled = !busy) { Text("Save Changes") }
        }
        AttendanceSummaryRow(summary)
    }
}

private fun Assignment.toRotationAssignment() = RotationAssignment(playerId, rotationIndex, playingState, position)

@Preview(showBackground = true)
@Composable
private fun LineupScreenPreview() { StartingSevenTheme { LineupScreen(Game(roundNumber = 1, gameDate = LocalDate.now()), emptyList(), emptyList()) } }
