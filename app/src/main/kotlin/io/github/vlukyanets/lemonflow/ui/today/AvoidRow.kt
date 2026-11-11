package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Mark

/**
 * Avoid: a badge (a check while within the limit, else the count), "-" and "+1" (FR-007e).
 * Above the limit only the count is shown, in neutral wording, with no verdict.
 */
@Composable
internal fun AvoidRow(
    row: TodayRowUi,
    hardDay: Boolean,
    actions: TodayActions,
    openEditor: (String) -> Unit
) {
    val habit = row.habit
    val count = if (row.amount == 0) {
        stringResource(R.string.today_avoid_none)
    } else {
        pluralStringResource(R.plurals.today_avoid_times, row.amount, row.amount)
    }
    val limits = if (hardDay) {
        stringResource(R.string.today_avoid_limit, habit.baseline.value)
    } else {
        stringResource(R.string.today_avoid_limits, habit.target.value, habit.baseline.value)
    }
    val badgeLabel = stringResource(R.string.today_avoid_badge_cd, habit.name, count)
    HabitRowFrame(
        row = row,
        // Within the hard-day limit on a normal day: the count itself is what's "minimum".
        meta = RowMeta(stringResource(R.string.meta_join, count, limits), minimum = count),
        openEditor = openEditor,
        leading = {
            val look = if (row.shownMark == Mark.OPEN) RoundLook.OVER else row.shownMark.roundLook()
            RoundBadge(
                look = look,
                modifier = Modifier.clearAndSetSemantics { contentDescription = badgeLabel }
            ) { tint ->
                if (row.shownMark == Mark.TARGET) {
                    CheckIcon(tint)
                } else {
                    Text(
                        row.amount.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = tint
                    )
                }
            }
        },
        trailing = {
            SquareButton(
                label = stringResource(R.string.today_undo_one_cd, habit.name),
                onClick = { actions.changeAmount(habit.id, -1) },
                icon = R.drawable.ic_minus
            )
            SquareButton(
                label = stringResource(R.string.today_log_one_cd, habit.name),
                onClick = { actions.changeAmount(habit.id, +1) },
                text = "+1"
            )
        }
    )
}
