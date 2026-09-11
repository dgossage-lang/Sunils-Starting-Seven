package com.sunilsstartingseven.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunilsstartingseven.app.AppContainer
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TeamState(
    val players: List<Player> = emptyList(),
    val games: List<Game> = emptyList(),
    val attendance: Map<Long, List<AttendanceWithPlayer>> = emptyMap(),
    val assignments: Map<Long, List<AssignmentWithPlayer>> = emptyMap(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val loadFailed: Boolean = false
) {
    fun game(id: Long): Game? = games.find { it.id == id }
    fun attendanceFor(gameId: Long): List<AttendanceWithPlayer> = attendance[gameId].orEmpty()
    fun assignmentsFor(gameId: Long): List<AssignmentWithPlayer> = assignments[gameId].orEmpty()
    fun attendanceSummary(gameId: Long): AttendanceSummary = attendanceSummary(attendanceFor(gameId).map { it.attendance.status })
    fun missingActivePlayersFor(gameId: Long): List<Player> {
        val represented = attendanceFor(gameId).map { it.player.id }.toSet()
        return players.filter { it.isActive && it.id !in represented }
    }
}

class TeamViewModel(private val container: AppContainer) : ViewModel() {
    private val mutableState = MutableStateFlow(TeamState())
    val state = mutableState.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                container.developmentSeed.applyDevelopmentSeedIfNeeded()
                observe()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("StartingSeven", "Unable to apply development seed", error)
                mutableState.update { it.copy(loading = false, loadFailed = true, error = "Couldn't prepare the development roster. Please retry.") }
            }
        }
    }

    private fun observe() {
        viewModelScope.launch {
            combine(container.players.players, container.games.games, container.roster.attendance, container.roster.assignments) { players, games, attendance, assignments ->
                TeamState(
                    players = players,
                    games = games,
                    attendance = attendance.groupBy { it.attendance.gameId },
                    assignments = assignments.groupBy { it.assignment.gameId },
                    loading = false,
                    loadFailed = false
                )
            }
                .catch { error ->
                    Log.e("StartingSeven", "Unable to read local team data", error)
                    mutableState.update { it.copy(loading = false, loadFailed = true, error = "Couldn't load your team. Please retry.") }
                }
                .collect { next -> mutableState.update { next.copy(saving = it.saving, error = it.error) } }
        }
    }

    fun retryLoad() {
        if (!state.value.loadFailed) return
        mutableState.update { it.copy(loading = true, loadFailed = false, error = null) }
        observe()
    }

    fun clearError() { mutableState.update { it.copy(error = null) } }

    private fun write(onSuccess: () -> Unit = {}, action: suspend () -> Unit) {
        if (state.value.saving || state.value.loading || state.value.loadFailed) return
        mutableState.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                action()
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("StartingSeven", "Unable to save local team data", error)
                mutableState.update { it.copy(error = "Couldn't save your changes. Please try again.") }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun savePlayer(id: Long?, first: String, initial: String, onSuccess: () -> Unit) {
        if (!PlayerInput.validate(first, initial).isValid) return
        write(onSuccess) { container.players.save(id, first, initial) }
    }

    fun setActive(player: Player, active: Boolean) = write { container.players.setActive(player.id, active) }

    fun createGame(round: Int, date: LocalDate, notes: String, onSuccess: () -> Unit) {
        if (round <= 0) return
        write(onSuccess) { container.games.createDraft(round, date, notes) }
    }

    fun updateGame(gameId: Long, round: Int, date: LocalDate, notes: String, onSuccess: () -> Unit) {
        if (round <= 0) return
        write(onSuccess) { container.games.updateDraft(gameId, round, date, notes) }
    }

    fun initializeAttendance(gameId: Long) = write { container.roster.initializeAttendance(gameId) }

    fun setAttendance(gameId: Long, playerId: Long, status: AttendanceStatus) =
        write { container.roster.setAttendance(gameId, playerId, status) }

    fun addMissingActivePlayers(gameId: Long) = write { container.roster.addMissingActivePlayers(gameId) }

    fun generateLineup(gameId: Long) = write { container.roster.generateLineup(gameId) }

    fun markPlayed(gameId: Long, onSuccess: () -> Unit = {}) = write(onSuccess) { container.roster.markPlayed(gameId) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TeamViewModel::class.java))
                return TeamViewModel(container) as T
            }
        }
    }
}
