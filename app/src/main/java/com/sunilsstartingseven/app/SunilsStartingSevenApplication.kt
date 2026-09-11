package com.sunilsstartingseven.app

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.sunilsstartingseven.app.data.local.AppDatabase
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.GameRepository
import com.sunilsstartingseven.app.data.repository.PlayerRepository
import com.sunilsstartingseven.app.data.repository.RosterRepository

class SunilsStartingSevenApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

class AppContainer(context: Context) {
    val database = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "starting-seven.db").build()
    val players = PlayerRepository(database.playerDao())
    val games = GameRepository(database.gameDao())
    val roster = RosterRepository(database)
    val preferences = PreferenceRepository(context)
}
