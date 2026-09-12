package com.sunilsstartingseven.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.*
import com.sunilsstartingseven.app.domain.history.FairnessHistoryCalculator
import com.sunilsstartingseven.app.domain.history.HistoryAssignment
import com.sunilsstartingseven.app.domain.history.HistoryAttendance
import com.sunilsstartingseven.app.domain.history.HistoryGame
import com.sunilsstartingseven.app.domain.history.HistoryPlayer
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.LineupSwaps
import com.sunilsstartingseven.app.domain.rotation.RotationAssignment
import com.sunilsstartingseven.app.domain.rotation.RotationValidator
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

    @Test fun firstNameOnlySeedDisplayHasNoDanglingPeriod() {
        assertEquals("Nick", Player(firstName = "Nick", lastInitial = "").displayName)
        assertEquals("Mason G.", Player(firstName = "Mason", lastInitial = "G").displayName)
    }

    @Test fun developmentSeedClearsOnceAndSeedsOriginalRoster() = runBlocking {
        val preferences = PreferenceRepository(context)
        preferences.setDevelopmentSeedVersion(0)
        db.playerDao().insert(Player(firstName = "Test", lastInitial = "T"))
        val game = db.gameDao().insert(Game(roundNumber = 99, gameDate = LocalDate.now()))
        db.rosterDao().insertAttendance(GameAttendance(game, 1, AttendanceStatus.AWAY))

        DevelopmentSeedRepository(db, preferences).applyDevelopmentSeedIfNeeded()

        val seeded = db.playerDao().observeAll().first()
        assertEquals(DevelopmentSeedRepository.DevelopmentSeedPlayers.toSet(), seeded.map { it.firstName }.toSet())
        assertTrue(seeded.all { it.isActive && it.lastInitial.isBlank() })
        assertTrue(db.gameDao().observeAll().first().isEmpty())
        assertTrue(db.rosterDao().observeAttendance(game).first().isEmpty())

        PlayerRepository(db.playerDao()).save(null, "Mason", "G")
        DevelopmentSeedRepository(db, preferences).applyDevelopmentSeedIfNeeded()
        val afterSecondRun = db.playerDao().observeAll().first()
        assertTrue(afterSecondRun.any { it.displayName == "Mason G." })
        assertEquals(11, afterSecondRun.size)
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

    @Test fun attendanceInitializesActivePlayersOnlyAndDefaultsAvailable() = runBlocking {
        val active = (1..8).map { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val inactive = db.playerDao().insert(Player(firstName = "Inactive", lastInitial = "I", isActive = false))
        val game = db.gameDao().insert(Game(roundNumber = 2, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)

        roster.initializeAttendance(game)
        val attendance = db.rosterDao().observeAttendance(game).first()

        assertEquals(8, attendance.size)
        assertFalse(attendance.any { it.playerId == inactive })
        assertEquals(active.toSet(), attendance.map { it.playerId }.toSet())
        assertTrue(attendance.all { it.status == AttendanceStatus.AVAILABLE })
    }

    @Test fun attendanceInitializationIsIdempotentAndDoesNotResetAway() = runBlocking {
        val mason = db.playerDao().insert(Player(firstName = "Mason", lastInitial = "G"))
        val lucas = db.playerDao().insert(Player(firstName = "Lucas", lastInitial = "T"))
        val game = db.gameDao().insert(Game(roundNumber = 3, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)

        roster.initializeAttendance(game)
        roster.setAttendance(game, lucas, AttendanceStatus.AWAY)
        roster.initializeAttendance(game)
        val attendance = db.rosterDao().observeAttendance(game).first()

        assertEquals(2, attendance.size)
        assertEquals(AttendanceStatus.AVAILABLE, attendance.single { it.playerId == mason }.status)
        assertEquals(AttendanceStatus.AWAY, attendance.single { it.playerId == lucas }.status)
    }

    @Test fun attendanceIsIndependentPerGame() = runBlocking {
        val player = db.playerDao().insert(Player(firstName = "Mason", lastInitial = "G"))
        val round4 = db.gameDao().insert(Game(roundNumber = 4, gameDate = LocalDate.now()))
        val round5 = db.gameDao().insert(Game(roundNumber = 5, gameDate = LocalDate.now().plusDays(7)))
        val roster = RosterRepository(db)

        roster.initializeAttendance(round4)
        roster.initializeAttendance(round5)
        roster.setAttendance(round4, player, AttendanceStatus.AWAY)

        assertEquals(AttendanceStatus.AWAY, db.rosterDao().observeAttendance(round4).first().single().status)
        assertEquals(AttendanceStatus.AVAILABLE, db.rosterDao().observeAttendance(round5).first().single().status)
    }

    @Test fun initializedSnapshotSurvivesTeamChangesAndSupportsDeliberateMissingAdd() = runBlocking {
        val firstSeven = (1..7).map { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 6, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)

        roster.initializeAttendance(game)
        db.playerDao().setActive(firstSeven.last(), false)
        val latePlayer = db.playerDao().insert(Player(firstName = "Late", lastInitial = "L"))

        assertEquals(firstSeven.toSet(), db.rosterDao().observeAttendance(game).first().map { it.playerId }.toSet())
        assertEquals(listOf(latePlayer), roster.missingActivePlayers(game).map { it.id })

        roster.addMissingActivePlayers(game)
        roster.addMissingActivePlayers(game)

        val updated = db.rosterDao().observeAttendance(game).first()
        assertEquals(8, updated.size)
        assertEquals(1, updated.count { it.playerId == latePlayer })

        val futureGame = db.gameDao().insert(Game(roundNumber = 7, gameDate = LocalDate.now().plusDays(7)))
        roster.initializeAttendance(futureGame)
        val futurePlayers = db.rosterDao().observeAttendance(futureGame).first().map { it.playerId }.toSet()
        assertTrue(latePlayer in futurePlayers)
        assertFalse(firstSeven.last() in futurePlayers)
    }

    @Test fun attendanceSurvivesDatabaseReopen() = runBlocking {
        val player = db.playerDao().insert(Player(firstName = "Lucas", lastInitial = "T"))
        val game = db.gameDao().insert(Game(roundNumber = 8, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.setAttendance(game, player, AttendanceStatus.AWAY)
        db.close()
        db = open()

        assertEquals(AttendanceStatus.AWAY, db.rosterDao().observeAttendance(game).first().single().status)
    }

    @Test fun manualLineupSaveIsAtomicAndPlayedRosterBecomesHistoricalTruth() = runBlocking {
        (1..8).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 9, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)
        val before = db.rosterDao().getAssignments(game)
        val first = before.first { it.rotationIndex == 0 && it.playingState == PlayingState.ON_FIELD }
        val bench = before.first { it.rotationIndex == 0 && it.playingState == PlayingState.BENCH }
        val pending = LineupSwaps.swap(before.map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }, 0, first.playerId, bench.playerId)

        assertEquals(before, db.rosterDao().getAssignments(game)) // Cancel/no-save leaves Room untouched.
        roster.saveManualLineup(game, pending)

        val saved = db.rosterDao().getAssignments(game)
        assertEquals(32, saved.size)
        assertEquals(PlayingState.BENCH, saved.single { it.rotationIndex == 0 && it.playerId == first.playerId }.playingState)
        assertEquals(first.position, saved.single { it.rotationIndex == 0 && it.playerId == bench.playerId }.position)
        roster.markPlayed(game)
        assertTrue(db.rosterDao().playedAssignments().any { it.gameId == game && it.rotationIndex == 0 && it.playerId == bench.playerId && it.position == first.position })
        val players = db.playerDao().observeAll().first().map { HistoryPlayer(it.id, it.displayName, it.isActive) }
        val games = db.gameDao().observeAll().first().map { HistoryGame(it.id, it.roundNumber, it.gameDate, it.status) }
        val attendance = db.rosterDao().getAttendance(game).map { HistoryAttendance(it.gameId, it.playerId, it.status) }
        val assignments = db.rosterDao().getAssignments(game).map { HistoryAssignment(it.gameId, it.rotationIndex, it.playerId, it.playingState, it.position) }
        val history = FairnessHistoryCalculator().calculate(players, games, attendance, assignments)
        val benchPlayerHistory = history.player(bench.playerId)!!
        assertTrue(benchPlayerHistory.exactPositions.getValue(first.position!!) >= 1)
        assertEquals(first.position, benchPlayerHistory.games.single().rotations.single { it.rotationIndex == 0 }.position)
        assertFails { roster.saveManualLineup(game, pending) }
    }


    @Test fun regenerateCancelLeavesExistingRosterUnchanged() = runBlocking {
        (1..10).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 10, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)
        val before = db.rosterDao().getAssignments(game)

        // Cancel closes the dialog before calling the repository, so the saved roster is untouched.
        val afterCancel = db.rosterDao().getAssignments(game)

        assertEquals(before, afterCancel)
    }

    @Test fun regenerateConfirmSavesCompleteValidReplacement() = runBlocking {
        (1..10).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 11, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)

        roster.generateLineup(game)

        val replacement = db.rosterDao().getAssignments(game)
        val available = db.playerDao().all().map { it.id }.toSet()
        val validation = RotationValidator().validate(replacement.map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }, available, emptySet<Long>())
        assertTrue(validation.errors.toString(), validation.valid)
        assertEquals(40, replacement.size)
        assertEquals(GameStatus.READY, db.gameDao().get(game)!!.status)
    }

    @Test fun regenerateCancelPreservesManualEditsAndConfirmReplacesThem() = runBlocking {
        (1..10).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 12, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)
        val generated = db.rosterDao().getAssignments(game)
        val first = generated.first { it.rotationIndex == 0 && it.playingState == PlayingState.ON_FIELD }
        val bench = generated.first { it.rotationIndex == 0 && it.playingState == PlayingState.BENCH }
        val manual = LineupSwaps.swap(generated.map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }, 0, first.playerId, bench.playerId)
        roster.saveManualLineup(game, manual)
        val savedManual = db.rosterDao().getAssignments(game)

        // Cancel confirmation preserves the manually edited saved roster.
        assertEquals(savedManual, db.rosterDao().getAssignments(game))

        roster.generateLineup(game)
        val regenerated = db.rosterDao().getAssignments(game)
        val available = db.playerDao().all().map { it.id }.toSet()
        val validation = RotationValidator().validate(regenerated.map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }, available, emptySet<Long>())

        assertNotEquals(savedManual, regenerated)
        assertEquals(generated, regenerated)
        assertTrue(validation.errors.toString(), validation.valid)
    }

    @Test fun regenerateFailureKeepsPreviousRoster() = runBlocking {
        val playerIds = (1..7).map { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 13, gameDate = LocalDate.now()))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)
        val before = db.rosterDao().getAssignments(game)

        // Simulate attendance becoming invalid without using the repository path that intentionally clears lineups.
        db.rosterDao().updateAttendanceStatus(game, playerIds.first(), AttendanceStatus.AWAY)
        assertFails { roster.generateLineup(game) }

        assertEquals(before, db.rosterDao().getAssignments(game))
        assertEquals(GameStatus.READY, db.gameDao().get(game)!!.status)
    }

    private suspend fun assertFails(block: suspend () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue("Expected operation to reject invalid data", failed)
    }
}
