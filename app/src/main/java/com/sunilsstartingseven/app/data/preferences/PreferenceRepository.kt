package com.sunilsstartingseven.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore(name = "team_preferences")

class PreferenceRepository(context: Context) {
    private val store = context.applicationContext.preferences
    private val teamNameKey = stringPreferencesKey("team_name")
    val teamName = store.data.map { it[teamNameKey].orEmpty() }
    suspend fun setTeamName(name: String) { store.edit { it[teamNameKey] = name.trim() } }
}
