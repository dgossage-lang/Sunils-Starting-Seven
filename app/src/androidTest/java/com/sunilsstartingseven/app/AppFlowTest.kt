package com.sunilsstartingseven.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitText(text: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() }
    }


    private fun awaitContentDescription(description: String) {
        compose.waitUntil(15_000) { compose.onAllNodesWithContentDescription(description, substring = false).fetchSemanticsNodes().isNotEmpty() }
    }


    private fun seedRoster() {
        val players = listOf(
            "Nick" to "N",
            "Cameron" to "C",
            "Axel" to "A",
            "Hughie" to "H",
            "Jero" to "J",
            "Samrith" to "S",
            "Macson" to "M",
            "Artie" to "R",
            "Roman" to "O",
            "Nico" to "I"
        )
        val app = compose.activity.application as SunilsStartingSevenApplication
        runBlocking {
            players.forEach { (first, initial) -> app.container.players.save(null, first, initial) }
        }
    }
    @Test fun createdRosterGeneratesAndPersistsPlayedLineup() {
        awaitText("Home")
        awaitText("Add Players")
        seedRoster()
        awaitText("Home")
        awaitText("10 active players")
        compose.onNodeWithContentDescription("Settings").performClick()
        awaitText("Data & Backup")
        awaitText("Create Backup")
        awaitText("Restore Backup")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Home")
        compose.onNodeWithText("Team", useUnmergedTree = true).performClick()
        awaitContentDescription("Edit Axel A.")
        compose.onNodeWithContentDescription("Edit Axel A.").performClick()
        awaitText("Carry fairness across seasons")
        compose.onNodeWithContentDescription("Carry fairness across seasons").performScrollTo().performClick()
        compose.onNodeWithText("Save Player").performScrollTo().performClick()
        compose.onNodeWithText("Games", useUnmergedTree = true).performClick()
        awaitText("New Game")
        compose.onNodeWithText("Seasons").performClick()
        awaitText("New Season")
        compose.onNodeWithText("New Season").performClick()
        compose.onNodeWithText("Season name").performTextInput("2026 Season")
        compose.onNodeWithText("Save").performClick()
        awaitText("2026 Season")
        compose.onNodeWithContentDescription("Back").performClick()
        awaitText("New Game")
        compose.onNodeWithText("New Game").performClick()
        compose.onNodeWithText("Round number").performTextInput("1")
        compose.onNodeWithText("Notes (optional)").performTextInput("First fair rotation")
        compose.onNodeWithText("Save Draft").performScrollTo().performClick()
        compose.waitForIdle()
        awaitContentDescription("Open Round 1")
        compose.onNodeWithContentDescription("Open Round 1").performClick()
        awaitText("Attendance")
        compose.onNodeWithText("Pitch Layout · 7 positions").performScrollTo().performClick()
        awaitContentDescription("Remove Striker")
        compose.onNodeWithContentDescription("Remove Striker").performClick()
        awaitText("6 on field · 4 on bench · 10 playing")
        compose.onNodeWithText("Add Custom Position").performScrollTo().performClick()
        awaitText("Custom position")
        compose.onNodeWithText("Position name").performTextClearance()
        compose.onNodeWithText("Position name").performTextInput("Sweeper")
        compose.onNodeWithText("Short label").performTextClearance()
        compose.onNodeWithText("Short label").performTextInput("SW")
        compose.onNodeWithText("Place on Pitch").performClick()
        val moving = compose.onNodeWithContentDescription("Move Sweeper").performScrollTo()
        val beforeMove = moving.getUnclippedBoundsInRoot()
        moving.performTouchInput { swipe(center, center + androidx.compose.ui.geometry.Offset(50f, -90f), durationMillis = 500) }
        compose.waitForIdle()
        val afterMove = compose.onNodeWithContentDescription("Edit Sweeper").getUnclippedBoundsInRoot()
        org.junit.Assert.assertTrue(afterMove.left.value > beforeMove.left.value + 5)
        org.junit.Assert.assertTrue(afterMove.top.value < beforeMove.top.value - 5)
        awaitText("SW · Sweeper · On field")
        compose.onNodeWithText("SW · Sweeper · On field").performScrollTo().performClick()
        compose.onNode(isToggleable()).performClick()
        compose.onNodeWithText("Use Position").performClick()
        awaitText("SW · Sweeper · Available option")
        compose.onNodeWithText("SW · Sweeper · Available option").performScrollTo().performClick()
        compose.onNode(isToggleable()).assertIsOff()
        compose.onNodeWithText("Delete Position").performScrollTo().performClick()
        compose.onNodeWithText("SW · Sweeper · Available option").assertDoesNotExist()
        compose.onNodeWithText("Save Layout").performScrollTo().performClick()
        awaitText("Pitch Layout · 6 positions")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Artie R."))
        awaitText("Artie R.")
        compose.onNodeWithContentDescription("Mark Artie R. Away").performClick()
        awaitText("Away")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Open Lineup"))
        compose.onNodeWithText("Open Lineup").performClick()
        awaitText("Generate Fair Rotations")
        compose.onNodeWithText("Generate Fair Rotations").performScrollTo().performClick()
        awaitText("Fair rotation generated from attendance and playing history.")
        compose.onNodeWithText("Edit Lineup").performScrollTo().performClick()
        awaitText("Editing lineup")
        compose.onNodeWithText("Cancel").performScrollTo().performClick()
        awaitText("Edit Lineup")
        compose.onNodeWithContentDescription("Rotation 2").performClick()
        awaitText("Bench")
        compose.onNodeWithText("Mark as Played").performScrollTo().performClick()
        awaitText("Mark Round 1 as played?")
        compose.onNodeWithText("Mark Played").performClick()
        awaitText("Played games are locked as historical records.")
        compose.activityRule.scenario.recreate()
        awaitText("Played games are locked as historical records.")
        val app = compose.activity.application as SunilsStartingSevenApplication
        runBlocking {
            val game = app.container.database.gameDao().all().single()
            org.junit.Assert.assertEquals(6, game.fieldPositions.size)
            org.junit.Assert.assertFalse(game.positionOptions.any { it.fullLabel == "Sweeper" && it.shortLabel == "SW" })
            org.junit.Assert.assertTrue(app.container.database.playerDao().all().single { it.firstName == "Axel" }.carryFairnessAcrossSeasons)
            org.junit.Assert.assertEquals("2026 Season", app.container.database.seasonDao().get(game.seasonId!!)!!.name)
            val assignments = app.container.database.rosterDao().getAssignments(game.id)
            (0..3).forEach { rotation ->
                org.junit.Assert.assertEquals(6, assignments.count { it.rotationIndex == rotation && it.playingState == com.sunilsstartingseven.app.domain.model.PlayingState.ON_FIELD })
                org.junit.Assert.assertEquals(3, assignments.count { it.rotationIndex == rotation && it.playingState == com.sunilsstartingseven.app.domain.model.PlayingState.BENCH })
            }
        }
    }
}
