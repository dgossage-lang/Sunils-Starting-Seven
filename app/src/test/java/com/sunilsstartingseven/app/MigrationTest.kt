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

    @Test fun migration2To3PreservesPlayedAssignmentsMediaAndAttendance() = runBlocking {
        val name = "migration-2-3-test.db"
        context.deleteDatabase(name)
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        val schema = listOf(java.io.File("schemas/com.sunilsstartingseven.app.data.local.AppDatabase/2.json"), java.io.File("app/schemas/com.sunilsstartingseven.app.data.local.AppDatabase/2.json")).first { it.exists() }
        val database = org.json.JSONObject(schema.readText()).getJSONObject("database")
        SQLiteDatabase.openOrCreateDatabase(path, null).use { db ->
            db.version = 2
            val entities = database.getJSONArray("entities")
            for (index in 0 until entities.length()) {
                val entity = entities.getJSONObject(index)
                db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: org.json.JSONArray()
                for (i in 0 until indices.length()) db.execSQL(indices.getJSONObject(i).getString("createSql").replace("${'$'}{TABLE_NAME}", entity.getString("tableName")))
            }
            db.execSQL("INSERT INTO players VALUES (1, 'Nick', 'N', 1, 100, 'media/players/nick.jpg')")
            db.execSQL("INSERT INTO games VALUES (1, 4, 20700, 'PLAYED', 'Bring water', 200, 'media/games/round4.jpg')")
            db.execSQL("INSERT INTO attendance VALUES (1, 1, 'AVAILABLE')")
            db.execSQL("INSERT INTO assignments VALUES (1, 0, 1, 'ON_FIELD', 'STRIKER')")
        }
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_2_3).allowMainThreadQueries().build()
        try {
            val game = room.gameDao().all().single()
            assertEquals(com.sunilsstartingseven.app.domain.model.GameStatus.PLAYED, game.status)
            assertEquals("media/games/round4.jpg", game.photoPath)
            assertEquals("media/players/nick.jpg", room.playerDao().all().single().photoPath)
            assertFalse(room.playerDao().all().single().carryFairnessAcrossSeasons)
            assertEquals(1, room.rosterDao().allAttendance().size)
            assertEquals(com.sunilsstartingseven.app.domain.model.PlayingPosition.STRIKER, room.rosterDao().allAssignments().single().position)
            assertEquals(1L, game.seasonId)
            assertEquals(7, game.fieldPositions.size)
            assertEquals(7, game.positionOptions.size)
        } finally { room.close(); context.deleteDatabase(name) }
    }

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
        val room = Room.databaseBuilder(context, AppDatabase::class.java, name).addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).allowMainThreadQueries().build()
        val player = room.playerDao().all().single()
        val game = room.gameDao().all().single()
        assertEquals("Nick", player.displayName)
        assertNull(player.photoPath)
        assertEquals(4, game.roundNumber)
        assertNull(game.photoPath)
        assertEquals(1L, game.seasonId)
        assertEquals(7, game.fieldPositions.size)
        assertEquals("Earlier games", room.seasonDao().all().single().name)
        room.close()
        context.deleteDatabase(name)
        }
    }
}
