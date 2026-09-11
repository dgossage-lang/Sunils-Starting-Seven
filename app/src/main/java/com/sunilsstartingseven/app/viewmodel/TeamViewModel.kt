package com.sunilsstartingseven.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunilsstartingseven.app.AppContainer
import com.sunilsstartingseven.app.data.local.Game
import com.sunilsstartingseven.app.data.local.Player
import com.sunilsstartingseven.app.domain.model.PlayerInput
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TeamState(
    val players: List<Player> = emptyList(),
    val games: List<Game> = emptyList(),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val loadFailed: Boolean = false
)

class TeamViewModel(private val container: AppContainer) : ViewModel() {
    private val mutableState = MutableStateFlow(TeamState())
    val state = mutableState.asStateFlow()
    init { observe() }

    private fun observe() {
        viewModelScope.launch {
            combine(container.players.players, container.games.games) { players, games -> players to games }
                .catch { error ->
                    Log.e("StartingSeven", "Unable to read local team data", error)
                    mutableState.update { it.copy(loading = false, loadFailed = true, error = "Couldn't load your team. Please retry.") }
                }
                .collect { (players, games) -> mutableState.update { it.copy(players = players, games = games, loading = false, loadFailed = false) } }
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
