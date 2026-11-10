package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark

/** How amounts turn into marks (data-model.md, "Mark from amount"). */
object MarkRules {
    private const val SECONDS_PER_MINUTE = 60

    /**
     * The mark for a stored [amount]: COUNT units, TIMER seconds, AVOID count. CHECK habits are
     * marked by the user, so their amount says nothing and the result is [Mark.OPEN].
     */
    fun markFor(habit: Habit, amount: Int): Mark = when (habit.type) {
        HabitType.CHECK -> Mark.OPEN

        HabitType.COUNT -> reached(amount, habit.target.value, habit.baseline.value)

        HabitType.TIMER -> reached(
            amount,
            habit.target.value * SECONDS_PER_MINUTE,
            habit.baseline.value * SECONDS_PER_MINUTE
        )

        HabitType.AVOID -> when {
            amount <= habit.target.value -> Mark.TARGET
            amount <= habit.baseline.value -> Mark.BASELINE
            else -> Mark.OPEN
        }
    }

    /** A Baseline mark always counts as complete (FR-009). */
    fun isComplete(mark: Mark): Boolean = mark != Mark.OPEN

    /** One "+" or "-" in the stored unit: COUNT `step` units, TIMER `step` minutes, AVOID 1. */
    fun stepSize(habit: Habit): Int = when (habit.type) {
        HabitType.COUNT -> habit.step ?: 1
        HabitType.TIMER -> (habit.step ?: 1) * SECONDS_PER_MINUTE
        HabitType.AVOID, HabitType.CHECK -> 1
    }

    /** Today's goal: the Target, or the Baseline on a hard day (FR-011). */
    fun goal(habit: Habit, hardDay: Boolean): Amount = if (hardDay) habit.baseline else habit.target

    /** On a hard day a Baseline mark is presented as the plan being done (FR-011). */
    fun shown(mark: Mark, hardDay: Boolean): Mark =
        if (hardDay && mark == Mark.BASELINE) Mark.TARGET else mark

    /**
     * The mark after one tap on a Check habit (FR-007): an open habit is marked at the day's
     * goal; on a normal day a Baseline mark is raised to Target; any other mark is undone.
     */
    fun nextCheck(current: Mark, hardDay: Boolean): Mark = when {
        current == Mark.OPEN -> if (hardDay) Mark.BASELINE else Mark.TARGET
        current == Mark.BASELINE && !hardDay -> Mark.TARGET
        else -> Mark.OPEN
    }

    private fun reached(amount: Int, target: Int, baseline: Int): Mark = when {
        amount >= target -> Mark.TARGET
        amount >= baseline -> Mark.BASELINE
        else -> Mark.OPEN
    }
}
