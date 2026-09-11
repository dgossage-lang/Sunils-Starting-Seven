package com.sunilsstartingseven.app.domain.rotation

import com.sunilsstartingseven.app.domain.model.*
import kotlin.math.abs

const val ROTATION_COUNT = 4

val FieldPositions = listOf(
    PlayingPosition.GOALKEEPER,
    PlayingPosition.DEFENDER_1,
    PlayingPosition.DEFENDER_2,
    PlayingPosition.LEFT_WING,
    PlayingPosition.MID,
    PlayingPosition.RIGHT_WING,
    PlayingPosition.STRIKER
)

data class RotationPlayer(val id: Long, val displayName: String = "")
data class RotationAssignment(val playerId: Long, val rotationIndex: Int, val playingState: PlayingState, val position: PlayingPosition?)
data class HistoricalAssignment(val playerId: Long, val playingState: PlayingState, val position: PlayingPosition?)
data class RotationRequest(
    val availablePlayers: List<RotationPlayer>,
    val awayPlayerIds: Set<Long> = emptySet(),
    val history: List<HistoricalAssignment> = emptyList(),
    val seed: Int = 0
)
data class RotationResult(val assignments: List<RotationAssignment>, val diagnostics: List<String> = emptyList())

data class RosterValidation(
    val valid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList()
)

private data class PlayerStats(
    val active: Int = 0,
    val bench: Int = 0,
    val exact: Map<PlayingPosition, Int> = emptyMap(),
    val family: Map<PositionFamily, Int> = emptyMap()
) {
    fun exactCount(position: PlayingPosition) = exact[position] ?: 0
    fun familyCount(family: PositionFamily) = this.family[family] ?: 0
}

class RotationValidator {
    fun validate(assignments: List<RotationAssignment>, availablePlayerIds: Set<Long>, awayPlayerIds: Set<Long>): RosterValidation {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        if (availablePlayerIds.size < 7) errors += "At least seven available players are required."
        if (assignments.any { it.playerId in awayPlayerIds }) errors += "Away players cannot receive assignments."
        val expectedPerRotation = availablePlayerIds.size
        for (rotation in 0 until ROTATION_COUNT) {
            val rows = assignments.filter { it.rotationIndex == rotation }
            if (rows.size != expectedPerRotation) errors += "Rotation ${rotation + 1} has ${rows.size} assignments; expected $expectedPerRotation."
            val ids = rows.map { it.playerId }
            if (ids.toSet().size != ids.size) errors += "Rotation ${rotation + 1} has a duplicated player."
            if (ids.toSet() != availablePlayerIds) errors += "Rotation ${rotation + 1} does not represent every available player exactly once."
            val onField = rows.filter { it.playingState == PlayingState.ON_FIELD }
            val bench = rows.filter { it.playingState == PlayingState.BENCH }
            if (onField.size != FieldPositions.size) errors += "Rotation ${rotation + 1} has ${onField.size} on-field players."
            if (bench.size != availablePlayerIds.size - FieldPositions.size) errors += "Rotation ${rotation + 1} has ${bench.size} bench players."
            val positions = onField.mapNotNull { it.position }
            if (positions.toSet() != FieldPositions.toSet()) errors += "Rotation ${rotation + 1} does not fill each exact position once."
            if (onField.any { it.position == null }) errors += "Rotation ${rotation + 1} has an on-field row without a position."
            if (bench.any { it.position != null }) errors += "Rotation ${rotation + 1} has a bench row with a position."
        }
        val byPlayer = assignments.groupBy { it.playerId }
        byPlayer.forEach { (playerId, rows) ->
            rows.sortedBy { it.rotationIndex }.zipWithNext().forEach { (a, b) ->
                if (a.position == PlayingPosition.GOALKEEPER && b.playingState == PlayingState.BENCH) errors += "Player $playerId moves directly from goalkeeper to bench."
                if (a.playingState == PlayingState.BENCH && b.playingState == PlayingState.BENCH && availablePlayerIds.size == 10) errors += "Player $playerId has consecutive bench periods."
                if (a.position?.family == PositionFamily.DEF && b.position?.family == PositionFamily.DEF) warnings += "Player $playerId has consecutive DEF family assignments."
                if (a.position?.family == PositionFamily.WING && b.position?.family == PositionFamily.WING) warnings += "Player $playerId has consecutive WING family assignments."
            }
        }
        return RosterValidation(errors.isEmpty(), errors, warnings.distinct())
    }
}

class FairRotationEngine(private val validator: RotationValidator = RotationValidator()) {
    fun generate(request: RotationRequest): RotationResult {
        val players = request.availablePlayers.distinctBy { it.id }.sortedBy { seededOrder(it.id, request.seed) }
        require(players.size >= FieldPositions.size) { "At least seven available players are required." }
        val availableIds = players.map { it.id }.toSet()
        require(request.awayPlayerIds.none { it in availableIds }) { "Away players cannot be available." }
        val history = buildStats(request.history)
        val assignments = mutableListOf<RotationAssignment>()
        val inGameExact = players.associate { it.id to mutableMapOf<PlayingPosition, Int>() }
        val inGameFamily = players.associate { it.id to mutableMapOf<PositionFamily, Int>() }
        val inGameBench = players.associate { it.id to 0 }.toMutableMap()
        val previousPosition = mutableMapOf<Long, PlayingPosition?>()
        val previousBench = mutableSetOf<Long>()
        val diagnostics = mutableListOf<String>()

        for (rotation in 0 until ROTATION_COUNT) {
            val benchIds = chooseBench(players, rotation, request.seed, history, inGameBench, previousPosition, previousBench)
            val active = players.filter { it.id !in benchIds }
            val placed = choosePositions(active, rotation, request.seed, history, inGameExact, inGameFamily, previousPosition)
            placed.forEach { (position, player) ->
                assignments += RotationAssignment(player.id, rotation, PlayingState.ON_FIELD, position)
                inGameExact[player.id]!![position] = (inGameExact[player.id]!![position] ?: 0) + 1
                inGameFamily[player.id]!![position.family] = (inGameFamily[player.id]!![position.family] ?: 0) + 1
            }
            benchIds.forEach { playerId ->
                assignments += RotationAssignment(playerId, rotation, PlayingState.BENCH, null)
                inGameBench[playerId] = (inGameBench[playerId] ?: 0) + 1
            }
            previousBench.clear(); previousBench.addAll(benchIds)
            players.forEach { player -> previousPosition[player.id] = placed.entries.firstOrNull { it.value.id == player.id }?.key }
        }
        val validation = validator.validate(assignments, availableIds, request.awayPlayerIds)
        if (!validation.valid) error(validation.errors.joinToString("; "))
        diagnostics += validation.warnings
        return RotationResult(assignments.sortedWith(compareBy({ it.rotationIndex }, { it.position?.ordinal ?: 99 }, { it.playerId })), diagnostics)
    }

    private fun chooseBench(
        players: List<RotationPlayer>,
        rotation: Int,
        seed: Int,
        history: Map<Long, PlayerStats>,
        inGameBench: Map<Long, Int>,
        previousPosition: Map<Long, PlayingPosition?>,
        previousBench: Set<Long>
    ): Set<Long> {
        val benchCount = players.size - FieldPositions.size
        if (benchCount <= 0) return emptySet()
        val combos = combinations(players.map { it.id }, benchCount)
        val feasible = combos.filter { combo ->
            combo.none { previousPosition[it] == PlayingPosition.GOALKEEPER } &&
                (players.size != 10 || combo.none { it in previousBench })
        }.ifEmpty { combos.filter { combo -> combo.none { previousPosition[it] == PlayingPosition.GOALKEEPER } } }.ifEmpty { combos }
        return feasible.minWith(compareBy<Set<Long>> { combo ->
            combo.sumOf { playerId -> benchScore(playerId, history[playerId] ?: PlayerStats(), inGameBench[playerId] ?: 0) } + benchSpreadPenalty(players.map { it.id }, combo, inGameBench)
        }.thenBy { combo -> combo.sumOf { seededOrder(it, seed + rotation * 131) } }).toSet()
    }

    private fun benchScore(playerId: Long, stats: PlayerStats, gameBench: Int): Int {
        val total = stats.active + stats.bench
        val benchRatePenalty = if (total == 0) 0 else ((stats.bench * 100) / total)
        return gameBench * 220 + benchRatePenalty - stats.active * 4 + stats.bench * 3 + (playerId % 7).toInt()
    }

    private fun benchSpreadPenalty(playerIds: List<Long>, combo: Set<Long>, current: Map<Long, Int>): Int {
        val next = playerIds.map { (current[it] ?: 0) + if (it in combo) 1 else 0 }
        return (next.max() - next.min()) * 180
    }

    private fun choosePositions(
        players: List<RotationPlayer>,
        rotation: Int,
        seed: Int,
        history: Map<Long, PlayerStats>,
        inGameExact: Map<Long, Map<PlayingPosition, Int>>,
        inGameFamily: Map<Long, Map<PositionFamily, Int>>,
        previousPosition: Map<Long, PlayingPosition?>
    ): Map<PlayingPosition, RotationPlayer> {
        var bestScore = Int.MAX_VALUE
        var best = emptyMap<PlayingPosition, RotationPlayer>()
        permutations(players).forEach { permutation ->
            var score = 0
            val map = mutableMapOf<PlayingPosition, RotationPlayer>()
            FieldPositions.forEachIndexed { index, position ->
                val player = permutation[index]
                map[position] = player
                score += positionScore(player.id, position, rotation, seed, history[player.id] ?: PlayerStats(), inGameExact[player.id].orEmpty(), inGameFamily[player.id].orEmpty(), previousPosition[player.id])
            }
            if (score < bestScore) { bestScore = score; best = map }
        }
        return best
    }

    private fun positionScore(
        playerId: Long,
        position: PlayingPosition,
        rotation: Int,
        seed: Int,
        stats: PlayerStats,
        gameExact: Map<PlayingPosition, Int>,
        gameFamily: Map<PositionFamily, Int>,
        previous: PlayingPosition?
    ): Int {
        var score = 0
        score += stats.exactCount(position) * 140
        score += stats.familyCount(position.family) * 32
        score += (gameExact[position] ?: 0) * 360
        score += (gameFamily[position.family] ?: 0) * 170
        if (previous == position) score += 260
        if (previous?.family == position.family) score += when (position.family) {
            PositionFamily.DEF, PositionFamily.WING -> 420
            else -> 160
        }
        score += abs(position.ordinal - ((playerId.toInt() + rotation + seed) % FieldPositions.size))
        score += seededOrder(playerId + position.ordinal * 17L, seed + rotation * 31) % 11
        return score
    }

    private fun buildStats(history: List<HistoricalAssignment>): Map<Long, PlayerStats> {
        return history.groupBy { it.playerId }.mapValues { (_, rows) ->
            val exact = rows.mapNotNull { it.position }.groupingBy { it }.eachCount()
            val family = rows.mapNotNull { it.position?.family }.groupingBy { it }.eachCount()
            PlayerStats(
                active = rows.count { it.playingState == PlayingState.ON_FIELD },
                bench = rows.count { it.playingState == PlayingState.BENCH },
                exact = exact,
                family = family
            )
        }
    }

    private fun seededOrder(value: Long, seed: Int): Int {
        var x = value xor (seed.toLong() * -7046029254386353131L)
        x = (x xor (x ushr 33)) * -49064778989728563L
        x = (x xor (x ushr 33)) * -4265267296055464877L
        return ((x xor (x ushr 33)) and 0x7fffffff).toInt()
    }

    private fun <T> combinations(values: List<T>, size: Int): List<Set<T>> {
        val out = mutableListOf<Set<T>>()
        fun visit(start: Int, chosen: MutableList<T>) {
            if (chosen.size == size) { out += chosen.toSet(); return }
            for (i in start until values.size) { chosen += values[i]; visit(i + 1, chosen); chosen.removeAt(chosen.lastIndex) }
        }
        visit(0, mutableListOf())
        return out
    }

    private fun <T> permutations(values: List<T>): Sequence<List<T>> = sequence {
        val a = values.toMutableList()
        fun generate(k: Int): Sequence<List<T>> = sequence {
            if (k == a.size) yield(a.toList())
            else for (i in k until a.size) {
                java.util.Collections.swap(a, k, i)
                yieldAll(generate(k + 1))
                java.util.Collections.swap(a, k, i)
            }
        }
        yieldAll(generate(0))
    }
}
