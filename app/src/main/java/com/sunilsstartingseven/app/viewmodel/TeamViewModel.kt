package com.sunilsstartingseven.app.viewmodel

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sunilsstartingseven.app.AppContainer
import com.sunilsstartingseven.app.data.backup.BackupException
import com.sunilsstartingseven.app.data.backup.MAX_ARCHIVE_BACKUP_BYTES
import com.sunilsstartingseven.app.data.backup.ValidatedBackup
import com.sunilsstartingseven.app.data.media.MediaException
import com.sunilsstartingseven.app.data.local.*
import com.sunilsstartingseven.app.domain.history.FairnessHistoryCalculator
import com.sunilsstartingseven.app.domain.history.HistoryAssignment
import com.sunilsstartingseven.app.domain.history.HistoryAttendance
import com.sunilsstartingseven.app.domain.history.HistoryGame
import com.sunilsstartingseven.app.domain.history.HistoryPlayer
import com.sunilsstartingseven.app.domain.history.TeamFairnessSummary
import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.RotationAssignment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.LocalDate

data class TeamState(
    val seasons: List<Season> = emptyList(),
    val selectedSeasonId: Long? = null,
    val players: List<Player> = emptyList(),
    val games: List<Game> = emptyList(),
    val attendance: Map<Long, List<AttendanceWithPlayer>> = emptyMap(),
    val assignments: Map<Long, List<AssignmentWithPlayer>> = emptyMap(),
    val fairness: TeamFairnessSummary = FairnessHistoryCalculator().calculate(emptyList(), emptyList(), emptyList(), emptyList()),
    val loading: Boolean = true,
    val saving: Boolean = false,
    val error: String? = null,
    val backupMessage: String? = null,
    val restorePreview: ValidatedBackup? = null,
    val loadFailed: Boolean = false
) {
    val visibleGames: List<Game> get() = games.filter { selectedSeasonId == null || it.seasonId == selectedSeasonId }
    fun game(id: Long): Game? = games.find { it.id == id }
    fun attendanceFor(gameId: Long): List<AttendanceWithPlayer> = attendance[gameId].orEmpty()
    fun assignmentsFor(gameId: Long): List<AssignmentWithPlayer> = assignments[gameId].orEmpty()
    fun attendanceSummary(gameId: Long): AttendanceSummary = attendanceSummary(attendanceFor(gameId).map { it.attendance.status }, game(gameId)?.fieldPositions?.size ?: 7)
    fun missingActivePlayersFor(gameId: Long): List<Player> {
        val represented = attendanceFor(gameId).map { it.player.id }.toSet()
        return players.filter { it.isActive && it.id !in represented }
    }
}

class TeamViewModel(private val container: AppContainer) : ViewModel() {
    private val selectedSeason = MutableStateFlow<Long?>(null)
    private val mutableState = MutableStateFlow(TeamState())
    private val fairnessCalculator = FairnessHistoryCalculator()
    val state = mutableState.asStateFlow()
    init {
        observe()
    }

    private fun observe() {
        viewModelScope.launch {
            combine(container.players.players, container.games.games, container.roster.attendance, container.roster.assignments) { players, games, attendance, assignments ->
                val flatAttendance = attendance.map {
                    HistoryAttendance(it.attendance.gameId, it.attendance.playerId, it.attendance.status)
                }
                val flatAssignments = assignments.map {
                    HistoryAssignment(it.assignment.gameId, it.assignment.rotationIndex, it.assignment.playerId, it.assignment.playingState, it.assignment.position)
                }
                TeamState(
                    players = players,
                    games = games,
                    attendance = attendance.groupBy { it.attendance.gameId },
                    assignments = assignments.groupBy { it.assignment.gameId },
                    loading = false,
                    loadFailed = false
                )
            }
                .combine(container.seasons.seasons) { next, seasons -> next.copy(seasons = seasons) }
                .combine(selectedSeason) { next, selected ->
                    val validSelection = selected?.takeIf { id -> next.seasons.any { it.id == id } }
                    val games = next.games.filter { validSelection == null || it.seasonId == validSelection }
                    next.copy(selectedSeasonId = validSelection, fairness = fairnessCalculator.calculate(
                        next.players.map { HistoryPlayer(it.id, it.displayName, it.isActive, it.photoPath) },
                        games.map { game -> HistoryGame(game.id, game.roundNumber, game.gameDate, game.status, game.fieldPositions, next.seasons.find { it.id == game.seasonId }?.name ?: "Earlier games") },
                        next.attendance.values.flatten().map { HistoryAttendance(it.attendance.gameId, it.attendance.playerId, it.attendance.status) },
                        next.assignments.values.flatten().map { HistoryAssignment(it.assignment.gameId, it.assignment.rotationIndex, it.assignment.playerId, it.assignment.playingState, it.assignment.position) }
                    ))
                }
                .catch { error ->
                    Log.e("StartingSeven", "Unable to read local team data", error)
                    mutableState.update { it.copy(loading = false, loadFailed = true, error = "Couldn't load your team. Please retry.") }
                }
                .collect { next -> mutableState.update { next.copy(saving = it.saving, error = it.error, backupMessage = it.backupMessage, restorePreview = it.restorePreview) } }
        }
    }

    fun retryLoad() {
        if (!state.value.loadFailed) return
        mutableState.update { it.copy(loading = true, loadFailed = false, error = null) }
        observe()
    }

    fun clearError() { mutableState.update { it.copy(error = null) } }
    fun clearBackupMessage() { mutableState.update { it.copy(backupMessage = null) } }
    fun clearRestorePreview() { mutableState.update { it.copy(restorePreview = null) } }

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
                mutableState.update { it.copy(error = error.userMessage("Couldn't save your changes. Please try again.")) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun savePlayer(id: Long?, first: String, initial: String, carryAcrossSeasons: Boolean = false, onSuccess: () -> Unit) {
        if (!PlayerInput.validate(first, initial).isValid) return
        write(onSuccess) { container.players.save(id, first, initial, carryAcrossSeasons) }
    }

    fun setActive(player: Player, active: Boolean) = write { container.players.setActive(player.id, active) }

    fun createGame(round: Int, date: LocalDate, notes: String, onSuccess: () -> Unit) {
        if (round <= 0) return
        write(onSuccess) { container.games.createDraft(round, date, notes, state.value.selectedSeasonId) }
    }

    fun updateGame(gameId: Long, round: Int, date: LocalDate, notes: String, onSuccess: () -> Unit) {
        if (round <= 0) return
        write(onSuccess) { container.games.updateDraft(gameId, round, date, notes) }
    }

    fun selectSeason(id: Long?) { selectedSeason.value = id }

    fun saveSeason(id: Long?, name: String, onSuccess: () -> Unit) = write(onSuccess) {
        selectedSeason.value = container.seasons.save(id, name)
    }

    fun updateLayout(gameId: Long, positions: List<PlayingPosition>, options: List<PlayingPosition>, onSuccess: () -> Unit) =
        write(onSuccess) { container.roster.updateLayout(gameId, positions, options) }

    fun initializeAttendance(gameId: Long) = write { container.roster.initializeAttendance(gameId) }

    fun setAttendance(gameId: Long, playerId: Long, status: AttendanceStatus) =
        write { container.roster.setAttendance(gameId, playerId, status) }

    fun addMissingActivePlayers(gameId: Long) = write { container.roster.addMissingActivePlayers(gameId) }

    fun generateLineup(gameId: Long) = write { container.roster.generateLineup(gameId) }

    fun markPlayed(gameId: Long, onSuccess: () -> Unit = {}) = write(onSuccess) { container.roster.markPlayed(gameId) }

    fun saveManualLineup(gameId: Long, assignments: List<RotationAssignment>, onSuccess: () -> Unit) =
        write(onSuccess) { container.roster.saveManualLineup(gameId, assignments) }

    fun backupFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "Sunils-Starting-Seven-Backup-${now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm"))}.sssbackup"

    fun exportBackup(resolver: ContentResolver, uri: Uri) {
        if (state.value.saving || state.value.loading || state.value.loadFailed) return
        mutableState.update { it.copy(saving = true, error = null, backupMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    resolver.openOutputStream(uri, "wt")?.use { stream -> container.backup.writeBackupArchive(stream) }
                        ?: throw BackupException("The backup file could not be opened for writing.")
                }
                mutableState.update { it.copy(backupMessage = "Backup created") }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("StartingSeven", "Unable to create backup", error)
                mutableState.update { it.copy(error = error.userMessage("Couldn't create the backup. Please try again.")) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun readRestorePreview(resolver: ContentResolver, uri: Uri) {
        if (state.value.saving || state.value.loading || state.value.loadFailed) return
        mutableState.update { it.copy(saving = true, error = null, backupMessage = null, restorePreview = null) }
        viewModelScope.launch {
            try {
                val validated = withContext(Dispatchers.IO) {
                    val bytes = resolver.openInputStream(uri)?.use { stream ->
                        val data = stream.readCapped(MAX_ARCHIVE_BACKUP_BYTES)
                        if (data.size > MAX_ARCHIVE_BACKUP_BYTES) throw BackupException("This backup file is too large to restore.")
                        data
                    } ?: throw BackupException("The backup file could not be opened.")
                    container.backup.validateBytes(bytes)
                }
                mutableState.update { it.copy(restorePreview = validated) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("StartingSeven", "Unable to read backup", error)
                mutableState.update { it.copy(error = error.userMessage("Couldn't read that backup file.")) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    fun setPlayerPhoto(playerId: Long, resolver: ContentResolver, uri: Uri) = write { container.players.setPhoto(playerId, resolver, uri) }

    fun removePlayerPhoto(playerId: Long) = write { container.players.removePhoto(playerId) }

    fun setGamePhoto(gameId: Long, resolver: ContentResolver, uri: Uri) = write { container.games.setPhoto(gameId, resolver, uri) }

    fun removeGamePhoto(gameId: Long) = write { container.games.removePhoto(gameId) }

    fun restoreBackup(onSuccess: () -> Unit) {
        val validated = state.value.restorePreview ?: return
        if (state.value.saving || state.value.loading || state.value.loadFailed) return
        mutableState.update { it.copy(saving = true, error = null, backupMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { container.backup.restore(validated) }
                selectedSeason.value = null
                mutableState.update { it.copy(restorePreview = null, backupMessage = "Backup restored") }
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e("StartingSeven", "Unable to restore backup", error)
                mutableState.update { it.copy(error = error.userMessage("Couldn't restore the backup. Your current data was left unchanged.")) }
            } finally {
                mutableState.update { it.copy(saving = false) }
            }
        }
    }

    private fun Exception.userMessage(fallback: String): String = if (this is BackupException || this is MediaException) message ?: fallback else fallback

    private fun java.io.InputStream.readCapped(maxBytes: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
            if (output.size() > maxBytes) break
        }
        return output.toByteArray()
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
