package com.sunilsstartingseven.app.data.repository

import android.content.ContentResolver
import android.net.Uri
import androidx.room.withTransaction
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.data.media.MediaRepository
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import java.time.LocalDate

class PlayerRepository(private val dao: PlayerDao, private val media: MediaRepository? = null) {
    val players = dao.observeAll()
    suspend fun save(id: Long?, firstName: String, lastInitial: String, carryFairnessAcrossSeasons: Boolean = false) {
        require(PlayerInput.validate(firstName, lastInitial).isValid) { "Invalid player name." }
        val first = firstName.trim()
        val initial = PlayerInput.normalizeInitial(lastInitial)
        if (id == null) dao.insert(Player(firstName = first, lastInitial = initial, carryFairnessAcrossSeasons = carryFairnessAcrossSeasons))
        else check(dao.updateName(id, first, initial, carryFairnessAcrossSeasons) == 1) { "Player no longer exists." }
    }
    suspend fun setActive(id: Long, active: Boolean) {
        check(dao.setActive(id, active) == 1) { "Player no longer exists." }
    }
    suspend fun setPhoto(id: Long, resolver: ContentResolver, uri: Uri) = requireNotNull(media) { "Media storage is not available." }.setPlayerPhoto(id, resolver, uri)
    suspend fun removePhoto(id: Long) = requireNotNull(media) { "Media storage is not available." }.removePlayerPhoto(id)
}

class SeasonRepository(private val dao: SeasonDao) {
    val seasons = dao.observeAll()
    suspend fun save(id: Long?, name: String): Long {
        require(name.trim().isNotEmpty()) { "Enter a season name." }
        return if (id == null) dao.insert(Season(name = name.trim())) else {
            check(dao.rename(id, name.trim()) == 1) { "Season no longer exists." }
            id
        }
    }
}

class GameRepository(private val dao: GameDao, private val media: MediaRepository? = null, private val db: AppDatabase? = null) {
    val games = dao.observeAll()
    suspend fun createDraft(roundNumber: Int, date: LocalDate, notes: String, seasonId: Long? = null) {
        require(roundNumber > 0) { "Round number must be positive." }
        if (db == null) {
            dao.insert(Game(roundNumber = roundNumber, gameDate = date, notes = notes.trim(), seasonId = seasonId))
        } else db.withTransaction {
            val season = seasonId ?: db.seasonDao().all().maxWithOrNull(compareBy({ it.createdAt }, { it.id }))?.id
                ?: db.seasonDao().insert(Season(name = "First season"))
            check(db.seasonDao().get(season) != null) { "Season no longer exists." }
            val previous = dao.latestInSeason(season) ?: dao.latest()
            val positions = previous?.fieldPositions ?: PlayingPosition.entries.toList()
            dao.insert(Game(roundNumber = roundNumber, gameDate = date, notes = notes.trim(), seasonId = season, fieldPositions = positions, positionOptions = previous?.positionOptions ?: PlayingPosition.entries.toList()))
        }
    }
    suspend fun updateDraft(id: Long, roundNumber: Int, date: LocalDate, notes: String) {
        require(roundNumber > 0) { "Round number must be positive." }
        check(dao.updateDraftDetails(id, roundNumber, date, notes.trim()) == 1) { "Draft game no longer exists or is not editable." }
    }
    suspend fun setPhoto(id: Long, resolver: ContentResolver, uri: Uri) = requireNotNull(media) { "Media storage is not available." }.setGamePhoto(id, resolver, uri)
    suspend fun removePhoto(id: Long) = requireNotNull(media) { "Media storage is not available." }.removeGamePhoto(id)
}

class DevelopmentSeedRepository(private val db: AppDatabase, private val preferences: PreferenceRepository) {
    suspend fun applyDevelopmentSeedIfNeeded() {
        if (preferences.developmentSeedVersion() >= DEVELOPMENT_SEED_VERSION) return
        db.withTransaction {
            db.rosterDao().deleteAllAssignments()
            db.rosterDao().deleteAllAttendance()
            db.gameDao().deleteAll()
            db.playerDao().deleteAll()
            db.playerDao().insert(DevelopmentSeedPlayers.map { Player(firstName = it, lastInitial = "", isActive = true) })
        }
        preferences.setDevelopmentSeedVersion(DEVELOPMENT_SEED_VERSION)
    }

    companion object {
        const val DEVELOPMENT_SEED_VERSION = 1
        val DevelopmentSeedPlayers = listOf("Nick", "Cameron", "Axel", "Hughie", "Jero", "Samrith", "Macson", "Artie", "Roman", "Nico")
    }
}

class RosterRepository(private val db: AppDatabase, private val engine: FairRotationEngine = FairRotationEngine(), private val validator: RotationValidator = RotationValidator(), private val structuralValidator: StructuralRosterValidator = StructuralRosterValidator()) {
    private val players = db.playerDao()
    private val games = db.gameDao()
    private val roster = db.rosterDao()
    val attendance = roster.observeAllAttendanceWithPlayers()
    val assignments = roster.observeAllAssignmentsWithPlayers()

    suspend fun updateLayout(gameId: Long, positions: List<PlayingPosition>, options: List<PlayingPosition>? = null) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.DRAFT || game.status == GameStatus.READY) { "Only draft or ready games can change layout." }
        validateLayout(positions)
        val mergedOptions = (PlayingPosition.entries + (options ?: game.positionOptions) + positions).associateBy { it.name }.values.toList()
        require(mergedOptions.size <= 40) { "Up to 40 position options are supported per game." }
        mergedOptions.forEach { PositionCodec.decode(PositionCodec.encode(it)) }
        val layoutChanged = PositionCodec.encodeLayout(game.fieldPositions) != PositionCodec.encodeLayout(positions)
        if (!layoutChanged && PositionCodec.encodeLayout(game.positionOptions) == PositionCodec.encodeLayout(mergedOptions)) return@withTransaction
        if (layoutChanged) roster.deleteAssignmentsForGame(gameId)
        games.updateLayout(gameId, positions, mergedOptions)
        if (layoutChanged) games.updateStatus(gameId, GameStatus.DRAFT)
    }

    suspend fun initializeAttendance(gameId: Long) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        if (game.status == GameStatus.PLAYED) return@withTransaction
        if (roster.attendanceCount(gameId) == 0) {
            roster.insertAttendance(players.activePlayers().map { GameAttendance(gameId, it.id, AttendanceStatus.AVAILABLE) })
        }
    }

    suspend fun setAttendance(gameId: Long, playerId: Long, status: AttendanceStatus) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status != GameStatus.PLAYED) { "Played games are historical and cannot be edited." }
        check(roster.updateAttendanceStatus(gameId, playerId, status) == 1) { "Attendance record no longer exists." }
        if (roster.assignmentCount(gameId) > 0) {
            roster.deleteAssignmentsForGame(gameId)
            games.updateStatus(gameId, GameStatus.DRAFT)
        }
    }

    suspend fun missingActivePlayers(gameId: Long): List<Player> = roster.activePlayersMissingFromAttendance(gameId)

    suspend fun addMissingActivePlayers(gameId: Long) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.DRAFT) { "Only draft games can add missing players." }
        roster.insertAttendance(roster.activePlayersMissingFromAttendance(gameId).map { GameAttendance(gameId, it.id, AttendanceStatus.AVAILABLE) })
        if (roster.assignmentCount(gameId) > 0) roster.deleteAssignmentsForGame(gameId)
    }

    suspend fun generateLineup(gameId: Long) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.DRAFT || game.status == GameStatus.READY) { "Only draft or ready games can generate a lineup." }
        val attendanceRows = roster.getAttendanceWithPlayers(gameId)
        val available = attendanceRows.filter { it.attendance.status == AttendanceStatus.AVAILABLE }
        val away = attendanceRows.filter { it.attendance.status == AttendanceStatus.AWAY }.map { it.player.id }.toSet()
        require(available.size >= game.fieldPositions.size) { "Not enough playing players for this layout." }
        val carryPlayerIds = players.all().filter { it.carryFairnessAcrossSeasons }.map { it.id }.toSet()
        val seasonGameIds = games.all().filter { it.seasonId == game.seasonId }.map { it.id }.toSet()
        val result = engine.generate(
            RotationRequest(
                availablePlayers = available.map { RotationPlayer(it.player.id, it.player.displayName) },
                awayPlayerIds = away,
                history = roster.playedAssignments().filter { it.gameId in seasonGameIds || it.playerId in carryPlayerIds }.map { HistoricalAssignment(it.playerId, it.playingState, it.position) },
                seed = game.roundNumber * 31 + game.id.toInt(),
                fieldPositions = game.fieldPositions
            )
        )
        val validation = validator.validate(result.assignments, available.map { it.player.id }.toSet(), away, game.fieldPositions)
        check(validation.valid) { validation.errors.joinToString("; ") }
        roster.deleteAssignmentsForGame(gameId)
        roster.insertAssignments(result.assignments.map { Assignment(gameId, it.rotationIndex, it.playerId, it.playingState, it.position) })
        games.updateStatus(gameId, GameStatus.READY)
    }

    suspend fun markPlayed(gameId: Long) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.READY) { "Only ready games can be marked played." }
        val attendanceRows = roster.getAttendance(gameId)
        val available = attendanceRows.filter { it.status == AttendanceStatus.AVAILABLE }.map { it.playerId }.toSet()
        val away = attendanceRows.filter { it.status == AttendanceStatus.AWAY }.map { it.playerId }.toSet()
        val current = roster.getAssignments(gameId).map { RotationAssignment(it.playerId, it.rotationIndex, it.playingState, it.position) }
        val validation = structuralValidator.validate(current, available, away, game.fieldPositions)
        check(validation.valid) { validation.errors.joinToString("; ") }
        check(games.updateStatus(gameId, GameStatus.PLAYED) == 1) { "Game no longer exists." }
    }

    suspend fun saveManualLineup(gameId: Long, pending: List<RotationAssignment>) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.READY) { "Only ready games can have a lineup edited." }
        val attendanceRows = roster.getAttendance(gameId)
        val available = attendanceRows.filter { it.status == AttendanceStatus.AVAILABLE }.map { it.playerId }.toSet()
        val away = attendanceRows.filter { it.status == AttendanceStatus.AWAY }.map { it.playerId }.toSet()
        val validation = structuralValidator.validate(pending, available, away, game.fieldPositions)
        check(validation.valid) { validation.errors.joinToString(" ") }
        roster.deleteAssignmentsForGame(gameId)
        roster.insertAssignments(pending.map { Assignment(gameId, it.rotationIndex, it.playerId, it.playingState, it.position) })
    }
}
