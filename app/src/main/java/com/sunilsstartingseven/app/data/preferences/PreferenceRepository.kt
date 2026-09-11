package com.sunilsstartingseven.app.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.preferences by preferencesDataStore(name = "team_preferences")

class PreferenceRepository(context: Context) {
    private val store = context.applicationContext.preferences
    private val teamNameKey = stringPreferencesKey("team_name")
    private val developmentSeedVersionKey = intPreferencesKey("development_seed_version")
    val teamName = store.data.map { it[teamNameKey].orEmpty() }
    suspend fun setTeamName(name: String) { store.edit { it[teamNameKey] = name.trim() } }
    suspend fun developmentSeedVersion(): Int = store.data.map { it[developmentSeedVersionKey] ?: 0 }.first()
    suspend fun setDevelopmentSeedVersion(version: Int) { store.edit { it[developmentSeedVersionKey] = version } }
}
