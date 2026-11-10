package io.github.vlukyanets.lemonflow.ui.habits

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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
class ArchiveScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = FakeHabitRepository(listOf(Habits.reading(), Habits.journaling()))

    private fun open() {
        val viewModel = ArchiveViewModel(repo)
        compose.setContent { LemonFlowTheme { ArchiveScreen(viewModel, onBack = {}) } }
    }

    @Test
    fun restoreBringsHabitBack() {
        open()
        compose.onNodeWithText("Journaling").assertIsDisplayed()

        compose.onNodeWithContentDescription("Restore Journaling").performClick()

        compose.waitForIdle()
        assertTrue(repo.habits.value.all { it.archivedAt == null })
    }

    @Test
    fun deletePermanentlyNeedsConfirmation() {
        open()

        compose.onNodeWithContentDescription("Delete Journaling permanently").performClick()
        compose.onNodeWithText(
            "Its history will be removed and cannot be restored."
        ).assertIsDisplayed()
        assertEquals(2, repo.habits.value.size)

        compose.onNodeWithText("Delete").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Reading"), repo.habits.value.map { it.name })
    }

    @Test
    fun archiveIsAccessible() {
        open()
        compose.assertAllClickablesAccessible()
    }
}
