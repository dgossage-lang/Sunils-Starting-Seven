package com.sunilsstartingseven.app

import com.sunilsstartingseven.app.data.local.Assignment
import com.sunilsstartingseven.app.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    @Test fun blankFirstNameRejected() { assertFalse(PlayerInput.validate("", "G").isValid) }
    @Test fun whitespaceFirstNameRejected() { assertFalse(PlayerInput.validate(" \t\n", "G").isValid) }
    @Test fun validNameAccepted() { assertTrue(PlayerInput.validate(" Mason ", "g").isValid) }
    @Test fun lowercaseInitialNormalized() { assertEquals("G", PlayerInput.normalizeInitial(" g ")) }
    @Test fun oneLetterAccepted() { assertTrue(PlayerInput.validate("Mason", "G").isValid) }
    @Test fun blankInitialRejected() { assertFalse(PlayerInput.validate("Mason", " ").isValid) }
    @Test fun multipleLettersRejected() { assertFalse(PlayerInput.validate("Mason", "GH").isValid) }
    @Test fun numberRejected() { assertFalse(PlayerInput.validate("Mason", "7").isValid) }
    @Test fun punctuationRejected() { assertFalse(PlayerInput.validate("Mason", ".").isValid) }
    @Test fun accentedInitialRemainsOneLetter() {
        assertTrue(PlayerInput.validate("Émile", "é").isValid)
        assertEquals("É", PlayerInput.normalizeInitial("é"))
        assertEquals(1, PlayerInput.normalizeInitial("ß").length)
    }
    @Test fun allPositionFamiliesMatch() {
        val expected = mapOf(
            PlayingPosition.GOALKEEPER to PositionFamily.GK,
            PlayingPosition.DEFENDER_1 to PositionFamily.DEF,
            PlayingPosition.DEFENDER_2 to PositionFamily.DEF,
            PlayingPosition.LEFT_WING to PositionFamily.WING,
            PlayingPosition.RIGHT_WING to PositionFamily.WING,
            PlayingPosition.MID to PositionFamily.MID,
            PlayingPosition.STRIKER to PositionFamily.ATT
        )
        assertEquals(PlayingPosition.entries.toSet(), expected.keys)
        expected.forEach { (position, family) -> assertEquals(family, position.family) }
    }
    @Test fun benchHasNoPosition() { assertNull(Assignment(1, 0, 1, PlayingState.BENCH, null).position) }
    @Test(expected = IllegalArgumentException::class)
    fun onFieldRequiresPosition() { Assignment(1, 0, 1, PlayingState.ON_FIELD, null) }
    @Test(expected = IllegalArgumentException::class)
    fun benchRejectsPosition() { Assignment(1, 0, 1, PlayingState.BENCH, PlayingPosition.GOALKEEPER) }
    @Test(expected = IllegalArgumentException::class)
    fun negativeRotationRejected() { Assignment(1, -1, 1, PlayingState.BENCH, null) }
}
