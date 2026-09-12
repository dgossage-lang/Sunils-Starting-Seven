package com.sunilsstartingseven.app.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunilsstartingseven.app.R
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
fun PhotoAvatar(photoPath: String?, initials: String, contentDescription: String?, modifier: Modifier = Modifier, size: Dp = 44.dp, onClick: (() -> Unit)? = null) {
    val context = LocalContext.current
    val bitmap = remember(photoPath) {
        photoPath?.let { path -> runCatching { BitmapFactory.decodeFile(java.io.File(context.filesDir, path).absolutePath)?.asImageBitmap() }.getOrNull() }
    }
    val base = modifier.size(size).clip(MaterialTheme.shapes.large).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    if (bitmap != null) {
        Image(bitmap, contentDescription, base, contentScale = ContentScale.Crop)
    } else {
        Image(
            painter = painterResource(R.drawable.player_silhouette_placeholder),
            contentDescription = contentDescription,
            modifier = base,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun PlayerAvatar(player: Player, modifier: Modifier = Modifier, size: Dp = 44.dp, onClick: (() -> Unit)? = null) {
    PhotoAvatar(player.photoPath, player.initials(), player.displayName, modifier, size, onClick)
}

@Composable
fun InitialsAvatar(player: Player, modifier: Modifier = Modifier) { PlayerAvatar(player, modifier) }

fun Player.initials(): String = "${firstName.firstOrNull()?.uppercaseChar() ?: ""}$lastInitial".ifBlank { "?" }
fun displayInitials(displayName: String): String = displayName.split(" ").filter { it.isNotBlank() }.take(2).mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }.joinToString("").ifBlank { "?" }


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
private fun PositionTextColor(family: PositionFamily): Color = when (family) {
    PositionFamily.GK -> Brand.Warning
    PositionFamily.DEF -> Brand.SecondaryNavy
    PositionFamily.WING -> Brand.AccentBlue
    PositionFamily.MID -> Brand.Primary
    PositionFamily.ATT -> Brand.SecondaryNavy
}

@Composable
private fun PitchPortraitTile(
    player: Player,
    modifier: Modifier = Modifier,
    width: Dp = 30.dp,
    height: Dp = 40.dp
) {
    val context = LocalContext.current
    val bitmap = remember(player.photoPath) {
        player.photoPath?.let { path ->
            runCatching { BitmapFactory.decodeFile(java.io.File(context.filesDir, path).absolutePath)?.asImageBitmap() }.getOrNull()
        }
    }
    val tile = modifier
        .width(width)
        .height(height)
        .clip(RoundedCornerShape(10.dp))
    if (bitmap != null) {
        Image(bitmap, player.displayName, tile, contentScale = ContentScale.Crop)
    } else {
        Image(
            painter = painterResource(R.drawable.player_silhouette_placeholder),
            contentDescription = "${player.displayName} silhouette",
            modifier = tile,
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun PitchPlayerCard(
    player: Player?,
    position: PlayingPosition,
    modifier: Modifier = Modifier,
    width: Dp = 82.dp,
    height: Dp = 69.dp,
    selected: Boolean = false,
    target: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val positionColor = PositionTextColor(position.family)
    val portraitWidth = minOf(29.dp, height * .42f)
    val portraitHeight = minOf(39.dp, height * .56f)
    Surface(
        modifier.width(width).height(height)
            .clip(MaterialTheme.shapes.medium)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .semantics { contentDescription = if (player == null) "${position.fullLabel} position" else "${player.displayName}, ${position.fullLabel}" },
        color = when { selected -> MaterialTheme.colorScheme.secondaryContainer; target -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.surfaceContainerLow },
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 3.dp,
        shadowElevation = if (selected) 6.dp else 2.dp,
        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else if (target) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        if (player == null) {
            Box(Modifier.fillMaxSize().padding(Space.Small), contentAlignment = Alignment.Center) {
                Text(
                    position.shortLabel,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 20.sp),
                    color = positionColor,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    player.displayName,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 13.sp),
                    color = Brand.Navy,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PitchPortraitTile(player, width = portraitWidth, height = portraitHeight)
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(
                            position.shortLabel,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp, lineHeight = 20.sp),
                            color = positionColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PitchLineup(
    playersByPosition: Map<PlayingPosition, Player>,
    modifier: Modifier = Modifier,
    selectedPlayerId: Long? = null,
    onPlayerClick: ((Long) -> Unit)? = null
) {
    BoxWithConstraints(modifier.aspectRatio(PitchAspectRatio)) {
        SoccerPitch(Modifier.fillMaxSize())
        val pitchWidth = maxWidth
        val pitchHeight = maxHeight
        val cardWidth = minOf(84.dp, maxOf(78.dp, pitchWidth * .245f))
        val cardHeight = minOf(72.dp, maxOf(66.dp, cardWidth * .84f))
        PlayingPosition.entries.forEach { position ->
            val visualY = if (position == PlayingPosition.GOALKEEPER) position.y + .025f else position.y
            PitchPlayerCard(
                player = playersByPosition[position],
                position = position,
                selected = playersByPosition[position]?.id == selectedPlayerId,
                target = selectedPlayerId != null && playersByPosition[position]?.id != selectedPlayerId,
                onClick = playersByPosition[position]?.id?.let { id -> onPlayerClick?.let { click -> { click(id) } } },
                width = cardWidth,
                height = cardHeight,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = pitchWidth * position.x - cardWidth / 2,
                        y = pitchHeight * visualY - cardHeight / 2
                    )
            )
        }
    }
}

@Composable
fun BenchStrip(players: List<Player>, expectedBench: Int, modifier: Modifier = Modifier, selectedPlayerId: Long? = null, onPlayerClick: ((Long) -> Unit)? = null) {
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
                        val selected = player.id == selectedPlayerId
                        Surface(
                            modifier = Modifier.clip(MaterialTheme.shapes.medium).then(if (onPlayerClick != null) Modifier.clickable { onPlayerClick(player.id) } else Modifier),
                            color = if (selected) MaterialTheme.colorScheme.secondaryContainer else if (selectedPlayerId != null) MaterialTheme.colorScheme.primaryContainer else Color.White,
                            shape = MaterialTheme.shapes.medium,
                            tonalElevation = if (selected) 6.dp else 2.dp,
                            border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.secondary) else if (selectedPlayerId != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                        ) {
                            Column(Modifier.padding(Space.Small), horizontalAlignment = Alignment.CenterHorizontally) {
                                PlayerAvatar(player, Modifier, 36.dp)
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
                PlayingPosition.GOALKEEPER to Player(1, "Nick", "", photoPath = "media/players/preview-nick.jpg"),
                PlayingPosition.LEFT_WING to Player(2, "Cameron", "", photoPath = "media/players/preview-cameron.jpg"),
                PlayingPosition.MID to Player(3, "Samrith", "", photoPath = "media/players/preview-samrith.jpg"),
                PlayingPosition.RIGHT_WING to Player(4, "Axel", ""),
                PlayingPosition.DEFENDER_1 to Player(5, "Artie", "G"),
                PlayingPosition.DEFENDER_2 to Player(6, "Roman", ""),
                PlayingPosition.STRIKER to Player(7, "Hughie", "")
            ),
            Modifier.padding(16.dp).fillMaxWidth(),
            selectedPlayerId = 5
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PitchCardShortNameGkSelectedPreview() {
    StartingSevenTheme { PitchPlayerCard(Player(1, "Nick", "", photoPath = "media/players/preview-nick.jpg"), PlayingPosition.GOALKEEPER, selected = true) }
}

@Preview(showBackground = true)
@Composable
private fun PitchCardLongNameMidPreview() {
    StartingSevenTheme { PitchPlayerCard(Player(2, "Cameron", ""), PlayingPosition.MID) }
}

@Preview(showBackground = true)
@Composable
private fun PitchCardWingTargetPreview() {
    StartingSevenTheme { PitchPlayerCard(Player(3, "Artie", "G"), PlayingPosition.LEFT_WING, target = true) }
}

@Preview(showBackground = true)
@Composable
private fun PitchCardEmptyPreview() {
    StartingSevenTheme { PitchPlayerCard(null, PlayingPosition.STRIKER) }
}

@Preview(showBackground = true, widthDp = 320)
@Composable
private fun PitchCardMidfieldRowPreview() {
    StartingSevenTheme {
        Row(Modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PitchPlayerCard(Player(1, "Cameron", ""), PlayingPosition.LEFT_WING, width = 82.dp, height = 69.dp)
            PitchPlayerCard(Player(2, "Samrith", ""), PlayingPosition.MID, width = 82.dp, height = 69.dp)
            PitchPlayerCard(Player(3, "Macson", ""), PlayingPosition.RIGHT_WING, width = 82.dp, height = 69.dp)
        }
    }
}


@Preview(showBackground = true, widthDp = 240, heightDp = 180)
@Composable
private fun PitchCardDefenceGoalkeeperSpacingPreview() {
    StartingSevenTheme {
        Box(Modifier.padding(8.dp).width(220.dp).height(160.dp)) {
            PitchPlayerCard(Player(1, "Artie", "G"), PlayingPosition.DEFENDER_1, modifier = Modifier.align(Alignment.TopStart))
            PitchPlayerCard(Player(2, "Roman", ""), PlayingPosition.DEFENDER_2, modifier = Modifier.align(Alignment.TopEnd))
            PitchPlayerCard(Player(3, "Nick", ""), PlayingPosition.GOALKEEPER, selected = true, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}
