package com.sunilsstartingseven.app

import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import org.junit.Assert.*
import org.junit.Test

class RotationEngineTest {
    private val engine = FairRotationEngine()
    private val validator = RotationValidator()

    @Test fun rejectsFewerThanSevenPlayers() {
        assertFails { engine.generate(RotationRequest(players(6))) }
    }

    @Test fun supportsSevenThroughTwelvePlayers() {
        (7..12).forEach { count ->
            val result = engine.generate(RotationRequest(players(count), seed = count))
            val validation = validator.validate(result.assignments, players(count).map { it.id }.toSet(), emptySet())
            assertTrue("$count players: ${validation.errors}", validation.valid)
            assertEquals(count * 4, result.assignments.size)
            (0..3).forEach { rotation ->
                assertEquals(7, result.assignments.count { it.rotationIndex == rotation && it.playingState == PlayingState.ON_FIELD })
                assertEquals(count - 7, result.assignments.count { it.rotationIndex == rotation && it.playingState == PlayingState.BENCH })
            }
        }
    }

    @Test fun tenPlayersAvoidConsecutiveBenchAndGkToBench() {
        val result = engine.generate(RotationRequest(players(10), seed = 10))
        val byPlayer = result.assignments.groupBy { it.playerId }
        byPlayer.forEach { (id, rows) ->
            rows.sortedBy { it.rotationIndex }.zipWithNext().forEach { (a, b) ->
                assertFalse("$id consecutive bench", a.playingState == PlayingState.BENCH && b.playingState == PlayingState.BENCH)
                assertFalse("$id GK to bench", a.position == PlayingPosition.GOALKEEPER && b.playingState == PlayingState.BENCH)
            }
        }
    }

    @Test fun awayPlayersReceiveNoAssignments() {
        val available = players(9)
        val away = setOf(99L, 100L)
        val result = engine.generate(RotationRequest(available, awayPlayerIds = away, seed = 3))
        assertTrue(result.assignments.none { it.playerId in away })
        assertTrue(validator.validate(result.assignments, available.map { it.id }.toSet(), away).valid)
    }

    @Test fun reversedOrderStillProducesValidRoster() {
        val forward = engine.generate(RotationRequest(players(10), seed = 4))
        val reversed = engine.generate(RotationRequest(players(10).reversed(), seed = 4))
        assertTrue(validator.validate(forward.assignments, players(10).map { it.id }.toSet(), emptySet()).valid)
        assertTrue(validator.validate(reversed.assignments, players(10).map { it.id }.toSet(), emptySet()).valid)
    }

    @Test fun historicalPositionDeficitInfluencesNextGame() {
        val roster = players(10)
        val overloadedGk = (1..8).map { HistoricalAssignment(1L, PlayingState.ON_FIELD, PlayingPosition.GOALKEEPER) }
        val result = engine.generate(RotationRequest(roster, history = overloadedGk, seed = 1))
        assertTrue(result.assignments.count { it.playerId == 1L && it.position == PlayingPosition.GOALKEEPER } <= 1)
    }

    @Test fun seventeenRoundAllPresentStaysBalanced() {
        val roster = players(10)
        val history = mutableListOf<HistoricalAssignment>()
        repeat(17) { round ->
            val result = engine.generate(RotationRequest(roster, history = history, seed = round + 1))
            val validation = validator.validate(result.assignments, roster.map { it.id }.toSet(), emptySet())
            assertTrue("round ${round + 1}: ${validation.errors}", validation.valid)
            history += result.assignments.map { HistoricalAssignment(it.playerId, it.playingState, it.position) }
        }
        val byPlayer = history.groupBy { it.playerId }
        byPlayer.forEach { (id, rows) ->
            val bench = rows.count { it.playingState == PlayingState.BENCH }
            assertTrue("bench $id = $bench", bench in 20..21)
            PlayingPosition.entries.forEach { position ->
                val count = rows.count { it.position == position }
                assertTrue("$id $position = $count", count in 6..7)
            }
        }
    }

    @Test fun absenceCompensatesProgressively() {
        val all = players(10)
        val history = mutableListOf<HistoricalAssignment>()
        repeat(17) { round ->
            val available = if (round in setOf(4, 9)) all.filterNot { it.id == 3L } else all
            val result = engine.generate(RotationRequest(available, awayPlayerIds = all.map { it.id }.toSet() - available.map { it.id }.toSet(), history = history, seed = round + 20))
            assertTrue(result.assignments.none { round in setOf(4, 9) && it.playerId == 3L })
            assertTrue(validator.validate(result.assignments, available.map { it.id }.toSet(), all.map { it.id }.toSet() - available.map { it.id }.toSet()).valid)
            history += result.assignments.map { HistoricalAssignment(it.playerId, it.playingState, it.position) }
        }
        val activeCounts = all.associate { player -> player.id to history.count { it.playerId == player.id && it.playingState == PlayingState.ON_FIELD } }
        assertTrue(activeCounts.getValue(3L) >= activeCounts.values.min())
    }

    private fun players(count: Int) = (1..count).map { RotationPlayer(it.toLong(), "Player $it") }
    private fun assertFails(block: () -> Unit) {
        var failed = false
        try { block() } catch (_: Exception) { failed = true }
        assertTrue(failed)
    }
}
