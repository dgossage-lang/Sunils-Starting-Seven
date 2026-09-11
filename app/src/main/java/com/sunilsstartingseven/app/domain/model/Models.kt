package com.sunilsstartingseven.app.domain.model

enum class GameStatus(val label: String) { DRAFT("Draft"), READY("Ready"), PLAYED("Played"), CANCELLED("Cancelled") }
enum class AttendanceStatus(val label: String) { AVAILABLE("Playing"), AWAY("Away") }
enum class PositionFamily { GK, DEF, WING, MID, ATT }
enum class PlayingPosition(val family: PositionFamily, val shortLabel: String, val fullLabel: String, val x: Float, val y: Float) {
    STRIKER(PositionFamily.ATT, "ST", "Striker", .50f, .19f),
    LEFT_WING(PositionFamily.WING, "LW", "Left wing", .20f, .38f),
    MID(PositionFamily.MID, "MID", "Midfielder", .50f, .43f),
    RIGHT_WING(PositionFamily.WING, "RW", "Right wing", .80f, .38f),
    DEFENDER_1(PositionFamily.DEF, "D1", "Defender 1", .34f, .67f),
    DEFENDER_2(PositionFamily.DEF, "D2", "Defender 2", .66f, .67f),
    GOALKEEPER(PositionFamily.GK, "GK", "Goalkeeper", .50f, .84f)
}
enum class PlayingState { ON_FIELD, BENCH }

data class RotationPeriod(val index: Int, val title: String, val subtitle: String)

val RotationPeriods = listOf(
    RotationPeriod(0, "Rotation 1", "First Half - Start"),
    RotationPeriod(1, "Rotation 2", "First Half - Finish"),
    RotationPeriod(2, "Rotation 3", "Second Half - Start"),
    RotationPeriod(3, "Rotation 4", "Second Half - Finish")
)

data class AttendanceSummary(val available: Int, val away: Int) {
    val onFieldCapacity: Int = 7
    val expectedBench: Int get() = maxOf(available - onFieldCapacity, 0)
    val neededForFullSeven: Int get() = maxOf(onFieldCapacity - available, 0)
    val readyForLineup: Boolean get() = available >= onFieldCapacity
}

fun attendanceSummary(statuses: Iterable<AttendanceStatus>): AttendanceSummary =
    AttendanceSummary(
        available = statuses.count { it == AttendanceStatus.AVAILABLE },
        away = statuses.count { it == AttendanceStatus.AWAY }
    )

data class PlayerValidation(val firstNameError: String? = null, val lastInitialError: String? = null) {
    val isValid: Boolean get() = firstNameError == null && lastInitialError == null
}

object PlayerInput {
    fun normalizeInitial(value: String): String = value.trim().map { it.uppercaseChar() }.joinToString("")
    fun validate(firstName: String, lastInitial: String): PlayerValidation {
        val initial = lastInitial.trim()
        return PlayerValidation(
            firstNameError = if (firstName.isBlank()) "Enter a first name." else null,
            lastInitialError = if (initial.length != 1 || !initial[0].isLetter()) "Enter one letter for the last initial." else null
        )
    }
}
