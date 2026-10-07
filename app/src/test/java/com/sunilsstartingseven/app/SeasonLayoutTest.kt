package com.sunilsstartingseven.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.backup.*
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.media.MediaRepository
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.repository.*
import com.sunilsstartingseven.app.domain.history.*
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class SeasonLayoutTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val six = FieldPositions.filter { it != PlayingPosition.STRIKER }

    @Test fun configuredLayoutsGenerateCorrectFieldAndBenchCounts() {
        val engine = FairRotationEngine()
        val layouts = listOf(six, FieldPositions.filter { it != PlayingPosition.GOALKEEPER }, listOf(PlayingPosition.MID), FieldPositions)
        layouts.forEach { layout ->
            for (count in layout.size..12) {
                val result = engine.generate(RotationRequest((1..count).map { RotationPlayer(it.toLong()) }, fieldPositions = layout))
                assertTrue(StructuralRosterValidator().validate(result.assignments, (1..count).map { it.toLong() }.toSet(), emptySet(), layout).valid)
                (0..3).forEach { rotation ->
                    val rows = result.assignments.filter { it.rotationIndex == rotation }
                    assertEquals(layout.size, rows.count { it.playingState == PlayingState.ON_FIELD })
                    assertEquals(count - layout.size, rows.count { it.playingState == PlayingState.BENCH })
                }
            }
        }
        val summary = attendanceSummary(List(9) { AttendanceStatus.AVAILABLE }, six.size)
        assertEquals(6, summary.onFieldCapacity)
        assertEquals(3, summary.expectedBench)
        assertTrue(summary.readyForLineup)
        assertEquals(1, attendanceSummary(List(5) { AttendanceStatus.AVAILABLE }, 6).neededForLayout)
    }

    @Test fun layoutsAreInheritedWithinSeasonAndChangesInvalidateOnlyThatGame() = runBlocking {
        val name = "season-layout-test.db"
        context.deleteDatabase(name)
        var db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        try {
            val seasons = SeasonRepository(db.seasonDao())
            val firstSeason = seasons.save(null, "2026")
            val secondSeason = seasons.save(null, "2027")
            val games = GameRepository(db.gameDao(), db = db)
            games.createDraft(1, LocalDate.of(2026, 1, 1), "", firstSeason)
            val first = db.gameDao().all().single()
            (1..9).forEach { db.playerDao().insert(Player(firstName = "Player$it", lastInitial = "A")) }
            val roster = RosterRepository(db)
            roster.initializeAttendance(first.id)
            roster.generateLineup(first.id)
            val attendance = db.rosterDao().getAttendance(first.id)
            roster.updateLayout(first.id, six)
            assertEquals(GameStatus.DRAFT, db.gameDao().get(first.id)!!.status)
            assertEquals(attendance, db.rosterDao().getAttendance(first.id))
            assertTrue(db.rosterDao().getAssignments(first.id).isEmpty())
            roster.generateLineup(first.id)
            assertEquals(12, db.rosterDao().getAssignments(first.id).count { it.playingState == PlayingState.BENCH })
            // A manual swap remains valid for a six-position game.
            val rows = db.rosterDao().getAssignments(first.id).map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }
            val field = rows.first { it.rotationIndex == 0 && it.playingState == PlayingState.ON_FIELD }
            val bench = rows.first { it.rotationIndex == 0 && it.playingState == PlayingState.BENCH }
            roster.saveManualLineup(first.id, LineupSwaps.swap(rows, 0, field.playerId, bench.playerId))
            roster.markPlayed(first.id)
            val saved = db.rosterDao().getAssignments(first.id)
            try { roster.updateLayout(first.id, FieldPositions); fail("Played layout must be locked") } catch (_: IllegalStateException) { }
            games.createDraft(2, LocalDate.of(2026, 1, 8), "", firstSeason)
            games.createDraft(1, LocalDate.of(2027, 1, 1), "", secondSeason)
            val all = db.gameDao().all()
            assertEquals(six.toSet(), all[1].fieldPositions.toSet())
            assertTrue(db.rosterDao().getAssignments(all[1].id).isEmpty())
            assertEquals(six.toSet(), all[2].fieldPositions.toSet())
            assertEquals(saved, db.rosterDao().getAssignments(first.id))
            db.close()
            db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
            assertEquals(all, db.gameDao().all())
            assertEquals(2, db.seasonDao().all().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun seasonHistoryDoesNotInfluenceAnotherSeasonsGeneration() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val old = db.seasonDao().insert(Season(name = "Old"))
            val current = db.seasonDao().insert(Season(name = "Current"))
            (1..9).forEach { db.playerDao().insert(Player(firstName = "P$it", lastInitial = "A")) }
            val oldGame = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now(), seasonId = old))
            val game = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now(), seasonId = current, fieldPositions = six))
            val roster = RosterRepository(db)
            roster.initializeAttendance(game)
            roster.generateLineup(game)
            val before = db.rosterDao().getAssignments(game)
            roster.initializeAttendance(oldGame)
            roster.generateLineup(oldGame)
            roster.markPlayed(oldGame)
            roster.generateLineup(game)
            assertEquals(before, db.rosterDao().getAssignments(game))
            PlayerRepository(db.playerDao()).save(1, "P1", "A", carryFairnessAcrossSeasons = true)
            assertTrue(db.playerDao().get(1)!!.carryFairnessAcrossSeasons)
            roster.generateLineup(game)
            val expected = FairRotationEngine().generate(RotationRequest(
                db.playerDao().all().map { RotationPlayer(it.id, it.displayName) },
                history = db.rosterDao().playedAssignments().filter { it.playerId == 1L }.map { HistoricalAssignment(it.playerId, it.playingState, it.position) },
                seed = 31 + game.toInt(), fieldPositions = six
            ))
            assertEquals(expected.assignments.toSet(), db.rosterDao().getAssignments(game).map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }.toSet())
            PlayerRepository(db.playerDao()).save(1, "P1", "A", carryFairnessAcrossSeasons = false)
            roster.generateLineup(game)
            assertEquals(before, db.rosterDao().getAssignments(game))
        } finally { db.close() }
    }

    @Test fun v3BackupPreservesSeasonsLayoutsAndMediaAndRejectsBrokenLayouts() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        val media = MediaRepository(context, db)
        try {
            val season = db.seasonDao().insert(Season(name = "Summer"))
            val player = db.playerDao().insert(Player(firstName = "Mason", lastInitial = "M"))
            val game = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now(), seasonId = season, fieldPositions = six))
            db.playerDao().updatePhotoPath(player, "media/players/season_test.jpg")
            val bytes = ByteArrayOutputStream().also { android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888).compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
            media.writeRestoredMedia("media/players/season_test.jpg", bytes)
            val repo = BackupRepository(db, PreferenceRepository(context), media)
            val original = repo.createBackup()
            val archive = ByteArrayOutputStream().also { repo.writeBackupArchive(it) }.toByteArray()
            val validated = repo.validateBytes(archive)
            assertEquals(3, validated.preview.backupVersion)
            assertEquals(1, validated.preview.seasonCount)
            repo.restore(validated)
            assertEquals(original.seasons, repo.createBackup().seasons)
            assertEquals(original.games, repo.createBackup().games)
            assertTrue(media.readOwnedMedia("media/players/season_test.jpg").contentEquals(bytes))
            listOf(original.copy(games = original.games.map { it.copy(fieldPositions = emptyList()) }),
                original.copy(games = original.games.map { it.copy(seasonId = 999) }),
                original.copy(games = original.games.map { it.copy(fieldPositions = listOf("MID", "MID")) })).forEach { invalid ->
                try { repo.validate(invalid); fail("Invalid layout/season must be rejected") } catch (_: BackupException) { }
            }
            assertEquals(six.toSet(), db.gameDao().get(game)!!.fieldPositions.toSet())
        } finally { media.fileFor("media/players/season_test.jpg")?.delete(); db.close() }
    }

    @Test fun legacyV2BackupCreatesSeasonAndSevenPositionLayout() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now()))
            val repo = BackupRepository(db, PreferenceRepository(context), MediaRepository(context, db))
            val v2 = repo.createBackup().copy(backupVersion = 2, databaseSchemaVersion = 2)
            val json = BackupCodec().encode(v2)
            assertFalse(json.contains("fieldPositions"))
            repo.restore(repo.validateJson(json))
            assertEquals("Earlier games", db.seasonDao().all().single().name)
            assertEquals(1L, db.gameDao().all().single().seasonId)
            assertEquals(FieldPositions.toSet(), db.gameDao().all().single().fieldPositions.toSet())
        } finally { db.close() }
    }

    @Test fun historyOnlyCountsPositionsOfferedAndAdjustsForLayoutCapacity() {
        val games = (1..3).map { HistoryGame(it.toLong(), it, LocalDate.now().plusDays(it.toLong()), GameStatus.PLAYED, six, "Summer") }
        val players = (1..9).map { HistoryPlayer(it.toLong(), "P$it", true) }
        val attendance = games.flatMap { g -> players.map { HistoryAttendance(g.id, it.id, AttendanceStatus.AVAILABLE) } }
        val assignments = games.flatMap { g -> FairRotationEngine().generate(RotationRequest(players.map { RotationPlayer(it.id) }, fieldPositions = six, seed = g.id.toInt())).assignments.map { HistoryAssignment(g.id, it.rotationIndex, it.playerId, it.playingState, it.position) } }
        val summary = FairnessHistoryCalculator().calculate(players, games, attendance, assignments)
        assertEquals(72, summary.activePlayers.sumOf { it.activeRotations })
        assertEquals(36, summary.activePlayers.sumOf { it.benchRotations })
        summary.activePlayers.forEach { p ->
            assertEquals(8.0, p.expectedActiveRotations, .001)
            assertEquals(6, p.eligiblePositions.size)
            assertFalse(PlayingPosition.STRIKER in p.nextOpportunities)
            assertFalse(PlayingPosition.STRIKER in p.exactPositions)
            assertEquals(6, p.games.first().fieldCount)
            assertEquals("Summer", p.games.first().seasonName)
        }
    }

    @Test fun customPositionsPersistInLayoutsAssignmentsBackupsAndLaterGames() = runBlocking {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val season = db.seasonDao().insert(Season(name = "Custom"))
            (1..9).forEach { db.playerDao().insert(Player(firstName = "P$it", lastInitial = "A", carryFairnessAcrossSeasons = it == 1)) }
            val custom = PlayingPosition("CUSTOM_test", PositionFamily.ATT, "CF", "Centre forward", .5f, .25f)
            val game = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.now(), seasonId = season))
            val roster = RosterRepository(db)
            val layout = six + custom
            roster.updateLayout(game, layout)
            roster.initializeAttendance(game)
            roster.generateLineup(game)
            assertEquals(4, db.rosterDao().getAssignments(game).count { it.position == custom })
            roster.markPlayed(game)
            val repo = BackupRepository(db, PreferenceRepository(context), MediaRepository(context, db))
            repo.restore(repo.validateJson(repo.createBackupJson()))
            assertTrue(db.playerDao().get(1)!!.carryFairnessAcrossSeasons)
            assertEquals(layout, db.gameDao().get(game)!!.fieldPositions)
            assertEquals(4, db.rosterDao().getAssignments(game).count { it.position == custom })
            val games = GameRepository(db.gameDao(), db = db)
            games.createDraft(2, LocalDate.now().plusDays(7), "", season)
            val next = db.gameDao().all().last()
            assertEquals(layout, next.fieldPositions)
            assertTrue(custom in next.positionOptions)
            val moved = custom.copy(x = .7f, fullLabel = "Right forward")
            roster.updateLayout(next.id, six + moved)
            assertEquals(.7f, db.gameDao().get(next.id)!!.fieldPositions.last().x, .001f)
            assertEquals(.5f, db.gameDao().get(game)!!.fieldPositions.last().x, .001f)
            roster.updateLayout(next.id, six)
            assertTrue(db.gameDao().get(next.id)!!.positionOptions.any { it.name == custom.name })
            games.createDraft(3, LocalDate.now().plusDays(14), "", season)
            assertEquals(six, db.gameDao().all().last().fieldPositions)
            assertTrue(db.gameDao().all().last().positionOptions.any { it.name == custom.name })
            val third = db.gameDao().all().last()
            roster.initializeAttendance(third.id)
            roster.generateLineup(third.id)
            val savedAssignments = db.rosterDao().getAssignments(third.id)
            // Delete an inactive option without clearing the ready lineup. Standard options cannot be deleted.
            roster.updateLayout(third.id, six, third.positionOptions.filter { it.name != custom.name && it != PlayingPosition.STRIKER })
            assertEquals(GameStatus.READY, db.gameDao().get(third.id)!!.status)
            assertEquals(savedAssignments, db.rosterDao().getAssignments(third.id))
            assertFalse(db.gameDao().get(third.id)!!.positionOptions.any { it.name == custom.name })
            assertTrue(PlayingPosition.STRIKER in db.gameDao().get(third.id)!!.positionOptions)
            games.createDraft(4, LocalDate.now().plusDays(21), "", season)
            val fourth = db.gameDao().all().last()
            assertFalse(fourth.positionOptions.any { it.name == custom.name })
            // Active deletion clears only this game's assignments and retains attendance.
            roster.updateLayout(fourth.id, six + custom)
            roster.initializeAttendance(fourth.id)
            roster.generateLineup(fourth.id)
            val beforeAttendance = db.rosterDao().getAttendance(fourth.id)
            roster.updateLayout(fourth.id, six, PlayingPosition.entries.toList())
            assertEquals(GameStatus.DRAFT, db.gameDao().get(fourth.id)!!.status)
            assertTrue(db.rosterDao().getAssignments(fourth.id).isEmpty())
            assertEquals(beforeAttendance, db.rosterDao().getAttendance(fourth.id))
            assertFalse(db.gameDao().get(fourth.id)!!.positionOptions.any { it.name == custom.name })
            assertEquals(4, db.rosterDao().getAssignments(game).count { it.position == custom })
            repo.restore(repo.validateJson(repo.createBackupJson()))
            assertFalse(db.gameDao().get(fourth.id)!!.positionOptions.any { it.name == custom.name })
            assertEquals(4, db.rosterDao().getAssignments(game).count { it.position == custom })
        } finally { db.close() }
    }

    @Test fun customLayoutsAboveSevenUseAllSlotsWithoutFactorialSearch() {
        for (count in 8..12) {
            val custom = (1..count - 7).map { PlayingPosition("CUSTOM_$it", PositionFamily.MID, "M$it", "Mid $it", .2f, .5f) }
            val layout = FieldPositions + custom
            val players = (1..12).map { RotationPlayer(it.toLong()) }
            val result = FairRotationEngine().generate(RotationRequest(players, fieldPositions = layout))
            assertTrue(StructuralRosterValidator().validate(result.assignments, players.map { it.id }.toSet(), emptySet(), layout).valid)
            assertEquals(4 * count, result.assignments.count { it.playingState == PlayingState.ON_FIELD })
            assertEquals(4 * (12 - count), result.assignments.count { it.playingState == PlayingState.BENCH })
        }
    }
}
