package com.sunilsstartingseven.app.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.sunilsstartingseven.app.ui.components.PrimaryAction
import com.sunilsstartingseven.app.ui.theme.Space
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameForm(
    busy: Boolean,
    error: String?,
    initialRound: Int? = null,
    initialDate: LocalDate = LocalDate.now(),
    initialNotes: String = "",
    title: String = "Plan a game day",
    onSave: (Int, LocalDate, String) -> Unit,
    onCancel: () -> Unit
) {
    var round by rememberSaveable { mutableStateOf(initialRound?.toString().orEmpty()) }
    var dateDay by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    var notes by rememberSaveable { mutableStateOf(initialNotes) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val date = LocalDate.ofEpochDay(dateDay)
    val roundNumber = round.toIntOrNull()
    val roundInvalid = roundNumber == null || roundNumber <= 0
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text("Save the basics as a draft.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(round, { round = it }, Modifier.fillMaxWidth(), label = { Text("Round number") }, enabled = !busy, singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), isError = submitted && roundInvalid,
            supportingText = { if (submitted && roundInvalid) Text("Enter a whole round number greater than zero.") })
        Text("Game date", style = MaterialTheme.typography.labelLarge)
        OutlinedButton({ showPicker = true }, Modifier.fillMaxWidth(), enabled = !busy, contentPadding = PaddingValues(Space.Normal)) {
            Icon(Icons.Outlined.DateRange, null)
            Spacer(Modifier.width(Space.Small))
            Text(date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)))
        }
        OutlinedTextField(notes, { notes = it }, Modifier.fillMaxWidth(), label = { Text("Notes (optional)") }, minLines = 3, enabled = !busy)
        Text("Draft • Stored on this device", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryAction(if (busy) "Saving…" else "Save Draft", {
            submitted = true
            if (!roundInvalid) onSave(roundNumber, date, notes)
        }, Modifier.fillMaxWidth(), !busy)
        OutlinedButton(onCancel, Modifier.fillMaxWidth(), enabled = !busy, contentPadding = PaddingValues(Space.Normal)) { Text("Cancel") }
    }
    if (showPicker) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli())
        DatePickerDialog(onDismissRequest = { showPicker = false }, confirmButton = {
            TextButton({
                picker.selectedDateMillis?.let { dateDay = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay() }
                showPicker = false
            }, enabled = picker.selectedDateMillis != null) { Text("Use Date") }
        }, dismissButton = { TextButton({ showPicker = false }) { Text("Cancel") } }) { DatePicker(picker) }
    }
}
