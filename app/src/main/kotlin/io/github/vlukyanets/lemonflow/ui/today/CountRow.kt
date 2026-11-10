package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.ui.common.amountText

/** Count: a ring towards today's goal, and "-" / "+" by one step (FR-007b). */
@Composable
internal fun CountRow(
    row: TodayRowUi,
    hardDay: Boolean,
    actions: TodayActions,
    openEditor: (String) -> Unit
) {
    val habit = row.habit
    val progress =
        stringResource(R.string.today_progress, row.amount.toString(), amountText(row.goal))
    val minimum = stringResource(R.string.today_min_amount, amountText(habit.baseline))
    val meta = if (hardDay) progress else stringResource(R.string.meta_join, progress, minimum)
    val stepText =
        amountText(Amount(habit.step ?: 1, habit.target.unit, habit.target.customLabel))
    val ringLabel = stringResource(R.string.today_progress_cd, progress)
    HabitRowFrame(
        row = row,
        meta = RowMeta(meta, minimum),
        openEditor = openEditor,
        leading = {
            Box(
                Modifier.size(48.dp).semantics { contentDescription = ringLabel },
                contentAlignment = Alignment.Center
            ) {
                ProgressRing(row.amount.toFloat() / row.goal.value.coerceAtLeast(1))
                if (row.shownMark == Mark.TARGET) {
                    CheckIcon(MaterialTheme.colorScheme.primary, size = 18)
                }
            }
        },
        trailing = {
            SquareButton(
                label = stringResource(R.string.today_remove_cd, stepText, habit.name),
                onClick = { actions.changeAmount(habit.id, -1) },
                icon = R.drawable.ic_minus
            )
            SquareButton(
                label = stringResource(R.string.today_add_cd, stepText, habit.name),
                onClick = { actions.changeAmount(habit.id, +1) },
                icon = R.drawable.ic_plus
            )
        }
    )
}
