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
    assignments: List<AssignmentWithPlayer>
) {
    var selectedRotation by rememberSaveable(game.id) { mutableIntStateOf(0) }
    val summary = attendanceSummary(attendance.map { it.attendance.status })
    val rotation = RotationPeriods[selectedRotation]
    val rotationAssignments = assignments.filter { it.assignment.rotationIndex == selectedRotation }
    val playersByPosition = rotationAssignments
        .filter { it.assignment.playingState == PlayingState.ON_FIELD && it.assignment.position != null }
        .associate { it.assignment.position!! to it.player }
    val benchPlayers = rotationAssignments.filter { it.assignment.playingState == PlayingState.BENCH }.map { it.player }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.Tiny)) {
            Text("Round ${game.roundNumber} Lineup", style = MaterialTheme.typography.headlineLarge)
            Text("${rotation.title} · ${rotation.subtitle}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RotationSelector(selectedRotation, { selectedRotation = it })
        PitchLineup(playersByPosition, Modifier.fillMaxWidth())
        if (rotationAssignments.isEmpty()) {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(Space.Normal), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
                    Text("Lineup not generated yet", style = MaterialTheme.typography.titleMedium)
                    Text("The seven spots are ready once rotations have been created.", style = MaterialTheme.typography.bodyMedium)
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
