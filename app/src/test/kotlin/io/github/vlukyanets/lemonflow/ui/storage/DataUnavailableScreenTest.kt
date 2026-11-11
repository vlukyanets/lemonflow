package io.github.vlukyanets.lemonflow.ui.storage

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.domain.port.StorageGate
import io.github.vlukyanets.lemonflow.domain.port.StorageState
import io.github.vlukyanets.lemonflow.testing.FakeDayRepository
import io.github.vlukyanets.lemonflow.testing.FakeHabitRepository
import io.github.vlukyanets.lemonflow.testing.FakeSettingsRepository
import io.github.vlukyanets.lemonflow.testing.MutableClock
import io.github.vlukyanets.lemonflow.ui.TALL_PHONE
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import io.github.vlukyanets.lemonflow.ui.today.TodayScreen
import io.github.vlukyanets.lemonflow.ui.today.TodayViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TALL_PHONE)
class DataUnavailableScreenTest {
    @get:Rule
    val compose = createComposeRule()

    /** Unreadable until [startFresh] erases it; a new launch calls [open] again. */
    private class FakeStorageGate : StorageGate {
        var state = StorageState.Unavailable
        var startFreshCalls = 0

        override suspend fun open(): StorageState = state

        override suspend fun startFresh(): StorageState {
            startFreshCalls++
            state = StorageState.Ready
            return state
        }
    }

    private val gate = FakeStorageGate()

    private fun launch(ready: @Composable () -> Unit = { Text("ready") }) {
        val viewModel = StartupViewModel(gate)
        compose.setContent { LemonFlowTheme { StartupHost(viewModel, ready) } }
    }

    @Test
    fun unavailableShowsACalmExplanation() {
        launch()

        compose.onNodeWithText("Your data can't be opened").assertIsDisplayed()
        compose.onNodeWithText("Start fresh").assertIsDisplayed()
        listOf("exception", "error", "sqlite", "keystore", "AEAD", "cipher").forEach {
            compose.onAllNodes(hasText(it, substring = true, ignoreCase = true))
                .assertCountEquals(0)
        }
    }

    @Test
    fun startFreshAsksForConfirmation() {
        launch()
        compose.onNodeWithText("Start fresh").performClick()

        compose.onAllNodes(isDialog()).assertCountEquals(1)
        compose.onNodeWithText("Start fresh?").assertIsDisplayed()
        // The confirmation itself states that the data cannot be recovered (FR-015).
        compose.onNode(
            hasText("can't be recovered", substring = true) and hasAnyAncestor(isDialog())
        ).assertIsDisplayed()
        assertEquals(0, gate.startFreshCalls)
    }

    @Test
    fun cancelErasesNothing() {
        launch()
        compose.onNodeWithText("Start fresh").performClick()

        compose.onNodeWithText("Cancel").performClick()

        compose.onAllNodes(isDialog()).assertCountEquals(0)
        compose.onNodeWithText("Your data can't be opened").assertIsDisplayed()
        assertEquals(0, gate.startFreshCalls)
    }

    @Test
    fun confirmStartsFreshAndShowsTheEmptyToday() {
        val clock = MutableClock.atLocal("2026-11-09T10:00")
        val habits = FakeHabitRepository()
        val settings = FakeSettingsRepository()
        val today = TodayViewModel(FakeDayRepository(habits, settings, clock), settings, clock)
        launch { TodayScreen(today, openEditor = {}, addHabit = {}) }
        compose.onNodeWithText("Start fresh").performClick()

        compose.onNode(hasText("Start fresh") and hasClickAction() and hasAnyAncestor(isDialog()))
            .performClick()

        assertEquals(1, gate.startFreshCalls)
        compose.onNodeWithText("Welcome").assertIsDisplayed()
        compose.onNodeWithText("Add first habit").assertIsDisplayed()
    }

    @Test
    fun aNewLaunchStillUnavailableShowsTheScreenAgain() {
        launch()
        compose.onNodeWithText("Start fresh").performClick()
        compose.onNodeWithText("Cancel").performClick()

        // The user left without confirming; the next launch opens the store again.
        val relaunched = StartupViewModel(gate)
        compose.runOnIdle { assertEquals(StartupState.Unavailable, relaunched.state.value) }
        compose.onNodeWithText("Your data can't be opened").assertIsDisplayed()
        assertEquals(0, gate.startFreshCalls)
    }

    @Test
    fun textDefaultsToOnBackgroundInTheDarkTheme() {
        var seen = Color.Unspecified
        var expected = Color.Unspecified
        gate.state = StorageState.Ready
        val viewModel = StartupViewModel(gate)
        compose.setContent {
            LemonFlowTheme(darkTheme = true) {
                StartupHost(viewModel) {
                    seen = LocalContentColor.current
                    expected = MaterialTheme.colorScheme.onBackground
                }
            }
        }

        // Every StartupHost branch (this one and the unavailable screen) sits on one Surface,
        // so plain text is never black on the dark background.
        compose.runOnIdle { assertEquals(expected, seen) }
    }

    @Test
    fun accessibleWithAndWithoutTheDialog() {
        launch()
        compose.assertAllClickablesAccessible()

        compose.onNodeWithText("Start fresh").performClick()
        compose.assertAllClickablesAccessible()
    }
}
