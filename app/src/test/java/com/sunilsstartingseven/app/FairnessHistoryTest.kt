package com.sunilsstartingseven.app

import com.sunilsstartingseven.app.domain.history.FairnessHistoryCalculator
import com.sunilsstartingseven.app.domain.history.FairnessStatus
import com.sunilsstartingseven.app.domain.history.HistoryAssignment
import com.sunilsstartingseven.app.domain.history.HistoryAttendance
import com.sunilsstartingseven.app.domain.history.HistoryGame
import com.sunilsstartingseven.app.domain.history.HistoryPlayer
import com.sunilsstartingseven.app.domain.model.AttendanceStatus
import com.sunilsstartingseven.app.domain.model.GameStatus
import com.sunilsstartingseven.app.domain.model.PlayingPosition
import com.sunilsstartingseven.app.domain.model.PlayingState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FairnessHistoryTest {
    private val calculator = FairnessHistoryCalculator()

    @Test fun playedGamesDefineAttendanceAndPositionHistory() {
        val summary = calculator.calculate(
            players = players(1),
            games = listOf(game(1, GameStatus.PLAYED), game(2, GameStatus.PLAYED)),
            attendance = listOf(
                HistoryAttendance(1, 1, AttendanceStatus.AVAILABLE),
                HistoryAttendance(2, 1, AttendanceStatus.AWAY)
            ),
            assignments = listOf(
                assignment(1, 1, 0, PlayingPosition.GOALKEEPER),
                assignment(1, 1, 1, PlayingPosition.MID),
                assignment(1, 1, 2, null),
                assignment(2, 1, 0, PlayingPosition.STRIKER)
            )
        )

        val player = summary.player(1)!!
        assertEquals(2, summary.playedGames)
        assertEquals(1, player.gamesAttended)
        assertEquals(1, player.gamesAway)
        assertEquals(2, player.activeRotations)
        assertEquals(1, player.benchRotations)
        assertEquals(1, player.exactPositions.getValue(PlayingPosition.GOALKEEPER))
        assertEquals(0, player.exactPositions.getValue(PlayingPosition.STRIKER))
        assertEquals("Away", player.games.first { it.gameId == 2L }.attendance.label)
    }

    @Test fun draftReadyAndCancelledGamesDoNotContribute() {
        val summary = calculator.calculate(
            players = players(1),
            games = listOf(game(1, GameStatus.DRAFT), game(2, GameStatus.READY), game(3, GameStatus.CANCELLED), game(4, GameStatus.PLAYED)),
            attendance = listOf(
                HistoryAttendance(1, 1, AttendanceStatus.AVAILABLE),
                HistoryAttendance(2, 1, AttendanceStatus.AVAILABLE),
                HistoryAttendance(3, 1, AttendanceStatus.AVAILABLE),
                HistoryAttendance(4, 1, AttendanceStatus.AVAILABLE)
            ),
            assignments = listOf(
                assignment(1, 1, 0, PlayingPosition.GOALKEEPER),
                assignment(2, 1, 0, PlayingPosition.MID),
                assignment(3, 1, 0, PlayingPosition.STRIKER),
                assignment(4, 1, 0, PlayingPosition.LEFT_WING)
            )
        )

        val player = summary.player(1)!!
        assertEquals(1, summary.playedGames)
        assertEquals(1, player.gamesAttended)
        assertEquals(1, player.activeRotations)
        assertEquals(1, player.exactPositions.getValue(PlayingPosition.LEFT_WING))
        assertEquals(0, player.exactPositions.getValue(PlayingPosition.GOALKEEPER))
    }

    @Test fun fairnessUsesAttendanceNormalisedPlayingRate() {
        val games = (1..10).map { game(it.toLong(), GameStatus.PLAYED) }
        val attendance = games.flatMap { g -> (1..9).map { id ->
            HistoryAttendance(g.id, id.toLong(), if (id == 2 && g.id > 5) AttendanceStatus.AWAY else AttendanceStatus.AVAILABLE)
        } }
        val history = mutableListOf<com.sunilsstartingseven.app.domain.rotation.HistoricalAssignment>()
        val assignments = games.flatMap { g ->
            val available = attendance.filter { it.gameId == g.id && it.status == AttendanceStatus.AVAILABLE }
            val result = com.sunilsstartingseven.app.domain.rotation.FairRotationEngine().generate(
                com.sunilsstartingseven.app.domain.rotation.RotationRequest(available.map { com.sunilsstartingseven.app.domain.rotation.RotationPlayer(it.playerId) }, history = history.toList(), seed = g.id.toInt()))
            history += result.assignments.map { com.sunilsstartingseven.app.domain.rotation.HistoricalAssignment(it.playerId, it.playingState, it.position) }
            result.assignments.map { HistoryAssignment(g.id, it.rotationIndex, it.playerId, it.playingState, it.position) }
        }
        val summary = calculator.calculate(players(9), games, attendance, assignments)
        assertEquals(10, summary.player(1)!!.gamesAttended)
        assertEquals(5, summary.player(2)!!.gamesAttended)
        assertEquals(5, summary.player(2)!!.gamesAway)
        assertEquals(5 * 28.0 / 9 + 5 * 28.0 / 8, summary.player(1)!!.expectedActiveRotations, .001)
        assertEquals(5 * 28.0 / 9, summary.player(2)!!.expectedActiveRotations, .001)
        assertEquals(FairnessStatus.BALANCED, summary.player(1)!!.fairnessStatus)
        assertEquals(FairnessStatus.BALANCED, summary.player(2)!!.fairnessStatus)
    }

    @Test fun earlyHistoryAvoidsStrongLabels() {
        val summary = calculator.calculate(
            players = players(2),
            games = listOf(game(1, GameStatus.PLAYED)),
            attendance = listOf(
                HistoryAttendance(1, 1, AttendanceStatus.AVAILABLE),
                HistoryAttendance(1, 2, AttendanceStatus.AVAILABLE)
            ),
            assignments = listOf(
                assignment(1, 1, 0, PlayingPosition.GOALKEEPER),
                assignment(1, 2, 0, null)
            )
        )

        assertEquals(FairnessStatus.BUILDING_HISTORY, summary.player(1)!!.fairnessStatus)
        assertEquals(FairnessStatus.BUILDING_HISTORY, summary.player(2)!!.fairnessStatus)
    }

    @Test fun nextOpportunitiesUseExactPositionDeficits() {
        val assignments = buildList {
            repeat(8) { rotation -> add(assignment(1, 1, rotation, if (rotation % 2 == 0) PlayingPosition.GOALKEEPER else PlayingPosition.DEFENDER_1)) }
        }

        val summary = calculator.calculate(
            players = players(1),
            games = listOf(game(1, GameStatus.PLAYED)),
            attendance = listOf(HistoryAttendance(1, 1, AttendanceStatus.AVAILABLE)),
            assignments = assignments
        )

        assertTrue(PlayingPosition.MID in summary.player(1)!!.nextOpportunities)
        assertTrue(PlayingPosition.GOALKEEPER !in summary.player(1)!!.nextOpportunities)
        assertTrue(PlayingPosition.DEFENDER_1 !in summary.player(1)!!.nextOpportunities)
    }

    private fun players(count: Int) = (1..count).map { HistoryPlayer(it.toLong(), "Player $it", true) }
    private fun game(id: Long, status: GameStatus) = HistoryGame(id, id.toInt(), LocalDate.of(2026, 1, 1).plusDays(id), status)
    private fun assignment(gameId: Long, playerId: Long, rotation: Int, position: PlayingPosition?) =
        HistoryAssignment(gameId, rotation, playerId, if (position == null) PlayingState.BENCH else PlayingState.ON_FIELD, position)
}
