package com.sunilsstartingseven.app.ui.screens.team

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.sunilsstartingseven.app.data.local.Player
import com.sunilsstartingseven.app.domain.model.PlayerInput
import com.sunilsstartingseven.app.ui.components.PlayerAvatar
import com.sunilsstartingseven.app.ui.components.PrimaryAction
import com.sunilsstartingseven.app.ui.theme.Space

@Composable
fun PlayerForm(editing: Boolean, initialFirst: String, initialLast: String, busy: Boolean, error: String?, onSave: (String, String) -> Unit, onCancel: () -> Unit, player: Player? = null, onPhoto: (() -> Unit)? = null) {
    var first by rememberSaveable { mutableStateOf(initialFirst) }
    var last by rememberSaveable { mutableStateOf(initialLast) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val validation = PlayerInput.validate(first, last)
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        Text(if (editing) "Edit player" else "A new teammate", style = MaterialTheme.typography.headlineLarge)
        Text("First name. Last initial. Ready to play.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (player != null && onPhoto != null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.Normal)) {
                PlayerAvatar(player, size = 72.dp, onClick = if (!busy) onPhoto else null)
                OutlinedButton(onPhoto, enabled = !busy) { Text(if (player.photoPath == null) "Add Portrait" else "Change Portrait") }
            }
        }
        OutlinedTextField(first, { first = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("First name") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            isError = submitted && validation.firstNameError != null,
            supportingText = { if (submitted) validation.firstNameError?.let { Text(it) } })
        OutlinedTextField(last, { last = PlayerInput.normalizeInitial(it) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text("Last initial") }, singleLine = true,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = submitted && validation.lastInitialError != null,
            supportingText = { Text(if (submitted) validation.lastInitialError ?: "Just one letter — no surname needed." else "Just one letter — no surname needed.") })
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
        PrimaryAction(if (busy) "Saving…" else if (editing) "Save Player" else "Add Player", {
            submitted = true
            if (validation.isValid) onSave(first, last)
        }, Modifier.fillMaxWidth(), !busy)
        OutlinedButton(onCancel, Modifier.fillMaxWidth(), enabled = !busy, contentPadding = PaddingValues(Space.Normal)) { Text("Cancel") }
    }
}
