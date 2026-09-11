package com.sunilsstartingseven.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.data.local.Player
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.ui.theme.*

@Composable
fun PrimaryAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(onClick, modifier.heightIn(min = 52.dp), enabled = enabled, shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Space.Large, vertical = Space.Medium)) { Text(label) }
}

@Composable
fun EmptyCard(title: String, message: String, action: String, onAction: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Medium)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            PrimaryAction(action, onAction)
        }
    }
}

@Composable
fun InitialsAvatar(player: Player, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(44.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "${player.firstName.firstOrNull()?.uppercaseChar() ?: ""}${player.lastInitial}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AttendanceSummaryRow(summary: AttendanceSummary, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
        SummaryPill("${summary.available}", "Playing", Modifier.weight(1f), MaterialTheme.colorScheme.primaryContainer)
        SummaryPill("${summary.away}", "Away", Modifier.weight(1f), MaterialTheme.colorScheme.secondaryContainer)
        SummaryPill("7", "On field", Modifier.weight(1f), MaterialTheme.colorScheme.surfaceContainerHigh)
        SummaryPill("${summary.expectedBench}", "Bench", Modifier.weight(1f), MaterialTheme.colorScheme.tertiaryContainer)
    }
}

@Composable
private fun SummaryPill(value: String, label: String, modifier: Modifier, color: Color) {
    Surface(modifier, color = color, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(horizontal = Space.Small, vertical = Space.Medium), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun RotationSelector(selected: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        RotationPeriods.forEach { rotation ->
            SegmentedButton(
                selected = selected == rotation.index,
                onClick = { onSelected(rotation.index) },
                modifier = Modifier.semantics { contentDescription = rotation.title },
                shape = SegmentedButtonDefaults.itemShape(rotation.index, RotationPeriods.size),
                label = { Text("${rotation.index + 1}") }
            )
        }
    }
}

@Composable
fun SoccerPitch(modifier: Modifier = Modifier) {
    Canvas(modifier.aspectRatio(1.45f).clip(RoundedCornerShape(24.dp)).semantics { contentDescription = "Empty seven-a-side soccer pitch" }) {
        drawRect(Brand.Primary)
        for (stripe in 0..5 step 2) drawRect(Brand.Green, Offset(0f, size.height * stripe / 6), Size(size.width, size.height / 6))
        val inset = size.minDimension * .08f
        val left = inset
        val top = inset
        val w = size.width - 2 * inset
        val h = size.height - 2 * inset
        val line = Color.White.copy(alpha = .75f)
        val stroke = Stroke(2.dp.toPx())
        drawRect(line, Offset(left, top), Size(w, h), style = stroke)
        drawLine(line, Offset(size.width / 2, top), Offset(size.width / 2, top + h), stroke.width)
        drawCircle(line, h * .19f, center, style = stroke)
        drawCircle(line, 2.5.dp.toPx(), center)
        for (right in listOf(false, true)) {
            drawRect(line, Offset(if (right) left + w * .82f else left, top + h * .23f), Size(w * .18f, h * .54f), style = stroke)
            drawRect(line, Offset(if (right) left + w * .93f else left, top + h * .37f), Size(w * .07f, h * .26f), style = stroke)
        }
    }
}

@Composable
fun PositionColor(family: PositionFamily): Color = when (family) {
    PositionFamily.GK -> Brand.Highlight
    PositionFamily.DEF -> Brand.Sky
    PositionFamily.WING -> Color(0xFFCDEFFF)
    PositionFamily.MID -> Brand.Mint
    PositionFamily.ATT -> Color(0xFFD8E5FF)
}

@Composable
fun PitchPlayerCard(player: Player?, position: PlayingPosition, modifier: Modifier = Modifier) {
    val accent = PositionColor(position.family)
    Surface(
        modifier.semantics { contentDescription = "${position.fullLabel} position" },
        color = Color.White,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.widthIn(min = 56.dp, max = 82.dp).padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = accent, shape = MaterialTheme.shapes.small) {
                Text(position.shortLabel, Modifier.padding(horizontal = 7.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
            }
            Text(player?.firstName ?: position.shortLabel, style = MaterialTheme.typography.labelMedium, maxLines = 1, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun PitchLineup(
    playersByPosition: Map<PlayingPosition, Player>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier) {
        SoccerPitch(Modifier.fillMaxWidth())
        val pitchWidth = maxWidth
        val pitchHeight = pitchWidth / 1.45f
        PlayingPosition.entries.forEach { position ->
            PitchPlayerCard(
                player = playersByPosition[position],
                position = position,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = pitchWidth * position.x - 35.dp,
                        y = pitchHeight * position.y - 24.dp
                    )
            )
        }
    }
}

@Composable
fun BenchStrip(players: List<Player>, expectedBench: Int, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
        Text("Bench", style = MaterialTheme.typography.titleMedium)
        Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.large) {
            if (players.isEmpty()) {
                Text(
                    if (expectedBench > 0) "$expectedBench expected on the bench each rotation." else "No bench expected with the current attendance.",
                    Modifier.fillMaxWidth().padding(Space.Normal),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Row(Modifier.padding(Space.Small), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    players.forEach { player ->
                        Surface(color = Color.White, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                            Column(Modifier.padding(Space.Small), horizontalAlignment = Alignment.CenterHorizontally) {
                                InitialsAvatar(player, Modifier.size(36.dp))
                                Text(player.displayName, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PitchPreview() { StartingSevenTheme { SoccerPitch(Modifier.padding(16.dp)) } }

@Preview(showBackground = true)
@Composable
private fun LineupPreview() { StartingSevenTheme { PitchLineup(emptyMap(), Modifier.padding(16.dp).fillMaxWidth()) } }
