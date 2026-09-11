package com.sunilsstartingseven.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.backup.*
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.DevelopmentSeedRepository
import com.sunilsstartingseven.app.data.repository.PlayerRepository
import com.sunilsstartingseven.app.data.repository.RosterRepository
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.LineupSwaps
import com.sunilsstartingseven.app.domain.rotation.RotationAssignment
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
import java.time.OffsetDateTime

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class BackupRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var preferences: PreferenceRepository
    private val name = "backup-test.db"

    private fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()

    @Before fun setup() {
        context.deleteDatabase(name)
        db = open()
        preferences = PreferenceRepository(context)
    }

    @After fun cleanup() {
        db.close()
        context.deleteDatabase(name)
    }

    @Test fun completeBackupRoundTripPreservesPortableStateAndFutureIds() = runBlocking {
        createRepresentativeState()
        val repository = BackupRepository(db, preferences)
        val backup = repository.createBackup(OffsetDateTime.parse("2026-09-12T08:35:20+10:00"))
        val json = BackupCodec().encode(backup)

        assertTrue(json.contains("\"format\": \"sunils-starting-seven-backup\""))
        assertFalse(json.contains("development_seed_version"))
        assertFalse(json.contains("C:\\\\Users"))

        val before = portableSnapshot()
        repository.restore(repository.validateJson(json))
        assertEquals(before, portableSnapshot())
        assertEquals("Nick", db.playerDao().all().single { it.firstName == "Nick" }.displayName)
        assertFalse(db.playerDao().all().single { it.firstName == "Inactive" }.isActive)

        PlayerRepository(db.playerDao()).save(null, "Future", "F")
        val nextPlayer = db.playerDao().all().single { it.firstName == "Future" }
        assertTrue(nextPlayer.id > before.players.maxOf { it.id })
        val nextGame = db.gameDao().insert(Game(roundNumber = 99, gameDate = LocalDate.of(2026, 12, 1)))
        assertTrue(nextGame > before.games.maxOf { it.id })
    }

    @Test fun exportDoesNotMutateStoredData() = runBlocking {
        createRepresentativeState()
        val before = portableSnapshot()
        BackupRepository(db, preferences).createBackupJson()
        assertEquals(before, portableSnapshot())
    }

    @Test fun restoreRejectsMalformedWrongFormatFutureVersionBrokenReferencesAndDuplicateIds() = runBlocking {
        createRepresentativeState()
        val repository = BackupRepository(db, preferences)
        val original = portableSnapshot()
        val backup = repository.createBackup()

        assertRejectsAndPreserves(repository, "{ not json", original)
        assertRejectsAndPreserves(repository, BackupCodec().encode(backup.copy(format = "other-app")), original)
        assertRejectsAndPreserves(repository, BackupCodec().encode(backup.copy(backupVersion = 99)), original)
        assertRejectsAndPreserves(repository, BackupCodec().encode(backup.copy(attendance = backup.attendance + BackupAttendanceDto(backup.games.first().id, 99999, AttendanceStatus.AVAILABLE.name))), original)
        assertRejectsAndPreserves(repository, BackupCodec().encode(backup.copy(players = backup.players + backup.players.first().copy(firstName = "Duplicate"))), original)
    }

    @Test fun manuallyEditedPlayedRosterSurvivesRestoreAsHistoryTruth() = runBlocking {
        (1..8).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
        val game = db.gameDao().insert(Game(roundNumber = 8, gameDate = LocalDate.of(2026, 9, 12)))
        val roster = RosterRepository(db)
        roster.initializeAttendance(game)
        roster.generateLineup(game)
        val generated = db.rosterDao().getAssignments(game)
        val field = generated.first { it.rotationIndex == 0 && it.playingState == PlayingState.ON_FIELD }
        val bench = generated.first { it.rotationIndex == 0 && it.playingState == PlayingState.BENCH }
        roster.saveManualLineup(game, LineupSwaps.swap(generated.map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }, 0, field.playerId, bench.playerId))
        roster.markPlayed(game)
        val edited = db.rosterDao().getAssignments(game)

        val repository = BackupRepository(db, preferences)
        val validated = repository.validateJson(repository.createBackupJson())
        db.rosterDao().deleteAllAssignments()
        db.rosterDao().deleteAllAttendance()
        db.gameDao().deleteAll()
        db.playerDao().deleteAll()
        repository.restore(validated)

        assertEquals(edited.toSet(), db.rosterDao().getAssignments(game).toSet())
        assertEquals(field.position, db.rosterDao().getAssignments(game).single { it.gameId == game && it.rotationIndex == 0 && it.playerId == bench.playerId }.position)
    }

    @Test fun restoreMarksDevelopmentSeedAppliedSoRestartDoesNotWipeRestoredData() = runBlocking {
        createRepresentativeState()
        val repository = BackupRepository(db, preferences)
        val validated = repository.validateJson(repository.createBackupJson())
        preferences.setDevelopmentSeedVersion(0)
        repository.restore(validated)

        db.close()
        db = open()
        DevelopmentSeedRepository(db, preferences).applyDevelopmentSeedIfNeeded()

        assertTrue(db.playerDao().all().any { it.firstName == "Nick" && it.lastInitial.isBlank() })
        assertTrue(db.gameDao().all().isNotEmpty())
    }

    private suspend fun assertRejectsAndPreserves(repository: BackupRepository, json: String, original: PortableSnapshot) {
        var rejected = false
        try {
            repository.restore(repository.validateJson(json))
        } catch (_: Exception) {
            rejected = true
        }
        assertTrue("Expected backup to be rejected", rejected)
        assertEquals(original, portableSnapshot())
    }

    private suspend fun createRepresentativeState() {
        val players = (1..8).map { id -> db.playerDao().insert(Player(firstName = "Player$id", lastInitial = "A", isActive = true, createdAt = id.toLong())) }
        val nick = db.playerDao().insert(Player(firstName = "Nick", lastInitial = "", isActive = true, createdAt = 20))
        val inactive = db.playerDao().insert(Player(firstName = "Inactive", lastInitial = "I", isActive = false, createdAt = 21))

        db.gameDao().insert(Game(id = 1, roundNumber = 1, gameDate = LocalDate.of(2026, 9, 1), status = GameStatus.DRAFT, notes = "Draft", createdAt = 101))
        db.gameDao().insert(Game(id = 2, roundNumber = 2, gameDate = LocalDate.of(2026, 9, 8), status = GameStatus.READY, notes = "Ready", createdAt = 102))
        db.gameDao().insert(Game(id = 3, roundNumber = 3, gameDate = LocalDate.of(2026, 9, 15), status = GameStatus.CANCELLED, notes = "Cancelled", createdAt = 103))
        db.gameDao().insert(Game(id = 4, roundNumber = 4, gameDate = LocalDate.of(2026, 9, 22), status = GameStatus.PLAYED, notes = "Played", createdAt = 104))

        val activeForPlayed = (players + nick).take(8)
        db.rosterDao().insertAttendance(activeForPlayed.map { GameAttendance(4, it, AttendanceStatus.AVAILABLE) } + GameAttendance(4, inactive, AttendanceStatus.AWAY))
        val positions = PlayingPosition.entries.toList()
        val assignments = (0..3).flatMap { rotation ->
            val benchPlayer = activeForPlayed[rotation % activeForPlayed.size]
            activeForPlayed.filterNot { it == benchPlayer }.take(7).mapIndexed { index, playerId ->
                Assignment(4, rotation, playerId, PlayingState.ON_FIELD, positions[index])
            } + Assignment(4, rotation, benchPlayer, PlayingState.BENCH, null)
        }
        db.rosterDao().insertAssignments(assignments)
    }

    private suspend fun portableSnapshot() = PortableSnapshot(
        players = db.playerDao().all(),
        games = db.gameDao().all(),
        attendance = db.rosterDao().allAttendance(),
        assignments = db.rosterDao().allAssignments()
    )
}

data class PortableSnapshot(
    val players: List<Player>,
    val games: List<Game>,
    val attendance: List<GameAttendance>,
    val assignments: List<Assignment>
)
