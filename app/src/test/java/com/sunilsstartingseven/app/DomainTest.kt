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
    @Test fun positionLabelsMatchSevenAsideLayout() {
        val labels = PlayingPosition.entries.associateWith { it.shortLabel }
        assertEquals("ST", labels[PlayingPosition.STRIKER])
        assertEquals("LW", labels[PlayingPosition.LEFT_WING])
        assertEquals("MID", labels[PlayingPosition.MID])
        assertEquals("RW", labels[PlayingPosition.RIGHT_WING])
        assertEquals("D1", labels[PlayingPosition.DEFENDER_1])
        assertEquals("D2", labels[PlayingPosition.DEFENDER_2])
        assertEquals("GK", labels[PlayingPosition.GOALKEEPER])
    }
    @Test fun positionAnchorsStayInsidePitch() {
        assertEquals(7, PlayingPosition.entries.size)
        PlayingPosition.entries.forEach {
            assertTrue("${it.name} x", it.x in 0f..1f)
            assertTrue("${it.name} y", it.y in 0f..1f)
        }
    }
    @Test fun attendanceSummaryCountsBenchAndShortage() {
        assertEquals(AttendanceSummary(8, 2), attendanceSummary(List(8) { AttendanceStatus.AVAILABLE } + List(2) { AttendanceStatus.AWAY }))
        assertEquals(1, AttendanceSummary(8, 0).expectedBench)
        assertEquals(0, AttendanceSummary(7, 1).expectedBench)
        assertTrue(AttendanceSummary(7, 0).readyForLineup)
        assertFalse(AttendanceSummary(6, 1).readyForLineup)
        assertEquals(1, AttendanceSummary(6, 0).neededForFullSeven)
    }
    @Test fun benchHasNoPosition() { assertNull(Assignment(1, 0, 1, PlayingState.BENCH, null).position) }
    @Test(expected = IllegalArgumentException::class)
    fun onFieldRequiresPosition() { Assignment(1, 0, 1, PlayingState.ON_FIELD, null) }
    @Test(expected = IllegalArgumentException::class)
    fun benchRejectsPosition() { Assignment(1, 0, 1, PlayingState.BENCH, PlayingPosition.GOALKEEPER) }
    @Test(expected = IllegalArgumentException::class)
    fun negativeRotationRejected() { Assignment(1, -1, 1, PlayingState.BENCH, null) }
}
