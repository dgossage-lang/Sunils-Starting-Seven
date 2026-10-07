package com.sunilsstartingseven.app.ui.screens.games

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.data.local.Game
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.ui.components.SoccerPitch
import com.sunilsstartingseven.app.ui.theme.Space
import java.util.UUID

@Composable
fun PitchLayoutScreen(game: Game, available: Int, busy: Boolean, onSave: (List<PlayingPosition>, List<PlayingPosition>, () -> Unit) -> Unit, onBack: () -> Unit) {
    var optionStrings by rememberSaveable(game.id, game.positionOptions) { mutableStateOf((PlayingPosition.entries + game.positionOptions + game.fieldPositions).associateBy { it.name }.values.map(PositionCodec::encode)) }
    var activeIds by rememberSaveable(game.id, game.fieldPositions) { mutableStateOf(game.fieldPositions.map { it.name }) }
    var confirmSave by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var editString by rememberSaveable { mutableStateOf<String?>(null) }
    var movingId by rememberSaveable { mutableStateOf<String?>(null) }
    val options = optionStrings.map(PositionCodec::decode)
    val positions = options.filter { it.name in activeIds }
    val layoutChanged = PositionCodec.encodeLayout(positions) != PositionCodec.encodeLayout(game.fieldPositions)
    val changed = PositionCodec.encodeLayout(positions) != PositionCodec.encodeLayout(game.fieldPositions) || PositionCodec.encodeLayout(options) != PositionCodec.encodeLayout(game.positionOptions)
    val editable = game.status == GameStatus.DRAFT || game.status == GameStatus.READY
    fun back() { if (changed && editable) confirmDiscard = true else onBack() }
    fun updateOption(position: PlayingPosition) {
        val current = optionStrings.map(PositionCodec::decode)
        optionStrings = current.map { if (it.name == position.name) position else it }
            .let { if (current.none { p -> p.name == position.name }) it + position else it }.map(PositionCodec::encode)
    }
    BackHandler { if (!busy) back() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Text("Round ${game.roundNumber} pitch layout", style = MaterialTheme.typography.headlineMedium)
        Text(if (editable) "Tap a standard position to add or remove it. Tap a custom position to edit it." else "This played game's pitch layout is saved with its history.")
        Text("${positions.size} on field · ${maxOf(available - positions.size, 0)} on bench · $available playing")
        if (available < positions.size) Text("${positions.size - available} more players needed for this layout.", color = MaterialTheme.colorScheme.error)
        movingId?.let { Text("Drag ${options.find { p -> p.name == it }?.fullLabel} on the pitch and release to place it. You can also tap an empty spot or use the sliders.") }
        BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(.78f)) {
            SoccerPitch(Modifier.fillMaxSize().pointerInput(movingId, busy, optionStrings) {
                detectTapGestures { offset ->
                    if (movingId != null && !busy && editable) {
                        options.find { it.name == movingId }?.let { updateOption(it.copy(x = (offset.x / size.width).coerceIn(.12f, .88f), y = (offset.y / size.height).coerceIn(.12f, .88f))) }
                        movingId = null
                    }
                }
            })
            val width = maxWidth
            val height = maxHeight
            val density = LocalDensity.current
            val widthPx = with(density) { width.toPx() }
            val heightPx = with(density) { height.toPx() }
            options.filter { !it.isCustom || it.name in activeIds || it.name == movingId }.forEach { position ->
                val included = position in positions
                FilledTonalButton(
                    onClick = { if (movingId == null) { if (position.isCustom) editString = PositionCodec.encode(position) else activeIds = if (included) activeIds - position.name else activeIds + position.name } },
                    enabled = editable && !busy && (movingId == null || movingId == position.name) && (position.isCustom || if (included) positions.size > 1 else positions.size < MAX_FIELD_POSITIONS),
                    modifier = Modifier.align(Alignment.TopStart).offset(width * position.x - 32.dp, height * position.y - 24.dp).width(64.dp).heightIn(min = 48.dp)
                        .alpha(if (included) 1f else .5f)
                        .pointerInput(position.name, movingId, busy, editable, widthPx, heightPx) {
                            if (position.isCustom && movingId == position.name && editable && !busy) {
                                var start = position
                                var dragged = position
                                detectDragGestures(
                                    onDragStart = {
                                        start = optionStrings.map(PositionCodec::decode).first { it.name == position.name }
                                        dragged = start
                                    },
                                    onDragEnd = { movingId = null },
                                    onDragCancel = { updateOption(start) },
                                    onDrag = { change, delta ->
                                        change.consume()
                                        dragged = dragged.copy(x = (dragged.x + delta.x / widthPx).coerceIn(.12f, .88f), y = (dragged.y + delta.y / heightPx).coerceIn(.12f, .88f))
                                        updateOption(dragged)
                                    }
                                )
                            }
                        }
                        .semantics {
                            contentDescription = if (position.isCustom) "${if (movingId == position.name) "Move" else "Edit"} ${position.fullLabel}" else "${if (included) "Remove" else "Add"} ${position.fullLabel}"
                            stateDescription = if (included) "On field" else "Off field"
                            selected = included
                        },
                    contentPadding = PaddingValues(2.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = if (included) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        disabledContainerColor = if (included) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        disabledContentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) { Text(position.shortLabel) }
            }
        }
        if (editable) {
            OutlinedButton({
                val points = listOf(.2f, .5f, .8f).flatMap { x -> listOf(.16f, .34f, .52f, .70f, .86f).map { y -> x to y } }
                val location = points.maxBy { point -> options.filter { !it.isCustom || it.name in activeIds }.minOfOrNull { p -> kotlin.math.abs(p.x - point.first) + kotlin.math.abs(p.y - point.second) } ?: 1f }
                editString = PositionCodec.encode(PlayingPosition("CUSTOM_${UUID.randomUUID()}", PositionFamily.MID, "CM", "Centre midfield", location.first, location.second))
            }, Modifier.fillMaxWidth(), enabled = !busy && options.size < 40) { Text("Add Custom Position") }
            options.filter { it.isCustom }.forEach { position ->
                OutlinedButton({ editString = PositionCodec.encode(position) }, Modifier.fillMaxWidth(), enabled = !busy) { Text("${position.shortLabel} · ${position.fullLabel} · ${if (position.name in activeIds) "On field" else "Available option"}") }
            }
            options.find { it.name == movingId }?.let { position ->
                Text("Horizontal position")
                Slider(position.x, { updateOption(position.copy(x = it)) }, valueRange = .12f.. .88f, enabled = !busy)
                Text("Vertical position")
                Slider(position.y, { updateOption(position.copy(y = it)) }, valueRange = .12f.. .88f, enabled = !busy)
                TextButton({ movingId = null }) { Text("Done Moving") }
            }
        }
        Text("New games inherit the most recently created game's layout in this season. A season's first game uses the latest game from your other seasons. Player assignments are generated separately.", style = MaterialTheme.typography.bodySmall)
        if (editable) Button({ if (game.status == GameStatus.READY && layoutChanged) confirmSave = true else onSave(positions, options, onBack) }, Modifier.fillMaxWidth(), enabled = changed && !busy && movingId == null) { Text("Save Layout") }
        OutlinedButton(::back, Modifier.fillMaxWidth(), enabled = !busy) { Text(if (changed && editable) "Cancel" else "Back") }
    }
    editString?.let { encoded ->
        val position = PositionCodec.decode(encoded)
        CustomPositionDialog(position, position.name in activeIds, positions.size, busy, { editString = null },
            existing = options.any { it.name == position.name },
            onDelete = {
                optionStrings = options.filter { it.name != position.name }.map(PositionCodec::encode)
                activeIds = activeIds - position.name
                if (movingId == position.name) movingId = null
                editString = null
            }) { updated, active, move ->
            updateOption(updated)
            activeIds = if (active) (activeIds + updated.name).distinct() else activeIds - updated.name
            editString = null
            if (move) movingId = updated.name
        }
    }
    if (confirmSave) AlertDialog(onDismissRequest = { confirmSave = false }, title = { Text("Replace this game's layout?") },
        text = { Text("This clears the existing lineup, including manual swaps. Attendance stays saved. Generate new fair rotations after saving the layout.") },
        confirmButton = { TextButton({ confirmSave = false; onSave(positions, options, onBack) }, enabled = !busy) { Text("Save Layout") } },
        dismissButton = { TextButton({ confirmSave = false }) { Text("Keep Current Layout") } })
    if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false }, title = { Text("Discard layout changes?") },
        confirmButton = { TextButton({ confirmDiscard = false; onBack() }) { Text("Discard") } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text("Keep Editing") } })
}

@Composable
private fun CustomPositionDialog(position: PlayingPosition, wasActive: Boolean, activeCount: Int, busy: Boolean, onDismiss: () -> Unit, existing: Boolean, onDelete: () -> Unit, onApply: (PlayingPosition, Boolean, Boolean) -> Unit) {
    var name by rememberSaveable(position.name) { mutableStateOf(position.fullLabel) }
    var label by rememberSaveable(position.name) { mutableStateOf(position.shortLabel) }
    var familyName by rememberSaveable(position.name) { mutableStateOf(position.family.name) }
    var active by rememberSaveable(position.name) { mutableStateOf(wasActive || (!existing && activeCount < MAX_FIELD_POSITIONS)) }
    var familyExpanded by remember { mutableStateOf(false) }
    val valid = name.trim().isNotEmpty() && name.trim().length <= 40 && label.trim().isNotEmpty() && label.trim().uppercase().length <= 5 &&
        (active || !wasActive || activeCount > 1) && (!active || wasActive || activeCount < MAX_FIELD_POSITIONS)
    fun apply(move: Boolean) { onApply(position.copy(fullLabel = name.trim(), shortLabel = label.trim().uppercase(), family = PositionFamily.valueOf(familyName)), active, move) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Custom position") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(Space.Small)) {
            OutlinedTextField(name, { name = it }, label = { Text("Position name") }, singleLine = true, enabled = !busy, supportingText = { Text("Up to 40 characters") })
            OutlinedTextField(label, { label = it }, label = { Text("Short label") }, singleLine = true, enabled = !busy, supportingText = { Text("Up to 5 characters") })
            Box {
                OutlinedButton({ familyExpanded = true }, enabled = !busy) { Text("Role group: $familyName") }
                DropdownMenu(familyExpanded, { familyExpanded = false }) { PositionFamily.entries.forEach { family -> DropdownMenuItem(text = { Text(family.name) }, onClick = { familyName = family.name; familyExpanded = false }) } }
            }
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(active, { active = it }, enabled = !busy && (wasActive || activeCount < MAX_FIELD_POSITIONS)); Text("On field for this game") }
            Text("Removed custom positions remain available for later games. Up to $MAX_FIELD_POSITIONS positions can be on field.", style = MaterialTheme.typography.bodySmall)
            TextButton({ apply(true) }, enabled = !busy && valid) { Text("Place on Pitch") }
            if (existing && position.isCustom) {
                TextButton(onDelete, enabled = !busy && (!wasActive || activeCount > 1), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Delete Position") }
                Text("Delete removes this option when you save the layout. Earlier games keep their history. Keep at least one position on field.", style = MaterialTheme.typography.bodySmall)
            }
        } },
        confirmButton = { TextButton({ apply(false) }, enabled = !busy && valid) { Text("Use Position") } },
        dismissButton = { TextButton(onDismiss, enabled = !busy) { Text("Cancel") } })
}
