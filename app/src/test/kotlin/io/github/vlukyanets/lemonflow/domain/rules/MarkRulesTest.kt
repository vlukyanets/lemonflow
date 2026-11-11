package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.testing.Habits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkRulesTest {
    private val water = Habits.water() // Target 2500, Baseline 500, step 250
    private val meditation = Habits.meditation() // Target 10 min, Baseline 1 min, step 1
    private val sugar = Habits.sugar() // limit 0, hard-day limit 1

    @Test
    fun countMarkFollowsTheAmount() {
        assertEquals(Mark.OPEN, MarkRules.markFor(water, 0))
        assertEquals(Mark.OPEN, MarkRules.markFor(water, 499))
        assertEquals(Mark.BASELINE, MarkRules.markFor(water, 500))
        assertEquals(Mark.BASELINE, MarkRules.markFor(water, 2499))
        assertEquals(Mark.TARGET, MarkRules.markFor(water, 2500))
        assertEquals(Mark.TARGET, MarkRules.markFor(water, 4000))
    }

    @Test
    fun timerComparesSecondsWithMinutesTimesSixty() {
        assertEquals(Mark.OPEN, MarkRules.markFor(meditation, 59))
        assertEquals(Mark.BASELINE, MarkRules.markFor(meditation, 60))
        assertEquals(Mark.BASELINE, MarkRules.markFor(meditation, 599))
        assertEquals(Mark.TARGET, MarkRules.markFor(meditation, 600))
    }

    @Test
    fun avoidMarkStaysDoneWithinTheLimits() {
        assertEquals(Mark.TARGET, MarkRules.markFor(sugar, 0))
        assertEquals(Mark.BASELINE, MarkRules.markFor(sugar, 1))
        assertEquals(Mark.OPEN, MarkRules.markFor(sugar, 2))

        val coffee = sugar.copy(
            target = Amount(2, AmountUnit.TIMES),
            baseline = Amount(4, AmountUnit.TIMES)
        )
        assertEquals(Mark.TARGET, MarkRules.markFor(coffee, 2))
        assertEquals(Mark.BASELINE, MarkRules.markFor(coffee, 4))
        assertEquals(Mark.OPEN, MarkRules.markFor(coffee, 5))
    }

    @Test
    fun baselineCountsAsComplete() {
        assertTrue(MarkRules.isComplete(Mark.TARGET))
        assertTrue(MarkRules.isComplete(Mark.BASELINE))
        assertFalse(MarkRules.isComplete(Mark.OPEN))
    }

    @Test
    fun stepSizeIsInTheStoredUnit() {
        assertEquals(250, MarkRules.stepSize(water))
        assertEquals(60, MarkRules.stepSize(meditation))
        assertEquals(1, MarkRules.stepSize(sugar))
        assertEquals(180, MarkRules.stepSize(meditation.copy(step = 3)))
    }

    @Test
    fun hardDayGoalIsTheBaseline() {
        assertEquals(water.target, MarkRules.goal(water, hardDay = false))
        assertEquals(water.baseline, MarkRules.goal(water, hardDay = true))
    }

    @Test
    fun hardDayPresentsBaselineAsDone() {
        assertEquals(Mark.TARGET, MarkRules.shown(Mark.BASELINE, hardDay = true))
        assertEquals(Mark.BASELINE, MarkRules.shown(Mark.BASELINE, hardDay = false))
        assertEquals(Mark.OPEN, MarkRules.shown(Mark.OPEN, hardDay = true))
        assertEquals(Mark.TARGET, MarkRules.shown(Mark.TARGET, hardDay = true))
    }

    @Test
    fun oneCheckTapMarksTheDaysGoalRaisesAMinimumOrUndoes() {
        // Normal day.
        assertEquals(Mark.TARGET, MarkRules.nextCheck(Mark.OPEN, hardDay = false))
        assertEquals(Mark.TARGET, MarkRules.nextCheck(Mark.BASELINE, hardDay = false))
        assertEquals(Mark.OPEN, MarkRules.nextCheck(Mark.TARGET, hardDay = false))
        // Hard day: the minimum is the goal, and any mark is shown as done, so a tap undoes.
        assertEquals(Mark.BASELINE, MarkRules.nextCheck(Mark.OPEN, hardDay = true))
        assertEquals(Mark.OPEN, MarkRules.nextCheck(Mark.BASELINE, hardDay = true))
        assertEquals(Mark.OPEN, MarkRules.nextCheck(Mark.TARGET, hardDay = true))
    }
}
