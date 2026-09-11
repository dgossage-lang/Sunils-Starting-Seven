package com.sunilsstartingseven.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.repository.*
import com.sunilsstartingseven.app.domain.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class PersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private val name = "repository-test.db"
    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
    @Before fun setup() { context.deleteDatabase(name); db = open() }
    @After fun cleanup() { db.close(); context.deleteDatabase(name) }

    @Test fun playerAndDraftSurviveDatabaseReopen() = runBlocking {
        val players = PlayerRepository(db.playerDao())
        players.save(null, " Mason ", " g ")
        val player = players.players.first().single()
        GameRepository(db.gameDao()).createDraft(7, LocalDate.of(2026, 9, 12), " Bring water ")
        db.close()
        db = open()
        assertEquals("Mason G.", db.playerDao().get(player.id)!!.displayName)
        val game = db.gameDao().observeAll().first().single()
        assertEquals(7, game.roundNumber)
        assertEquals(LocalDate.of(2026, 9, 12), game.gameDate)
        assertEquals(GameStatus.DRAFT, game.status)
        assertEquals("Bring water", game.notes)
    }

    @Test fun editsAndActivationPreserveIdentityAndHistory() = runBlocking {
        val players = PlayerRepository(db.playerDao())
        players.save(null, "Mason", "g")
        val original = players.players.first().single()
        val gameId = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now(), status = GameStatus.PLAYED))
        db.rosterDao().insertAttendance(GameAttendance(gameId, original.id, AttendanceStatus.AVAILABLE))
        db.rosterDao().insertAssignment(Assignment(gameId, 0, original.id, PlayingState.ON_FIELD, PlayingPosition.GOALKEEPER))
        players.save(original.id, " Mason-Junior ", "h")
        players.setActive(original.id, false)
        val inactive = db.playerDao().get(original.id)!!
        assertFalse(inactive.isActive)
        assertEquals(original.createdAt, inactive.createdAt)
        assertEquals("Mason-Junior H.", inactive.displayName)
        assertEquals(1, db.rosterDao().observeAssignments(gameId).first().size)
        assertEquals(1, db.rosterDao().observeAttendance(gameId).first().size)
        players.setActive(original.id, true)
        assertTrue(db.playerDao().get(original.id)!!.isActive)
    }

    @Test fun uniquePositionAndPlayerConstraintsRejectDuplicates() = runBlocking {
        val a = db.playerDao().insert(Player(firstName = "A", lastInitial = "A"))
        val b = db.playerDao().insert(Player(firstName = "B", lastInitial = "B"))
        val game = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now()))
        db.rosterDao().insertAssignment(Assignment(game, 0, a, PlayingState.ON_FIELD, PlayingPosition.GOALKEEPER))
        assertFails { db.rosterDao().insertAssignment(Assignment(game, 0, b, PlayingState.ON_FIELD, PlayingPosition.GOALKEEPER)) }
        assertFails { db.rosterDao().insertAssignment(Assignment(game, 0, a, PlayingState.BENCH, null)) }
        db.rosterDao().insertAssignment(Assignment(game, 1, a, PlayingState.BENCH, null))
        db.rosterDao().insertAssignment(Assignment(game, 1, b, PlayingState.BENCH, null))
        assertEquals(3, db.rosterDao().observeAssignments(game).first().size)
    }

    @Test fun foreignKeysProtectHistory() = runBlocking {
        val player = db.playerDao().insert(Player(firstName = "Mason", lastInitial = "G"))
        val game = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now()))
        db.rosterDao().insertAttendance(GameAttendance(game, player, AttendanceStatus.AWAY))
        assertFails { db.openHelper.writableDatabase.execSQL("DELETE FROM players WHERE id = ?", arrayOf(player)) }
        assertFails { db.openHelper.writableDatabase.execSQL("DELETE FROM games WHERE id = ?", arrayOf(game)) }
        assertFails { db.rosterDao().insertAttendance(GameAttendance(game, 9999, AttendanceStatus.AVAILABLE)) }
    }

    @Test fun invalidRepositoryInputIsNotPersisted() = runBlocking {
        assertFails { PlayerRepository(db.playerDao()).save(null, " ", "G") }
        assertFails { GameRepository(db.gameDao()).createDraft(0, LocalDate.now(), "") }
        assertTrue(db.playerDao().observeAll().first().isEmpty())
        assertTrue(db.gameDao().observeAll().first().isEmpty())
    }

    private suspend fun assertFails(block: suspend () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue("Expected operation to reject invalid data", failed)
    }
}
