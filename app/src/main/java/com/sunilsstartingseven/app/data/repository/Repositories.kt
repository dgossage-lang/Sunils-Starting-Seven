package com.sunilsstartingseven.app.data.repository

import androidx.room.withTransaction
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.data.preferences.PreferenceRepository
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import java.time.LocalDate

class PlayerRepository(private val dao: PlayerDao) {
    val players = dao.observeAll()
    suspend fun save(id: Long?, firstName: String, lastInitial: String) {
        require(PlayerInput.validate(firstName, lastInitial).isValid) { "Invalid player name." }
        val first = firstName.trim()
        val initial = PlayerInput.normalizeInitial(lastInitial)
        if (id == null) dao.insert(Player(firstName = first, lastInitial = initial))
        else check(dao.updateName(id, first, initial) == 1) { "Player no longer exists." }
    }
    suspend fun setActive(id: Long, active: Boolean) {
        check(dao.setActive(id, active) == 1) { "Player no longer exists." }
    }
}

class GameRepository(private val dao: GameDao) {
    val games = dao.observeAll()
    suspend fun createDraft(roundNumber: Int, date: LocalDate, notes: String) {
        require(roundNumber > 0) { "Round number must be positive." }
        dao.insert(Game(roundNumber = roundNumber, gameDate = date, notes = notes.trim()))
    }
    suspend fun updateDraft(id: Long, roundNumber: Int, date: LocalDate, notes: String) {
        require(roundNumber > 0) { "Round number must be positive." }
        check(dao.updateDraftDetails(id, roundNumber, date, notes.trim()) == 1) { "Draft game no longer exists or is not editable." }
    }
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
        require(available.size >= 7) { "At least seven playing players are required." }
        val result = engine.generate(
            RotationRequest(
                availablePlayers = available.map { RotationPlayer(it.player.id, it.player.displayName) },
                awayPlayerIds = away,
                history = roster.playedAssignments().map { HistoricalAssignment(it.playerId, it.playingState, it.position) },
                seed = game.roundNumber * 31 + game.id.toInt()
            )
        )
        val validation = validator.validate(result.assignments, available.map { it.player.id }.toSet(), away)
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
        val validation = validator.validate(current, available, away)
        check(validation.valid) { validation.errors.joinToString("; ") }
        check(games.updateStatus(gameId, GameStatus.PLAYED) == 1) { "Game no longer exists." }
    }

    suspend fun saveManualLineup(gameId: Long, pending: List<RotationAssignment>) = db.withTransaction {
        val game = games.get(gameId) ?: error("Game no longer exists.")
        check(game.status == GameStatus.READY) { "Only ready games can have a lineup edited." }
        val attendanceRows = roster.getAttendance(gameId)
        val available = attendanceRows.filter { it.status == AttendanceStatus.AVAILABLE }.map { it.playerId }.toSet()
        val away = attendanceRows.filter { it.status == AttendanceStatus.AWAY }.map { it.playerId }.toSet()
        val validation = structuralValidator.validate(pending, available, away)
        check(validation.valid) { validation.errors.joinToString(" ") }
        roster.deleteAssignmentsForGame(gameId)
        roster.insertAssignments(pending.map { Assignment(gameId, it.rotationIndex, it.playerId, it.playingState, it.position) })
    }
}
