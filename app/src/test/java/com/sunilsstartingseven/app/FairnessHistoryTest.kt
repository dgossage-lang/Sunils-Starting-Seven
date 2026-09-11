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
        val attendance = games.flatMap { playedGame ->
            buildList {
                add(HistoryAttendance(playedGame.id, 1, AttendanceStatus.AVAILABLE))
                add(HistoryAttendance(playedGame.id, 2, if (playedGame.id <= 5) AttendanceStatus.AVAILABLE else AttendanceStatus.AWAY))
            }
        }
        val assignments = buildList {
            repeat(10) { index -> add(assignment(index + 1L, 1, 0, if (index % 2 == 0) PlayingPosition.GOALKEEPER else null)) }
            repeat(5) { index -> add(assignment(index + 1L, 2, 0, if (index % 2 == 0) PlayingPosition.MID else null)) }
        }

        val summary = calculator.calculate(players(2), games, attendance, assignments)

        assertEquals(0.5, summary.player(1)!!.activeRotationsPerAttendedGame, 0.01)
        assertEquals(0.6, summary.player(2)!!.activeRotationsPerAttendedGame, 0.01)
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
