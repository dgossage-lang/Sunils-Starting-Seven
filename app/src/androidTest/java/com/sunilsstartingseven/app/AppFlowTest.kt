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
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun playerLifecycleDraftAndActivityRecreation() {
        awaitText("Home")
        compose.onNodeWithText("Team", useUnmergedTree = true).performClick()
        awaitText("Your team")
        compose.onAllNodesWithText("Add Player").onFirst().performClick()
        compose.onNodeWithText("First name").performTextInput("Mason")
        compose.onNodeWithText("Last initial").performTextInput("g")
        compose.onNodeWithText("Add Player").performScrollTo().performClick()
        awaitText("Mason G.")
        compose.onNodeWithContentDescription("Edit Mason G.").performClick()
        compose.onNodeWithText("First name").performTextReplacement("Mason-Junior")
        compose.onNodeWithText("Save Player").performScrollTo().performClick()
        awaitText("Mason-Junior G.")
        compose.onNodeWithText("Deactivate").performClick()
        awaitText("Inactive (1)")
        compose.onNodeWithText("Inactive (1)").performClick()
        compose.onNodeWithText("Mason-Junior G.").assertExists()
        compose.onNodeWithText("Reactivate").performClick()
        awaitText("Active (1)")
        compose.onNodeWithText("Active (1)").performClick()
        compose.onNodeWithText("Mason-Junior G.").assertExists()
        listOf("Lucas" to "T", "Aiden" to "P", "Tyler" to "S", "Caleb" to "R", "Dylan" to "M", "Noah" to "W").forEachIndexed { index, player ->
            addPlayer(player.first, player.second, index + 2)
        }
        compose.onNodeWithText("Games", useUnmergedTree = true).performClick()
        compose.onNodeWithText("New Game").performClick()
        compose.onNodeWithText("Round number").performTextInput("7")
        compose.onNodeWithText("Notes (optional)").performTextInput("Bring water")
        compose.onNodeWithText("Save Draft").performScrollTo().performClick()
        awaitText("Round 7")
        compose.onNodeWithText("Draft").assertExists()
        compose.onNodeWithContentDescription("Open Round 7").performClick()
        awaitText("Attendance")
        awaitText("Mason-Junior G.")
        compose.onNodeWithContentDescription("Mark Mason-Junior G. Away").performClick()
        awaitText("Away")
        compose.onNodeWithText("Lineup").performClick()
        awaitText("Lineup not generated yet")
        compose.onNodeWithContentDescription("Rotation 1").assertExists()
        compose.onNodeWithContentDescription("Rotation 2").assertExists()
        compose.onNodeWithContentDescription("Rotation 3").assertExists()
        compose.onNodeWithContentDescription("Rotation 4").assertExists()
        compose.onNodeWithContentDescription("Back").performClick()
        awaitText("Attendance")
        compose.activityRule.scenario.recreate()
        awaitText("Attendance")
        awaitText("Away")
        compose.onNodeWithContentDescription("Back").performClick()
        awaitText("Round 7")
        compose.onNodeWithText("Bring water").assertExists()
    }

    private fun addPlayer(first: String, last: String, activeCount: Int) {
        compose.onAllNodesWithText("Add Player").onFirst().performClick()
        compose.onNodeWithText("First name").performTextInput(first)
        compose.onNodeWithText("Last initial").performTextInput(last)
        compose.onNodeWithText("Add Player").performScrollTo().performClick()
        awaitText("Active ($activeCount)")
    }
}
