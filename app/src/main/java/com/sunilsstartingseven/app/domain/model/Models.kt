package com.sunilsstartingseven.app.domain.model

enum class GameStatus(val label: String) { DRAFT("Draft"), READY("Ready"), PLAYED("Played"), CANCELLED("Cancelled") }
enum class AttendanceStatus { AVAILABLE, AWAY }
enum class PositionFamily { GK, DEF, WING, MID, ATT }
enum class PlayingPosition(val family: PositionFamily) {
    GOALKEEPER(PositionFamily.GK), DEFENDER_1(PositionFamily.DEF), DEFENDER_2(PositionFamily.DEF),
    LEFT_WING(PositionFamily.WING), MID(PositionFamily.MID), RIGHT_WING(PositionFamily.WING), STRIKER(PositionFamily.ATT)
}
enum class PlayingState { ON_FIELD, BENCH }

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
