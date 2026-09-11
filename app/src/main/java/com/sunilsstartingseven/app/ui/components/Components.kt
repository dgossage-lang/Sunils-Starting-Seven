package com.sunilsstartingseven.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.data.local.Player
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.ui.theme.*

private const val PitchAspectRatio = 0.82f

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
    Canvas(modifier.aspectRatio(PitchAspectRatio).clip(RoundedCornerShape(24.dp)).semantics { contentDescription = "Portrait seven-a-side soccer pitch with goals at the top and bottom" }) {
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
        drawLine(line, Offset(left, size.height / 2), Offset(left + w, size.height / 2), stroke.width)
        drawCircle(line, w * .24f, center, style = stroke)
        drawCircle(line, 2.5.dp.toPx(), center)
        for (bottom in listOf(false, true)) {
            val penaltyHeight = h * .18f
            val goalAreaHeight = h * .07f
            drawRect(
                line,
                Offset(left + w * .23f, if (bottom) top + h - penaltyHeight else top),
                Size(w * .54f, penaltyHeight),
                style = stroke
            )
            drawRect(
                line,
                Offset(left + w * .37f, if (bottom) top + h - goalAreaHeight else top),
                Size(w * .26f, goalAreaHeight),
                style = stroke
            )
            drawRect(
                line,
                Offset(left + w * .42f, if (bottom) top + h else top - h * .025f),
                Size(w * .16f, h * .025f),
                style = stroke
            )
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
fun PitchPlayerCard(
    player: Player?,
    position: PlayingPosition,
    modifier: Modifier = Modifier,
    width: Dp = 74.dp,
    height: Dp = 56.dp
) {
    val accent = PositionColor(position.family)
    Surface(
        modifier.width(width).height(height).semantics { contentDescription = "${position.fullLabel} position" },
        color = Color.White,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column(Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            if (player == null) {
                Text(position.shortLabel, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            } else {
                Text(
                    player.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    position.shortLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PitchLineup(
    playersByPosition: Map<PlayingPosition, Player>,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier.aspectRatio(PitchAspectRatio)) {
        SoccerPitch(Modifier.fillMaxSize())
        val pitchWidth = maxWidth
        val pitchHeight = maxHeight
        val cardWidth = minOf(74.dp, pitchWidth * .23f)
        val cardHeight = minOf(56.dp, cardWidth * .76f)
        PlayingPosition.entries.forEach { position ->
            PitchPlayerCard(
                player = playersByPosition[position],
                position = position,
                width = cardWidth,
                height = cardHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = pitchWidth * position.x - cardWidth / 2,
                        y = pitchHeight * position.y - cardHeight / 2
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
                Row(Modifier.padding(Space.Small).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
                    players.forEach { player ->
                        Surface(color = Color.White, shape = MaterialTheme.shapes.medium, tonalElevation = 2.dp) {
                            Column(Modifier.padding(Space.Small), horizontalAlignment = Alignment.CenterHorizontally) {
                                InitialsAvatar(player, Modifier.size(36.dp))
                                Text(
                                    player.displayName,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
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

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun PopulatedLineupPreview() {
    StartingSevenTheme {
        PitchLineup(
            mapOf(
                PlayingPosition.GOALKEEPER to Player(firstName = "Mason", lastInitial = "G"),
                PlayingPosition.STRIKER to Player(firstName = "Alex", lastInitial = "T")
            ),
            Modifier.padding(16.dp).fillMaxWidth()
        )
    }
}
