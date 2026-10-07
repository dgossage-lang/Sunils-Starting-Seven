package com.sunilsstartingseven.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.sunilsstartingseven.app.data.local.AppDatabase
import com.sunilsstartingseven.app.data.backup.BackupRepository
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.media.MediaRepository
import com.sunilsstartingseven.app.data.repository.GameRepository
import com.sunilsstartingseven.app.data.repository.PlayerRepository
import com.sunilsstartingseven.app.data.repository.RosterRepository

class SunilsStartingSevenApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "starting-seven.db")
        .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
        .build()
    val media = MediaRepository(context.applicationContext, database)
    val players = PlayerRepository(database.playerDao(), media)
    val seasons = com.sunilsstartingseven.app.data.repository.SeasonRepository(database.seasonDao())
    val games = GameRepository(database.gameDao(), media, database)
    val roster = RosterRepository(database)
    val preferences = PreferenceRepository(context)
    val backup = BackupRepository(database, preferences, media)
}
