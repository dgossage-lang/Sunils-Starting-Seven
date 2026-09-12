package com.sunilsstartingseven.app

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MigrationTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun migration1To2AddsNullablePhotoFieldsAndPreservesRows() {
        runBlocking {
        val name = "migration-1-2-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.version = 1
            db.execSQL("CREATE TABLE players (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, firstName TEXT NOT NULL, lastInitial TEXT NOT NULL, isActive INTEGER NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE games (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, roundNumber INTEGER NOT NULL, gameDate INTEGER NOT NULL, status TEXT NOT NULL, notes TEXT NOT NULL, createdAt INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE attendance (gameId INTEGER NOT NULL, playerId INTEGER NOT NULL, status TEXT NOT NULL, PRIMARY KEY(gameId, playerId), FOREIGN KEY(gameId) REFERENCES games(id) ON DELETE RESTRICT, FOREIGN KEY(playerId) REFERENCES players(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE TABLE assignments (gameId INTEGER NOT NULL, rotationIndex INTEGER NOT NULL, playerId INTEGER NOT NULL, playingState TEXT NOT NULL, position TEXT, PRIMARY KEY(gameId, rotationIndex, playerId), FOREIGN KEY(gameId) REFERENCES games(id) ON DELETE RESTRICT, FOREIGN KEY(playerId) REFERENCES players(id) ON DELETE RESTRICT)")
            db.execSQL("CREATE INDEX index_games_gameDate ON games(gameDate)")
            db.execSQL("CREATE INDEX index_attendance_playerId ON attendance(playerId)")
            db.execSQL("CREATE INDEX index_assignments_playerId ON assignments(playerId)")
            db.execSQL("CREATE UNIQUE INDEX index_assignments_gameId_rotationIndex_position ON assignments(gameId, rotationIndex, position)")
            db.execSQL("INSERT INTO players (id, firstName, lastInitial, isActive, createdAt) VALUES (1, 'Nick', '', 1, 100)")
            db.execSQL("INSERT INTO games (id, roundNumber, gameDate, status, notes, createdAt) VALUES (1, 4, 20700, 'DRAFT', 'Bring water', 200)")
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2).allowMainThreadQueries().build()
        val player = room.playerDao().all().single()
        val game = room.gameDao().all().single()
        assertEquals("Nick", player.displayName)
        assertNull(player.photoPath)
        assertEquals(4, game.roundNumber)
        assertNull(game.photoPath)
        room.close()
        context.deleteDatabase(name)
        }
    }
}
