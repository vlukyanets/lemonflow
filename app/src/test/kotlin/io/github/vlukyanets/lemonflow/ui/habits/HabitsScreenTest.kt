package io.github.vlukyanets.lemonflow.ui.habits

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.testing.FakeHabitRepository
import io.github.vlukyanets.lemonflow.testing.Habits
import io.github.vlukyanets.lemonflow.ui.TALL_PHONE
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TALL_PHONE)
class HabitsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = FakeHabitRepository(
        listOf(
            Habits.reading(),
            Habits.water(),
            Habits.meditation(),
            Habits.sugar(),
            Habits.journaling()
        )
    )
    private var opened: String? = "none"
    private var archiveOpened = false

    private fun open() {
        val viewModel = HabitsViewModel(repo)
        compose.setContent {
            LemonFlowTheme {
                HabitsScreen(
                    viewModel = viewModel,
                    openEditor = { opened = it },
                    openArchive = { archiveOpened = true }
                )
            }
        }
    }

    @Test
    fun listShowsTypeAndGoalLines() {
        open()

        compose.onNodeWithText("Check · Target 30 min · minimum 1 page").assertIsDisplayed()
        compose.onNodeWithText(
            "Count · Target 2500 ml · minimum 500 ml · step 250 ml"
        ).assertIsDisplayed()
        compose.onNodeWithText("Timer · Target 10 min · minimum 1 min").assertIsDisplayed()
        compose.onNodeWithText("Avoid · Limit 0 · hard day 1").assertIsDisplayed()
    }

    @Test
    fun coreHabitsCarryTheCoreIcon() {
        open()
        compose.onNodeWithText("Reading").assertIsDisplayed()
        // Reading and Water are both core habits.
        compose.onAllNodesWithContentDescription("Core habit", useUnmergedTree = true)
            .assertCountEquals(2)
    }

    @Test
    fun tappingHabitOpensItsEditorAndAddOpensNew() {
        open()
        compose.onNodeWithText("Water").performClick()
        assertEquals("water", opened)

        compose.onNodeWithText("Add habit").performClick()
        assertEquals(null, opened)
    }

    @Test
    fun archiveEntryShowsCountAndOpensArchive() {
        open()
        compose.onNodeWithText("1").assertIsDisplayed()
        compose.onNodeWithText("Archive").performClick()
        assertTrue(archiveOpened)
    }

    @Test
    fun listIsAccessible() {
        open()
        compose.assertAllClickablesAccessible()
    }
}
