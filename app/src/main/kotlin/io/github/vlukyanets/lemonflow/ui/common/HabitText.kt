package io.github.vlukyanets.lemonflow.ui.common

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import java.util.Locale

/** "30 min", "1 page", "2500 ml" in the current locale. */
@Composable
fun amountText(amount: Amount): String = when (amount.unit) {
    AmountUnit.MINUTES -> stringResource(R.string.amount_minutes, amount.value)

    AmountUnit.TIMES -> pluralStringResource(R.plurals.amount_times, amount.value, amount.value)

    AmountUnit.PAGES -> pluralStringResource(R.plurals.amount_pages, amount.value, amount.value)

    AmountUnit.STEPS -> pluralStringResource(R.plurals.amount_steps, amount.value, amount.value)

    AmountUnit.CUSTOM -> stringResource(
        R.string.amount_custom,
        amount.value,
        amount.customLabel.orEmpty()
    )
}

/** "Target 30 min · minimum 1 page" or, for Avoid, "Limit 0 · hard day 1". */
@Composable
fun goalLine(habit: Habit): String = if (habit.type == HabitType.AVOID) {
    stringResource(R.string.goal_limits, habit.target.value, habit.baseline.value)
} else {
    stringResource(R.string.goal_target_min, amountText(habit.target), amountText(habit.baseline))
}

@StringRes
fun typeLabel(type: HabitType): Int = when (type) {
    HabitType.CHECK -> R.string.type_check
    HabitType.COUNT -> R.string.type_count
    HabitType.TIMER -> R.string.type_timer
    HabitType.AVOID -> R.string.type_avoid
}

@StringRes
fun typeHint(type: HabitType): Int = when (type) {
    HabitType.CHECK -> R.string.type_check_hint
    HabitType.COUNT -> R.string.type_count_hint
    HabitType.TIMER -> R.string.type_timer_hint
    HabitType.AVOID -> R.string.type_avoid_hint
}

@StringRes
fun unitName(unit: AmountUnit): Int = when (unit) {
    AmountUnit.MINUTES -> R.string.unit_minutes
    AmountUnit.TIMES -> R.string.unit_times
    AmountUnit.PAGES -> R.string.unit_pages
    AmountUnit.STEPS -> R.string.unit_steps
    AmountUnit.CUSTOM -> R.string.unit_custom
}

/** "03:00": "Day ends at" takes whole hours only (FR-018). */
fun hourText(hour: Int): String = "%02d:00".format(Locale.ROOT, hour)

/** The Habits-list line: "Count · Target 2500 ml · minimum 500 ml · step 250 ml". */
@Composable
fun habitSummary(habit: Habit): String {
    val base =
        stringResource(R.string.meta_join, stringResource(typeLabel(habit.type)), goalLine(habit))
    val step = habit.step
    return if (habit.type == HabitType.COUNT && step != null) {
        val stepAmount = amountText(Amount(step, habit.target.unit, habit.target.customLabel))
        stringResource(R.string.meta_join, base, stringResource(R.string.goal_step, stepAmount))
    } else {
        base
    }
}
