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
        compose.onNodeWithText("Add Player").performScrollTo().performClick()
        compose.onNodeWithText("Enter a first name.").assertExists()
        compose.onNodeWithText("First name").performTextInput("Mason")
        compose.onNodeWithText("Last initial").performTextInput("gg")
        compose.onNodeWithText("Add Player").performScrollTo().performClick()
        compose.onNodeWithText("Enter one letter for the last initial.").assertExists()
        compose.onNodeWithText("Last initial").performTextReplacement("g")
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
        compose.onNodeWithText("Games", useUnmergedTree = true).performClick()
        compose.onNodeWithText("New Game").performClick()
        compose.onNodeWithText("Round number").performTextInput("7")
        compose.onNodeWithText("Notes (optional)").performTextInput("Bring water")
        compose.onNodeWithText("Save Draft").performScrollTo().performClick()
        awaitText("Round 7")
        compose.onNodeWithText("Draft").assertExists()
        compose.activityRule.scenario.recreate()
        awaitText("Round 7")
        compose.onNodeWithText("Bring water").assertExists()
        compose.onNodeWithText("Team", useUnmergedTree = true).performClick()
        awaitText("Mason-Junior G.")
    }
}
