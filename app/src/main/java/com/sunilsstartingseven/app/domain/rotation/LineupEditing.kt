package com.sunilsstartingseven.app.domain.rotation

import com.sunilsstartingseven.app.domain.model.PlayingPosition
import com.sunilsstartingseven.app.domain.model.PlayingState
import com.sunilsstartingseven.app.domain.model.PositionFamily

/** Validates the invariants required to store a complete manual roster. */
class StructuralRosterValidator {
    fun validate(assignments: List<RotationAssignment>, availablePlayerIds: Set<Long>, awayPlayerIds: Set<Long>): RosterValidation {
        val errors = mutableListOf<String>()
        if (availablePlayerIds.size < FieldPositions.size) errors += "At least seven available players are required."
        if (assignments.any { it.rotationIndex !in 0 until ROTATION_COUNT }) errors += "The roster contains an unknown rotation."
        if (assignments.any { it.playerId in awayPlayerIds }) errors += "Away players cannot receive assignments."
        for (rotation in 0 until ROTATION_COUNT) {
            val rows = assignments.filter { it.rotationIndex == rotation }
            if (rows.size != availablePlayerIds.size) errors += "Rotation ${rotation + 1} is incomplete."
            if (rows.map { it.playerId }.toSet() != availablePlayerIds || rows.map { it.playerId }.distinct().size != rows.size) {
                errors += "Rotation ${rotation + 1} must represent every playing player once."
            }
            val field = rows.filter { it.playingState == PlayingState.ON_FIELD }
            val bench = rows.filter { it.playingState == PlayingState.BENCH }
            if (field.size != FieldPositions.size) errors += "Rotation ${rotation + 1} must have seven players on the field."
            if (bench.size != availablePlayerIds.size - FieldPositions.size) errors += "Rotation ${rotation + 1} has the wrong bench count."
            if (field.any { it.position == null } || bench.any { it.position != null } || field.mapNotNull { it.position }.toSet() != FieldPositions.toSet()) {
                errors += "Rotation ${rotation + 1} must fill every position exactly once."
            }
        }
        return RosterValidation(errors.isEmpty(), errors.distinct())
    }
}

data class FairnessNote(val message: String)

data class LineupAssessment(val notes: List<FairnessNote>) {
    val label: String get() = if (notes.isEmpty()) "Balanced" else "${notes.size} fairness ${if (notes.size == 1) "note" else "notes"}"
}

/** Coaching-friendly advice for a manually adjusted four-rotation roster. */
object ManualLineupAssessment {
    fun assess(assignments: List<RotationAssignment>, names: Map<Long, String>): LineupAssessment {
        val notes = mutableListOf<FairnessNote>()
        assignments.groupBy { it.playerId }.forEach { (id, rows) ->
            val name = names[id] ?: "This player"
            rows.sortedBy { it.rotationIndex }.zipWithNext().forEach { (first, second) ->
                when {
                    first.position == PlayingPosition.GOALKEEPER && second.playingState == PlayingState.BENCH ->
                        notes += FairnessNote("$name goes directly from Goalkeeper to Bench.")
                    first.playingState == PlayingState.BENCH && second.playingState == PlayingState.BENCH ->
                        notes += FairnessNote("$name is on the bench in consecutive rotations.")
                    first.position?.family == PositionFamily.DEF && second.position?.family == PositionFamily.DEF ->
                        notes += FairnessNote("$name plays Defence in consecutive rotations.")
                    first.position?.family == PositionFamily.WING && second.position?.family == PositionFamily.WING ->
                        notes += FairnessNote("$name moves between Wing positions in consecutive rotations.")
                    first.position != null && first.position == second.position ->
                        notes += FairnessNote("$name repeats ${first.position.fullLabel} in consecutive rotations.")
                }
            }
        }
        val benchCounts = assignments.filter { it.playingState == PlayingState.BENCH }.groupingBy { it.playerId }.eachCount()
        if (benchCounts.isNotEmpty() && (benchCounts.values.max() - benchCounts.values.min()) > 1) {
            notes += FairnessNote("Bench time is uneven across this game.")
        }
        return LineupAssessment(notes.distinct())
    }
}

/** Swap only assignment slots: player identity and the other rotations are retained. */
object LineupSwaps {
    fun swap(assignments: List<RotationAssignment>, rotation: Int, firstPlayerId: Long, secondPlayerId: Long): List<RotationAssignment> {
        if (firstPlayerId == secondPlayerId) return assignments
        val first = assignments.singleOrNull { it.rotationIndex == rotation && it.playerId == firstPlayerId } ?: return assignments
        val second = assignments.singleOrNull { it.rotationIndex == rotation && it.playerId == secondPlayerId } ?: return assignments
        if (first.playingState == PlayingState.BENCH && second.playingState == PlayingState.BENCH) return assignments
        return assignments.map { row ->
            when {
                row == first -> row.copy(playingState = second.playingState, position = second.position)
                row == second -> row.copy(playingState = first.playingState, position = first.position)
                else -> row
            }
        }
    }
}
