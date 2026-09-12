package com.sunilsstartingseven.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.media.MediaRepository
import com.sunilsstartingseven.app.data.repository.GameRepository
import com.sunilsstartingseven.app.data.repository.PlayerRepository
import com.sunilsstartingseven.app.data.repository.RosterRepository
import com.sunilsstartingseven.app.domain.history.FairnessHistoryCalculator
import com.sunilsstartingseven.app.domain.history.HistoryAssignment
import com.sunilsstartingseven.app.domain.history.HistoryAttendance
import com.sunilsstartingseven.app.domain.history.HistoryGame
import com.sunilsstartingseven.app.domain.history.HistoryPlayer
import com.sunilsstartingseven.app.domain.model.AttendanceStatus
import com.sunilsstartingseven.app.domain.model.GameStatus
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class MediaRepositoryTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var media: MediaRepository
    private val name = "media-test.db"

    @Before fun setup() {
        context.deleteDatabase(name)
        db = Room.databaseBuilder(context, AppDatabase::class.java, name).allowMainThreadQueries().build()
        media = MediaRepository(context, db)
        media.clearAllMedia()
    }

    @After fun cleanup() {
        db.close()
        context.deleteDatabase(name)
        media.clearAllMedia()
    }

    @Test fun playerPhotoCanBeAddedReplacedAndRemoved() = runBlocking {
        val playerId = db.playerDao().insert(Player(firstName = "Nick", lastInitial = ""))
        val players = PlayerRepository(db.playerDao(), media)
        players.setPhoto(playerId, context.contentResolver, imageUri("first.jpg"))
        val firstPath = db.playerDao().get(playerId)!!.photoPath!!
        assertEquals("media/players/player_${playerId}.jpg", firstPath)
        assertNotNull(media.fileFor(firstPath))

        players.setPhoto(playerId, context.contentResolver, imageUri("second.jpg"))
        val secondPath = db.playerDao().get(playerId)!!.photoPath!!
        assertEquals(firstPath, secondPath)
        assertNotNull(media.fileFor(secondPath))

        players.removePhoto(playerId)
        assertNull(db.playerDao().get(playerId)!!.photoPath)
        assertNull(media.fileFor(secondPath))
    }

    @Test fun playedGamePhotoCanChangeWithoutChangingSoccerHistory() = runBlocking {
        (1..8).forEach { db.playerDao().insert(Player(firstName = "P$it", lastInitial = "A")) }
        val gameId = db.gameDao().insert(Game(roundNumber = 1, gameDate = LocalDate.of(2026, 9, 12)))
        val roster = RosterRepository(db)
        roster.initializeAttendance(gameId)
        roster.generateLineup(gameId)
        roster.markPlayed(gameId)
        val beforeHistory = historyActiveRotations()
        val beforeAssignments = db.rosterDao().allAssignments()
        val games = GameRepository(db.gameDao(), media)

        games.setPhoto(gameId, context.contentResolver, imageUri("game.jpg"))
        val path = db.gameDao().get(gameId)!!.photoPath
        assertNotNull(path)
        games.removePhoto(gameId)

        assertEquals(GameStatus.PLAYED, db.gameDao().get(gameId)!!.status)
        assertEquals(beforeAssignments, db.rosterDao().allAssignments())
        assertEquals(beforeHistory, historyActiveRotations())
    }

    private suspend fun historyActiveRotations(): Map<Long, Int> {
        val players = db.playerDao().all().map { HistoryPlayer(it.id, it.displayName, it.isActive, it.photoPath) }
        val games = db.gameDao().all().map { HistoryGame(it.id, it.roundNumber, it.gameDate, it.status) }
        val attendance = db.rosterDao().allAttendance().map { HistoryAttendance(it.gameId, it.playerId, it.status) }
        val assignments = db.rosterDao().allAssignments().map { HistoryAssignment(it.gameId, it.rotationIndex, it.playerId, it.playingState, it.position) }
        return FairnessHistoryCalculator().calculate(players, games, attendance, assignments).activePlayers.associate { it.player.id to it.activeRotations }
    }

    private fun imageUri(name: String): Uri {
        val file = File(context.cacheDir, name)
        file.writeBytes(tinyJpeg())
        return Uri.fromFile(file)
    }

    private fun tinyJpeg(): ByteArray {
        val bitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray()
    }
}
