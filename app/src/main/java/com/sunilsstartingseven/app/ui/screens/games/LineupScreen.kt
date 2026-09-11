package com.sunilsstartingseven.app.ui.screens.games

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
import com.sunilsstartingseven.app.ui.components.*
import com.sunilsstartingseven.app.ui.theme.*
import java.time.LocalDate

@Composable
fun LineupScreen(
    game: Game,
    attendance: List<AttendanceWithPlayer>,
    assignments: List<AssignmentWithPlayer>,
    busy: Boolean = false,
    onGenerate: () -> Unit = {},
    onMarkPlayed: () -> Unit = {}
) {
    var selectedRotation by rememberSaveable(game.id) { mutableIntStateOf(0) }
    var confirmPlayed by rememberSaveable { mutableStateOf(false) }
    val summary = attendanceSummary(attendance.map { it.attendance.status })
    val rotation = RotationPeriods[selectedRotation]
    val rotationAssignments = assignments.filter { it.assignment.rotationIndex == selectedRotation }
    val playersByPosition = rotationAssignments
        .filter { it.assignment.playingState == PlayingState.ON_FIELD && it.assignment.position != null }
        .associate { it.assignment.position!! to it.player }
    val benchPlayers = rotationAssignments.filter { it.assignment.playingState == PlayingState.BENCH }.map { it.player }
    val hasLineup = assignments.isNotEmpty()
    val canGenerate = !busy && summary.readyForLineup && game.status != GameStatus.PLAYED
    val canMarkPlayed = !busy && game.status == GameStatus.READY && hasLineup

    if (confirmPlayed) {
        AlertDialog(
            onDismissRequest = { confirmPlayed = false },
            title = { Text("Mark Round ${game.roundNumber} as played?") },
            text = { Text("This roster will be included in future fairness calculations.") },
            confirmButton = { TextButton({ confirmPlayed = false; onMarkPlayed() }) { Text("Mark Played") } },
            dismissButton = { TextButton({ confirmPlayed = false }) { Text("Cancel") } }
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
            Text("Round ${game.roundNumber} Lineup", style = MaterialTheme.typography.headlineLarge)
            Text("${rotation.title} · ${rotation.subtitle}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RotationSelector(selectedRotation, { selectedRotation = it })
        PitchLineup(playersByPosition, Modifier.fillMaxWidth())
        if (!summary.readyForLineup) {
            Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = MaterialTheme.shapes.large) {
                Text("${summary.neededForFullSeven} more ${if (summary.neededForFullSeven == 1) "player" else "players"} needed before a lineup can be generated.", Modifier.fillMaxWidth().padding(Space.Normal))
            }
        } else if (!hasLineup) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                    Text("Lineup not generated yet", style = MaterialTheme.typography.titleMedium)
                    Text("Generate four fair rotations from attendance and played history.", style = MaterialTheme.typography.bodyMedium)
                    PrimaryAction(if (busy) "Generating…" else "Generate Fair Rotations", onGenerate, Modifier.fillMaxWidth(), canGenerate)
                }
            }
        } else {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                    Text("Fair rotation generated from attendance and playing history.", style = MaterialTheme.typography.bodyMedium)
                    if (game.status != GameStatus.PLAYED) PrimaryAction(if (busy) "Regenerating…" else "Regenerate Rotations", onGenerate, Modifier.fillMaxWidth(), canGenerate)
                    if (canMarkPlayed) OutlinedButton({ confirmPlayed = true }, Modifier.fillMaxWidth(), enabled = !busy, contentPadding = PaddingValues(Space.Normal)) { Text("Mark as Played") }
                    if (game.status == GameStatus.PLAYED) Text("Played games are locked as historical records.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        BenchStrip(benchPlayers, summary.expectedBench)
        AttendanceSummaryRow(summary)
    }
}

@Preview(showBackground = true)
@Composable
private fun LineupScreenPreview() {
    StartingSevenTheme { LineupScreen(Game(roundNumber = 1, gameDate = LocalDate.now()), emptyList(), emptyList()) }
}
