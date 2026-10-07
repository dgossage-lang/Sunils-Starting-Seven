package com.sunilsstartingseven.app.ui.screens.games

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.sunilsstartingseven.app.data.local.Season
import com.sunilsstartingseven.app.ui.theme.Space

@Composable
fun SeasonSelector(seasons: List<Season>, selectedId: Long?, enabled: Boolean, onSelect: (Long?) -> Unit, onManage: () -> Unit, allSeasons: Boolean = true) {
    var expanded by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = Space.Large), horizontalArrangement = Arrangement.spacedBy(Space.Small)) {
        Box(Modifier.weight(1f)) {
            OutlinedButton({ expanded = true }, Modifier.fillMaxWidth(), enabled = enabled && seasons.isNotEmpty()) {
                Text(seasons.find { it.id == selectedId }?.name ?: if (allSeasons) "All seasons" else seasons.firstOrNull()?.name ?: "First season")
            }
            DropdownMenu(expanded, { expanded = false }) {
                if (allSeasons) DropdownMenuItem(text = { Text("All seasons") }, onClick = { onSelect(null); expanded = false })
                seasons.forEach { season -> DropdownMenuItem(text = { Text(season.name) }, onClick = { onSelect(season.id); expanded = false }) }
            }
        }
        TextButton(onManage, enabled = enabled) { Text("Seasons") }
    }
}

@Composable
fun SeasonsScreen(seasons: List<Season>, gameCounts: Map<Long?, Int>, busy: Boolean, onSave: (Long?, String, () -> Unit) -> Unit, onOpen: (Long) -> Unit) {
    var editing by rememberSaveable { mutableStateOf(false) }
    var editId by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(Space.Large), verticalArrangement = Arrangement.spacedBy(Space.Normal)) {
        item { Text("Your seasons", style = MaterialTheme.typography.headlineLarge) }
        item { Text("Keep game history together by season. Players stay in your team across seasons.") }
        item { Button({ editId = null; name = ""; editing = true }, enabled = !busy) { Text("New Season") } }
        items(seasons, key = { it.id }) { season ->
            Card(onClick = { onOpen(season.id) }, enabled = !busy) {
                Column(Modifier.fillMaxWidth().padding(Space.Normal)) {
                    Text(season.name, style = MaterialTheme.typography.titleLarge)
                    Text("${gameCounts[season.id] ?: 0} games")
                    TextButton({ editId = season.id; name = season.name; editing = true }, enabled = !busy) { Text("Rename") }
                }
            }
        }
    }
    if (editing) AlertDialog(onDismissRequest = { if (!busy) editing = false }, title = { Text(if (editId == null) "New season" else "Rename season") },
        text = { OutlinedTextField(name, { name = it }, label = { Text("Season name") }, singleLine = true, enabled = !busy) },
        confirmButton = { TextButton({ onSave(editId, name) { editing = false } }, enabled = !busy && name.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton({ editing = false }, enabled = !busy) { Text("Cancel") } })
}
