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
        compose.onNodeWithText("Games", useUnmergedTree = true).performClick()
        awaitText("New Game")
        compose.onNodeWithText("New Game").performClick()
        compose.onNodeWithText("Round number").performTextInput("1")
        compose.onNodeWithText("Notes (optional)").performTextInput("First fair rotation")
        compose.onNodeWithText("Save Draft").performScrollTo().performClick()
        compose.waitForIdle()
        awaitContentDescription("Open Round 1")
        compose.onNodeWithContentDescription("Open Round 1").performClick()
        awaitText("Attendance")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Artie R."))
        awaitText("Artie R.")
        compose.onNodeWithContentDescription("Mark Artie R. Away").performClick()
        awaitText("Away")
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Open Lineup"))
        compose.onNodeWithText("Open Lineup").performClick()
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
    }
}
