package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.testing.FakeDayRepository
import io.github.vlukyanets.lemonflow.testing.FakeHabitRepository
import io.github.vlukyanets.lemonflow.testing.FakeSettingsRepository
import io.github.vlukyanets.lemonflow.testing.Habits
import io.github.vlukyanets.lemonflow.testing.MutableClock
import io.github.vlukyanets.lemonflow.ui.TALL_PHONE
import io.github.vlukyanets.lemonflow.ui.assertAllClickablesAccessible
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlowTheme
import java.time.Duration
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = TALL_PHONE)
class TodayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val scheduler = TestCoroutineScheduler()
    private val clock = MutableClock.atLocal("2026-11-09T10:00") // Monday
    private val monday = LocalDate.parse("2026-11-09")
    private val tuesday = LocalDate.parse("2026-11-10")
    private val habits = FakeHabitRepository(
        listOf(Habits.reading(), Habits.water(), Habits.meditation(), Habits.sugar())
    )
    private val settings = FakeSettingsRepository()
    private val days = FakeDayRepository(habits, settings, clock)
    private var openedEditor: String? = null
    private var addClicked = false

    @Before
    fun setUp() {
        // The ViewModel's day clock and timer ticker run on Main; virtual time drives them here.
        Dispatchers.setMain(UnconfinedTestDispatcher(scheduler))
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun open() {
        val viewModel = TodayViewModel(days, settings, clock)
        compose.setContent {
            LemonFlowTheme {
                TodayScreen(
                    viewModel = viewModel,
                    openEditor = { openedEditor = it },
                    addHabit = { addClicked = true }
                )
            }
        }
        compose.waitForIdle()
    }

    private fun advance(duration: Duration) {
        clock.advance(duration)
        scheduler.advanceTimeBy(duration.toMillis())
        scheduler.runCurrent()
        compose.waitForIdle()
    }

    private fun seed(block: suspend () -> Unit) = runBlocking { block() }

    private fun noDialogOpen() = compose.onAllNodes(isDialog()).assertCountEquals(0)

    // Header

    @Test
    fun headerShowsDateDayEndAndProgress() {
        days.put(Habits.reading().id, monday, Mark.TARGET)
        open()

        compose.onNodeWithText("MON · 9 NOV 2026").assertIsDisplayed()
        compose.onNodeWithText("Day closes at 03:00").assertIsDisplayed()
        // Reading done + the Avoid default.
        compose.onNodeWithText("2/4 done").assertIsDisplayed()
    }

    @Test
    fun screenMovesToTheNewDateAtTheDayEnd() {
        clock.setLocal("2026-11-10T02:59:59")
        days.put(Habits.reading().id, monday, Mark.TARGET)
        open()
        compose.onNodeWithText("MON · 9 NOV 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("Undo Reading").assertIsDisplayed()

        advance(Duration.ofSeconds(1))

        compose.onNodeWithText("TUE · 10 NOV 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark Reading done").assertIsDisplayed()
        compose.onNodeWithText("1/4 done").assertIsDisplayed() // only the Avoid default
        assertEquals(Mark.TARGET, days.records.value[Habits.reading().id to monday]?.mark)
    }

    @Test
    fun changingDayEndMovesNowToAnotherDate() {
        clock.setLocal("2026-11-10T04:00")
        days.put(Habits.reading().id, monday, Mark.TARGET)
        open()
        compose.onNodeWithText("TUE · 10 NOV 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add 250 ml to Water").performClick()
        compose.onNodeWithText("250 of 2500 ml · minimum 500 ml").assertIsDisplayed()

        seed { settings.setDayEndHour(5) }
        compose.waitForIdle()

        compose.onNodeWithText("MON · 9 NOV 2026").assertIsDisplayed()
        compose.onNodeWithText("Day closes at 05:00").assertIsDisplayed()
        compose.onNodeWithContentDescription("Undo Reading").assertIsDisplayed()
        compose.onNodeWithText("0 of 2500 ml · minimum 500 ml").assertIsDisplayed()
        assertEquals(250, days.records.value[Habits.water().id to tuesday]?.amount)
        assertNull(days.records.value[Habits.water().id to monday])
    }

    // Rows by type

    @Test
    fun checkRowOnANormalDayMarksTargetAndUndoes() {
        open()
        compose.onNodeWithText("Target 30 min · minimum 1 page").assertIsDisplayed()
        // One check only: the separate minimum button is gone (FR-007).
        compose.onAllNodesWithContentDescription("Mark Reading at minimum").assertCountEquals(0)

        compose.onNodeWithContentDescription("Mark Reading done").performClick()
        assertEquals(Mark.TARGET, days.records.value[Habits.reading().id to monday]?.mark)

        compose.onNodeWithContentDescription("Undo Reading").performClick()
        assertEquals(Mark.OPEN, days.records.value[Habits.reading().id to monday]?.mark)
    }

    @Test
    fun checkRowOnAHardDayMarksTheMinimum() {
        open()
        compose.onNode(switch()).performClick()

        compose.onNodeWithContentDescription("Mark Reading at minimum").performClick()
        assertEquals(Mark.BASELINE, days.records.value[Habits.reading().id to monday]?.mark)

        // Back on a normal day the minimum shows its tag, and a tap raises it to the target.
        compose.onNode(switch()).performClick()
        compose.onNodeWithTag(MIN_TAG, useUnmergedTree = true).assertIsDisplayed()
        compose.onNodeWithContentDescription("Mark Reading done").performClick()
        assertEquals(Mark.TARGET, days.records.value[Habits.reading().id to monday]?.mark)
        compose.onAllNodesWithTag(MIN_TAG, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun countRowShowsRingAndSteps() {
        days.put(Habits.water().id, monday, Mark.BASELINE, amount = 1500)
        open()

        compose.onNodeWithContentDescription("Progress: 1500 of 2500 ml").assertIsDisplayed()
        compose.onNodeWithText("1500 of 2500 ml · minimum 500 ml").assertIsDisplayed()
        compose.onNodeWithContentDescription("Add 250 ml to Water").performClick()
        compose.onNodeWithText("1750 of 2500 ml · minimum 500 ml").assertIsDisplayed()
        compose.onNodeWithContentDescription("Remove 250 ml from Water").performClick()
        compose.onNodeWithText("1500 of 2500 ml · minimum 500 ml").assertIsDisplayed()
    }

    @Test
    fun timerRowRunsLiveAndPauses() {
        open()
        compose.onNodeWithText("Target 10 min · minimum 1 min").assertIsDisplayed()

        compose.onNodeWithContentDescription("Start Meditation").performClick()
        advance(Duration.ofSeconds(252))
        compose.onNodeWithText("4:12 of 10 min · minimum 1 min").assertIsDisplayed()
        compose.onNodeWithTag(MIN_TAG, useUnmergedTree = true).assertIsDisplayed()

        compose.onNodeWithContentDescription("Pause Meditation").performClick()
        compose.onNodeWithContentDescription("Start Meditation").assertIsDisplayed()
        advance(Duration.ofSeconds(30))
        compose.onNodeWithText("4:12 of 10 min · minimum 1 min").assertIsDisplayed()

        compose.onNodeWithContentDescription("Add 1 min to Meditation").performClick()
        compose.onNodeWithText("5:12 of 10 min · minimum 1 min").assertIsDisplayed()
    }

    @Test
    fun finishedPausedTimerShowsAFilledCheckThatCanStillStart() {
        days.put(Habits.meditation().id, monday, Mark.TARGET, amount = 600)
        open()

        compose.onNodeWithText("10:00 of 10 min · minimum 1 min").assertIsDisplayed()
        compose.onNodeWithTag(TIMER_DONE, useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Start Meditation").performClick()
        compose.onNodeWithContentDescription("Pause Meditation").assertIsDisplayed()
        compose.onAllNodesWithTag(TIMER_DONE, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun avoidRowCountsWithoutVerdict() {
        open()
        compose.onNodeWithText("None today · limit 0, hard day 1").assertIsDisplayed()

        compose.onNodeWithContentDescription("Log one more: Added sugar").performClick()
        compose.onNodeWithText("1 time today · limit 0, hard day 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Added sugar: 1 time today").assertIsDisplayed()

        compose.onNodeWithContentDescription("Log one more: Added sugar").performClick()
        compose.onNodeWithText("2 times today · limit 0, hard day 1").assertIsDisplayed()

        compose.onNodeWithContentDescription("Undo one: Added sugar").performClick()
        compose.onNodeWithText("1 time today · limit 0, hard day 1").assertIsDisplayed()
    }

    @Test
    fun minTagShowsOnBaselineOnly() {
        days.put(Habits.reading().id, monday, Mark.BASELINE)
        days.put(Habits.water().id, monday, Mark.TARGET, amount = 2500)
        open()

        compose.onAllNodesWithTag(MIN_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    @Test
    fun emptyStateInvitesTheFirstHabit() {
        habits.habits.value = emptyList()
        open()

        compose.onNodeWithText("Welcome").assertIsDisplayed()
        compose.onNodeWithText("Add first habit").performClick()
        assertTrue(addClicked)
        compose.assertAllClickablesAccessible()
    }

    @Test
    fun tappingAHabitOpensItsEditor() {
        open()
        compose.onNodeWithText("Water").performClick()
        assertEquals("water", openedEditor)
    }

    // Week strip and past days

    @Test
    fun weekStripOpensAReadOnlyPastDay() {
        days.put(Habits.water().id, monday.minusDays(1), Mark.BASELINE, amount = 750)
        open()

        compose.onNodeWithContentDescription("Sunday 8", substring = true).performClick()

        compose.onNodeWithText("Sun, 8 Nov").assertIsDisplayed()
        compose.onNodeWithText("Past day · view only").assertIsDisplayed()
        compose.onNodeWithText("Minimum · 750 ml").assertIsDisplayed()
        compose.onNodeWithText("Done · none").assertIsDisplayed() // the Avoid default
        compose.assertAllClickablesAccessible()

        compose.onNodeWithContentDescription("Close").performClick()
        compose.onAllNodes(hasText("Past day · view only")).assertCountEquals(0)
    }

    @Test
    fun emptyPastDayReadsNothingRecorded() {
        open()
        compose.onNodeWithContentDescription("Wednesday 4", substring = true).performClick()

        compose.onNodeWithText("Wed, 4 Nov").assertIsDisplayed()
        compose.onNodeWithText("Nothing recorded").assertIsDisplayed()
        compose.onNodeWithText("No marks were made on this day.").assertIsDisplayed()
    }

    // Accessibility and one tap per action

    @Test
    fun everyRowTypeIsAccessible() {
        days.put(Habits.meditation().id, monday, Mark.BASELINE, amount = 60)
        open()
        compose.onNodeWithContentDescription("Start Meditation").performClick()
        compose.assertAllClickablesAccessible()
    }

    @Test
    fun everyActionTakesOneTapAndNoDialog() {
        open()
        val actions = listOf(
            "Mark Reading done" to "Undo Reading",
            "Add 250 ml to Water" to null,
            "Remove 250 ml from Water" to null,
            "Start Meditation" to "Pause Meditation",
            "Pause Meditation" to "Start Meditation",
            "Log one more: Added sugar" to "Added sugar: 1 time today"
        )
        actions.forEach { (action, after) ->
            val before = days.records.value
            compose.onNodeWithContentDescription(action).performClick()
            assertTrue("'$action' changed nothing", before != days.records.value)
            if (after != null) compose.onNodeWithContentDescription(after).assertIsDisplayed()
            noDialogOpen()
        }
        assertNull(openedEditor)
    }

    // Hard day (US3)

    @Test
    fun hardDaySwitchIsLabelled() {
        open()
        compose.onNode(switch()).assertIsOff()
        compose.onNodeWithText("Hard day").assertIsDisplayed()
        compose.onNodeWithText("Today counts at the minimum").assertIsDisplayed()
    }

    @Test
    fun hardDayShowsBaselineGoalsAfterOneTap() {
        days.put(Habits.water().id, monday, Mark.OPEN, amount = 250)
        open()

        compose.onNode(switch()).performClick()

        noDialogOpen()
        compose.onNode(switch()).assertIsOn()
        compose.onNodeWithText("Goal today: 1 page").assertIsDisplayed()
        compose.onNodeWithContentDescription("Progress: 250 of 500 ml").assertIsDisplayed()
        compose.onNodeWithText("250 of 500 ml").assertIsDisplayed()
        compose.onNodeWithText("Goal today: 1 min").assertIsDisplayed()
        compose.onNodeWithText("None today · limit 1").assertIsDisplayed()
        assertTrue(monday in days.hardDays.value)
    }

    @Test
    fun hardDayPresentsBaselineAsDoneWithoutMinTag() {
        days.put(Habits.reading().id, monday, Mark.BASELINE)
        days.put(Habits.water().id, monday, Mark.BASELINE, amount = 500)
        days.put(Habits.sugar().id, monday, Mark.BASELINE, amount = 1)
        seed { days.setHardDay(true) }
        open()

        compose.onAllNodesWithTag(MIN_TAG, useUnmergedTree = true).assertCountEquals(0)
        compose.onNodeWithContentDescription("Undo Reading").assertIsDisplayed()
        compose.onNodeWithText("3/4 done").assertIsDisplayed()
        compose.onNodeWithText("1 time today · limit 1").assertIsDisplayed()
    }

    @Test
    fun hardDayHasNoWarningCopy() {
        seed { days.setHardDay(true) }
        open()

        listOf("fail", "miss", "broken", "streak", "warning", "only", "reduced", "lost").forEach {
            compose.onAllNodes(hasText(it, substring = true, ignoreCase = true))
                .assertCountEquals(0)
        }
    }

    @Test
    fun hardDayIsAccessible() {
        seed { days.setHardDay(true) }
        open()
        compose.assertAllClickablesAccessible()
    }

    @Test
    fun timersSettleWhenTheScreenStarts() {
        open()
        assertTrue(days.settleCalls >= 1)
    }

    private fun switch() = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
}
