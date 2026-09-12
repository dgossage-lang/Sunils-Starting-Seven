package com.sunilsstartingseven.app.domain.history

import com.sunilsstartingseven.app.domain.model.AttendanceStatus
import com.sunilsstartingseven.app.domain.model.GameStatus
import com.sunilsstartingseven.app.domain.model.PlayingPosition
import com.sunilsstartingseven.app.domain.model.PlayingState
import com.sunilsstartingseven.app.domain.model.PositionFamily
import com.sunilsstartingseven.app.domain.rotation.FieldPositions
import com.sunilsstartingseven.app.domain.rotation.ROTATION_COUNT
import java.time.LocalDate
import kotlin.math.abs

private fun percent(value: Int, total: Int): Int = if (total == 0) 0 else ((value.toDouble() / total) * 100).toInt()

data class HistoryPlayer(val id: Long, val displayName: String, val isActive: Boolean, val photoPath: String? = null)
data class HistoryGame(val id: Long, val roundNumber: Int, val gameDate: LocalDate, val status: GameStatus)
data class HistoryAttendance(val gameId: Long, val playerId: Long, val status: AttendanceStatus)
data class HistoryAssignment(val gameId: Long, val rotationIndex: Int, val playerId: Long, val playingState: PlayingState, val position: PlayingPosition?)

enum class FairnessStatus(val label: String) {
    NO_HISTORY("No history"),
    BUILDING_HISTORY("Building history"),
    BALANCED("Balanced"),
    NEEDS_OPPORTUNITY("Needs opportunity"),
    SLIGHTLY_AHEAD("Slightly ahead")
}

data class GameHistoryEntry(
    val gameId: Long,
    val roundNumber: Int,
    val gameDate: LocalDate,
    val attendance: AttendanceStatus,
    val rotations: List<HistoryAssignment>
)

data class PlayerHistorySummary(
    val player: HistoryPlayer,
    val gamesAttended: Int,
    val gamesAway: Int,
    val activeRotations: Int,
    val benchRotations: Int,
    val exactPositions: Map<PlayingPosition, Int>,
    val families: Map<PositionFamily, Int>,
    val activeRotationsPerAttendedGame: Double,
    val benchRotationsPerAttendedGame: Double,
    val fairnessStatus: FairnessStatus,
    val nextOpportunities: List<PlayingPosition>,
    val games: List<GameHistoryEntry>
) {
    val totalRotationOpportunities: Int get() = gamesAttended * ROTATION_COUNT
    val activePercentage: Int get() = percent(activeRotations, totalRotationOpportunities)
    val benchPercentage: Int get() = percent(benchRotations, totalRotationOpportunities)
    val distinctExactPositions: Int get() = exactPositions.count { it.value > 0 }
    val distinctFamilies: Int get() = families.count { it.value > 0 }
}

data class TeamPositionBalance(val family: PositionFamily, val min: Int, val max: Int, val label: String)
data class BenchBalance(val minBench: Int, val maxBench: Int, val label: String)

data class TeamFairnessSummary(
    val playedGames: Int,
    val activePlayers: List<PlayerHistorySummary>,
    val inactivePlayers: List<PlayerHistorySummary>,
    val teamActiveRate: Double,
    val teamBenchRate: Double,
    val positionBalance: List<TeamPositionBalance>,
    val benchBalance: BenchBalance?
) {
    val hasPlayedHistory: Boolean get() = playedGames > 0
    val playingTimeLabel: String get() = if (activePlayers.any { it.fairnessStatus == FairnessStatus.NEEDS_OPPORTUNITY || it.fairnessStatus == FairnessStatus.SLIGHTLY_AHEAD }) "Needs a look" else "Well balanced"
    val positionLabel: String get() = if (positionBalance.any { it.max - it.min > 2 }) "Still evening out" else "Well distributed"
    fun player(playerId: Long): PlayerHistorySummary? = (activePlayers + inactivePlayers).find { it.player.id == playerId }
}

class FairnessHistoryCalculator {
    fun calculate(
        players: List<HistoryPlayer>,
        games: List<HistoryGame>,
        attendance: List<HistoryAttendance>,
        assignments: List<HistoryAssignment>
    ): TeamFairnessSummary {
        val playedGames = games.filter { it.status == GameStatus.PLAYED }.sortedWith(compareBy({ it.gameDate }, { it.roundNumber }, { it.id }))
        val playedIds = playedGames.map { it.id }.toSet()
        val playedAttendance = attendance.filter { it.gameId in playedIds }
        val availableGamePlayerPairs = playedAttendance
            .filter { it.status == AttendanceStatus.AVAILABLE }
            .map { it.gameId to it.playerId }
            .toSet()
        val playedAssignments = assignments.filter { it.gameId in playedIds && (it.gameId to it.playerId) in availableGamePlayerPairs }
        val attendanceByPlayer = playedAttendance.groupBy { it.playerId }
        val assignmentsByPlayer = playedAssignments.groupBy { it.playerId }
        val assignmentsByGamePlayer = playedAssignments.groupBy { it.gameId to it.playerId }

        val preliminary = players.map { player ->
            val playerAttendance = attendanceByPlayer[player.id].orEmpty()
            val playerAssignments = assignmentsByPlayer[player.id].orEmpty()
            val gamesAttended = playerAttendance.count { it.status == AttendanceStatus.AVAILABLE }
            val gamesAway = playerAttendance.count { it.status == AttendanceStatus.AWAY }
            val activeRotations = playerAssignments.count { it.playingState == PlayingState.ON_FIELD }
            val benchRotations = playerAssignments.count { it.playingState == PlayingState.BENCH }
            val exact = FieldPositions.associateWith { position -> playerAssignments.count { it.position == position } }
            val families = PositionFamily.entries.associateWith { family -> playerAssignments.count { it.position?.family == family } }
            val gameRows = playedGames.mapNotNull { game ->
                val row = playerAttendance.find { it.gameId == game.id } ?: return@mapNotNull null
                GameHistoryEntry(
                    gameId = game.id,
                    roundNumber = game.roundNumber,
                    gameDate = game.gameDate,
                    attendance = row.status,
                    rotations = assignmentsByGamePlayer[game.id to player.id].orEmpty().sortedBy { it.rotationIndex }
                )
            }.sortedWith(compareByDescending<GameHistoryEntry> { it.gameDate }.thenByDescending { it.roundNumber }.thenByDescending { it.gameId })
            PlayerHistorySummary(
                player = player,
                gamesAttended = gamesAttended,
                gamesAway = gamesAway,
                activeRotations = activeRotations,
                benchRotations = benchRotations,
                exactPositions = exact,
                families = families,
                activeRotationsPerAttendedGame = rate(activeRotations, gamesAttended),
                benchRotationsPerAttendedGame = rate(benchRotations, gamesAttended),
                fairnessStatus = FairnessStatus.NO_HISTORY,
                nextOpportunities = exactDeficits(activeRotations, exact),
                games = gameRows
            )
        }

        val activeWithAttendance = preliminary.filter { it.player.isActive && it.gamesAttended > 0 }
        val teamActiveRate = activeWithAttendance.map { it.activeRotationsPerAttendedGame }.averageOrZero()
        val teamBenchRate = activeWithAttendance.map { it.benchRotationsPerAttendedGame }.averageOrZero()
        val summaries = preliminary.map { summary ->
            summary.copy(fairnessStatus = statusFor(summary, teamActiveRate))
        }
        val active = summaries.filter { it.player.isActive }
        val inactive = summaries.filterNot { it.player.isActive }

        return TeamFairnessSummary(
            playedGames = playedGames.size,
            activePlayers = active,
            inactivePlayers = inactive,
            teamActiveRate = teamActiveRate,
            teamBenchRate = teamBenchRate,
            positionBalance = teamPositionBalance(active),
            benchBalance = benchBalance(active)
        )
    }

    private fun statusFor(player: PlayerHistorySummary, teamActiveRate: Double): FairnessStatus = when {
        player.gamesAttended == 0 -> FairnessStatus.NO_HISTORY
        player.gamesAttended < MINIMUM_ATTENDED_GAMES_FOR_LABEL -> FairnessStatus.BUILDING_HISTORY
        player.activeRotationsPerAttendedGame < teamActiveRate - ACTIVE_RATE_TOLERANCE -> FairnessStatus.NEEDS_OPPORTUNITY
        player.activeRotationsPerAttendedGame > teamActiveRate + ACTIVE_RATE_TOLERANCE -> FairnessStatus.SLIGHTLY_AHEAD
        else -> FairnessStatus.BALANCED
    }

    private fun exactDeficits(activeRotations: Int, counts: Map<PlayingPosition, Int>): List<PlayingPosition> {
        if (activeRotations < MINIMUM_ACTIVE_ROTATIONS_FOR_POSITION_INSIGHT) return emptyList()
        val expected = activeRotations.toDouble() / FieldPositions.size
        return FieldPositions
            .map { position -> position to (expected - (counts[position] ?: 0)) }
            .filter { it.second > POSITION_DEFICIT_TOLERANCE }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }

    private fun teamPositionBalance(activePlayers: List<PlayerHistorySummary>): List<TeamPositionBalance> {
        val players = activePlayers.filter { it.gamesAttended > 0 }
        if (players.isEmpty()) return emptyList()
        return PositionFamily.entries.map { family ->
            val values = players.map { it.families[family] ?: 0 }
            val min = values.minOrNull() ?: 0
            val max = values.maxOrNull() ?: 0
            TeamPositionBalance(family, min, max, if (max - min <= 2) "balanced" else "spreading out")
        }
    }

    private fun benchBalance(activePlayers: List<PlayerHistorySummary>): BenchBalance? {
        val players = activePlayers.filter { it.gamesAttended > 0 }
        if (players.isEmpty()) return null
        val values = players.map { it.benchRotations }
        val min = values.minOrNull() ?: 0
        val max = values.maxOrNull() ?: 0
        return BenchBalance(min, max, if (max - min <= 2 || abs(max - min) <= ROTATION_COUNT) "Well balanced" else "Needs a look")
    }

    private fun rate(count: Int, attendedGames: Int): Double = if (attendedGames == 0) 0.0 else count.toDouble() / attendedGames
    private fun List<Double>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
    companion object {
        const val MINIMUM_ATTENDED_GAMES_FOR_LABEL = 3
        const val ACTIVE_RATE_TOLERANCE = 0.5
        const val MINIMUM_ACTIVE_ROTATIONS_FOR_POSITION_INSIGHT = 8
        const val POSITION_DEFICIT_TOLERANCE = 0.75
    }
}
