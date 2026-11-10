package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.rules.MarkRules
import io.github.vlukyanets.lemonflow.ui.common.amountText
import io.github.vlukyanets.lemonflow.ui.common.goalLine

/**
 * Check: a single round check (FR-007). A tap marks the day's goal - Target, or Baseline on a
 * hard day; on a normal day it raises a Baseline mark to Target; otherwise it undoes.
 */
@Composable
internal fun CheckRow(
    row: TodayRowUi,
    hardDay: Boolean,
    actions: TodayActions,
    openEditor: (String) -> Unit
) {
    val name = row.habit.name
    val meta = if (hardDay) {
        stringResource(R.string.today_goal_today, amountText(row.habit.baseline))
    } else {
        goalLine(row.habit)
    }
    val checkLabel = stringResource(checkLabelRes(MarkRules.nextCheck(row.mark, hardDay)), name)
    HabitRowFrame(
        row = row,
        meta = RowMeta(
            meta,
            minimum = stringResource(R.string.today_min_amount, amountText(row.habit.baseline))
        ),
        openEditor = openEditor,
        leading = {
            RoundBadge(
                look = row.shownMark.roundLook(),
                modifier = Modifier
                    .clickable(role = Role.Button) { actions.toggleDone(row) }
                    .semantics { contentDescription = checkLabel }
            ) { tint -> if (row.shownMark != Mark.OPEN) CheckIcon(tint) }
        },
        trailing = {}
    )
}

/** The screen-reader label says what the tap will do. */
private fun checkLabelRes(next: Mark): Int = when (next) {
    Mark.TARGET -> R.string.today_mark_done_cd
    Mark.BASELINE -> R.string.today_mark_min_cd
    Mark.OPEN -> R.string.today_undo_cd
}

internal fun Mark.roundLook(): RoundLook = when (this) {
    Mark.TARGET -> RoundLook.DONE
    Mark.BASELINE -> RoundLook.MINIMUM
    Mark.OPEN -> RoundLook.OPEN
}
