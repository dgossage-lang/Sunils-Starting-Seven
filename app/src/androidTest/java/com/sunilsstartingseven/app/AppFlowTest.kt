package com.sunilsstartingseven.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun awaitText(text: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = false).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun seededRosterGeneratesAndPersistsPlayedLineup() {
        awaitText("Home")
        awaitText("10 active players")
        compose.onNodeWithText("Games", useUnmergedTree = true).performClick()
        awaitText("New Game")
        compose.onNodeWithText("New Game").performClick()
        compose.onNodeWithText("Round number").performTextInput("1")
        compose.onNodeWithText("Notes (optional)").performTextInput("First fair rotation")
        compose.onNodeWithText("Save Draft").performScrollTo().performClick()
        awaitText("Round 1")
        compose.onNodeWithContentDescription("Open Round 1").performClick()
        awaitText("Attendance")
        awaitText("Nick")
        compose.onNodeWithContentDescription("Mark Nick Away").performClick()
        awaitText("Away")
        compose.onNodeWithText("Lineup").performClick()
        awaitText("Generate Fair Rotations")
        compose.onNodeWithText("Generate Fair Rotations").performClick()
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
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Attendance")
        compose.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        awaitText("Games")
        compose.onNodeWithText("Fairness", useUnmergedTree = true).performClick()
        awaitText("Team Balance")
        awaitText("Player balance")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Nick"))
        awaitText("Nick")
        compose.onNodeWithText("Nick").performClick()
        awaitText("Player history")
        awaitText("Participation")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Game History"))
        awaitText("Game History")
        awaitText("Away")
    }
}
