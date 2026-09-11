package com.sunilsstartingseven.app.data.repository

import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.model.*
import androidx.room.withTransaction
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

class RosterRepository(private val db: AppDatabase) {
    private val players = db.playerDao()
    private val roster = db.rosterDao()
    val attendance = roster.observeAllAttendanceWithPlayers()
    val assignments = roster.observeAllAssignmentsWithPlayers()

    suspend fun initializeAttendance(gameId: Long) = db.withTransaction {
        if (roster.attendanceCount(gameId) == 0) {
            roster.insertAttendance(players.activePlayers().map { GameAttendance(gameId, it.id, AttendanceStatus.AVAILABLE) })
        }
    }

    suspend fun setAttendance(gameId: Long, playerId: Long, status: AttendanceStatus) {
        check(roster.updateAttendanceStatus(gameId, playerId, status) == 1) { "Attendance record no longer exists." }
    }

    suspend fun missingActivePlayers(gameId: Long): List<Player> = roster.activePlayersMissingFromAttendance(gameId)

    suspend fun addMissingActivePlayers(gameId: Long) = db.withTransaction {
        roster.insertAttendance(roster.activePlayersMissingFromAttendance(gameId).map { GameAttendance(gameId, it.id, AttendanceStatus.AVAILABLE) })
    }
}
