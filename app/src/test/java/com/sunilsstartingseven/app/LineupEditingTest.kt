package com.sunilsstartingseven.app

import com.sunilsstartingseven.app.domain.model.*
import com.sunilsstartingseven.app.domain.rotation.*
import org.junit.Assert.*
import org.junit.Test

class LineupEditingTest {
    private val validator = StructuralRosterValidator()
    private val names = (1L..8L).associateWith { "Player $it" }

    @Test fun onFieldSwapExchangesExactPositionsAndRetainsValidRoster() {
        val edited = LineupSwaps.swap(roster(), 0, 1, 5)
        assertEquals(PlayingPosition.MID, edited.single { it.rotationIndex == 0 && it.playerId == 1L }.position)
        assertEquals(PlayingPosition.GOALKEEPER, edited.single { it.rotationIndex == 0 && it.playerId == 5L }.position)
        assertTrue(validator.validate(edited, names.keys, emptySet()).valid)
    }

    @Test fun fieldToBenchSwapTransfersExactPositionAndRetainsValidRoster() {
        val edited = LineupSwaps.swap(roster(), 0, 1, 8)
        assertEquals(PlayingState.BENCH, edited.single { it.rotationIndex == 0 && it.playerId == 1L }.playingState)
        assertEquals(PlayingPosition.GOALKEEPER, edited.single { it.rotationIndex == 0 && it.playerId == 8L }.position)
        assertTrue(validator.validate(edited, names.keys, emptySet()).valid)
    }

    @Test fun benchToBenchSwapIsIgnored() {
        val withTwoBench = roster(9)
        assertEquals(withTwoBench, LineupSwaps.swap(withTwoBench, 0, 8, 9))
    }

    @Test fun swapOnlyChangesTheSelectedRotation() {
        val original = roster()
        val edited = LineupSwaps.swap(original, 1, 1, 3)
        assertEquals(original.filter { it.rotationIndex != 1 }, edited.filter { it.rotationIndex != 1 })
    }

    @Test fun assessmentReportsCoachRelevantTransitionsAndBalancedRosterStaysQuiet() {
        val balanced = balancedRoster()
        assertTrue(ManualLineupAssessment.assess(balanced, names).notes.isEmpty())
        val gkToBench = LineupSwaps.swap(roster(), 1, 1, 8)
        assertTrue(ManualLineupAssessment.assess(gkToBench, names).notes.any { it.message.contains("Goalkeeper to Bench") })
        val defToDef = LineupSwaps.swap(roster(), 1, 2, 3)
        assertTrue(ManualLineupAssessment.assess(defToDef, names).notes.any { it.message.contains("Defence") })
        val wingToWing = LineupSwaps.swap(roster(), 1, 4, 6)
        assertTrue(ManualLineupAssessment.assess(wingToWing, names).notes.any { it.message.contains("Wing") })
    }

    private fun roster(count: Int = 8): List<RotationAssignment> = buildList {
        repeat(ROTATION_COUNT) { rotation ->
            FieldPositions.forEachIndexed { index, position -> add(RotationAssignment((index + 1).toLong(), rotation, PlayingState.ON_FIELD, position)) }
            for (id in 8..count) add(RotationAssignment(id.toLong(), rotation, PlayingState.BENCH, null))
        }
    }

    private fun balancedRoster(): List<RotationAssignment> = buildList {
        repeat(ROTATION_COUNT) { rotation ->
            FieldPositions.forEachIndexed { index, position ->
                val player = ((index + rotation * 3) % FieldPositions.size + 1).toLong()
                add(RotationAssignment(player, rotation, PlayingState.ON_FIELD, position))
            }
        }
    }
}
