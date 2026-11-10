package io.github.vlukyanets.lemonflow.ui.editor

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.domain.model.HabitType
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
class HabitEditorScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val repo = FakeHabitRepository(listOf(Habits.reading()))
    private var closed = false

    private fun open(habitId: String? = null) {
        val viewModel = EditorViewModel(SavedStateHandle(mapOf("habitId" to habitId)), repo)
        compose.setContent {
            LemonFlowTheme { HabitEditorScreen(viewModel, onClose = { closed = true }) }
        }
    }

    private fun assertFields(present: List<String>, absent: List<String>) {
        present.forEach { compose.onNodeWithTag(it).assertIsDisplayed() }
        absent.forEach { compose.onAllNodesWithTag(it).assertCountEquals(0) }
    }

    @Test
    fun checkShowsTwoAmountUnitPairsAndNoStep() {
        open()

        assertFields(
            present = listOf(
                Tags.TARGET_AMOUNT,
                Tags.TARGET_UNIT,
                Tags.BASELINE_AMOUNT,
                Tags.BASELINE_UNIT
            ),
            absent = listOf(Tags.STEP, Tags.UNIT)
        )
    }

    @Test
    fun eachTypeShowsItsOwnFields() {
        open()

        compose.onNodeWithText("Count").performClick()
        assertFields(
            present = listOf(Tags.UNIT, Tags.TARGET_AMOUNT, Tags.BASELINE_AMOUNT, Tags.STEP),
            absent = listOf(Tags.TARGET_UNIT, Tags.BASELINE_UNIT)
        )

        compose.onNodeWithText("Timer").performClick()
        assertFields(
            present = listOf(Tags.TARGET_AMOUNT, Tags.BASELINE_AMOUNT, Tags.STEP),
            absent = listOf(Tags.UNIT, Tags.TARGET_UNIT, Tags.BASELINE_UNIT)
        )

        compose.onNodeWithText("Avoid").performClick()
        compose.onNodeWithText("Limit on a good day").assertIsDisplayed()
        compose.onNodeWithText("Limit on a hard day").assertIsDisplayed()
        assertFields(present = listOf(Tags.UNIT), absent = listOf(Tags.STEP))
    }

    @Test
    fun saveStaysDisabledUntilNameTargetAndMinimumAreSet() {
        open()
        compose.onNodeWithTag(Tags.SAVE).assertIsNotEnabled()

        compose.onNodeWithTag(Tags.NAME).performTextInput("Stretching")
        compose.onNodeWithTag(Tags.TARGET_AMOUNT).performTextInput("15")
        compose.onNodeWithTag(Tags.SAVE).assertIsNotEnabled()

        compose.onNodeWithTag(Tags.BASELINE_AMOUNT).performTextInput("2")
        compose.onNodeWithTag(Tags.SAVE).assertIsEnabled().performClick()

        compose.waitForIdle()
        assertTrue(closed)
        assertTrue(repo.habits.value.any { it.name == "Stretching" && it.type == HabitType.CHECK })
    }

    @Test
    fun clearedFieldShowsGentleHint() {
        open()
        compose.onNodeWithTag(Tags.TARGET_AMOUNT).performTextInput("1")
        compose.onNodeWithTag(Tags.TARGET_AMOUNT).performTextReplacement("")

        compose.onNodeWithText("Add an amount for a good day").assertIsDisplayed()
    }

    @Test
    fun countMinimumAboveTargetShowsHintAndBlocksSave() {
        open()
        compose.onNodeWithText("Count").performClick()
        compose.onNodeWithTag(Tags.NAME).performTextInput("Water")
        compose.onNodeWithTag(Tags.TARGET_AMOUNT).performTextInput("2500")
        compose.onNodeWithTag(Tags.BASELINE_AMOUNT).performTextInput("3000")

        compose.onNodeWithText("The minimum should be no more than the target").assertIsDisplayed()
        compose.onNodeWithTag(Tags.SAVE).assertIsNotEnabled()
    }

    @Test
    fun editModeShowsTypeReadOnlyAndConfirmsPermanentDelete() {
        open(habitId = "reading")

        compose.onNodeWithText("The type can't be changed after creation").assertIsDisplayed()
        compose.onAllNodesWithTag(Tags.TYPE_PICKER).assertCountEquals(0)
        compose.onNodeWithText("Archive habit").assertIsDisplayed()

        compose.onNodeWithText("Delete permanently").performClick()
        compose.onNodeWithText("Delete Reading permanently?").assertIsDisplayed()
        compose.onNodeWithText("Delete").performClick()

        compose.waitForIdle()
        assertTrue(closed)
        assertEquals(emptyList<Any>(), repo.habits.value)
    }

    @Test
    fun archiveFromEditorKeepsHabitInArchive() {
        open(habitId = "reading")

        compose.onNodeWithText("Archive habit").performClick()

        compose.waitForIdle()
        assertTrue(closed)
        assertTrue(repo.habits.value.single().archivedAt != null)
    }

    @Test
    fun everyControlIsAccessible() {
        open()
        compose.assertAllClickablesAccessible()
    }

    @Test
    fun editModeAndDialogAreAccessible() {
        open(habitId = "reading")
        compose.assertAllClickablesAccessible()
        compose.onNodeWithText("Delete permanently").performClick()
        compose.assertAllClickablesAccessible()
    }
}
