package com.sunilsstartingseven.app.data.repository

import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.model.PlayerInput
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
}
