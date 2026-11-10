package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.ui.common.amountText
import io.github.vlukyanets.lemonflow.ui.common.goalLine
import java.util.Locale

/** Timer: play/pause inside a ring, live time, and "-" / "+" by one step in minutes (FR-007c). */
@Composable
internal fun TimerRow(
    row: TodayRowUi,
    hardDay: Boolean,
    actions: TodayActions,
    openEditor: (String) -> Unit
) {
    val habit = row.habit
    val minimum = stringResource(R.string.today_min_amount, amountText(habit.baseline))
    val meta = when {
        row.amount > 0 -> {
            val progress = stringResource(
                R.string.today_progress,
                clockText(row.amount),
                amountText(row.goal)
            )
            if (hardDay) progress else stringResource(R.string.meta_join, progress, minimum)
        }

        hardDay -> stringResource(R.string.today_goal_today, amountText(habit.baseline))

        else -> goalLine(habit)
    }
    val stepText = stringResource(R.string.amount_minutes, habit.step ?: 1)
    HabitRowFrame(
        row = row,
        meta = RowMeta(meta, minimum),
        openEditor = openEditor,
        leading = { TimerButton(row, actions) },
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

@Composable
private fun TimerButton(row: TodayRowUi, actions: TodayActions) {
    val label = stringResource(
        if (row.running) R.string.today_pause_cd else R.string.today_start_cd,
        row.habit.name
    )
    val blue = MaterialTheme.colorScheme.primary
    val goalSeconds = row.goal.value.coerceAtLeast(1) * SECONDS_PER_MINUTE
    // A finished, paused timer looks like any done habit: a filled check. A tap still
    // continues the timer (the label says "Start"); no corner badge sticking out of the circle.
    val finished = !row.running && row.shownMark == Mark.TARGET
    Box(
        Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button) { actions.toggleTimer(row) }
            .semantics { contentDescription = label }
            .then(if (finished) Modifier.background(blue) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        when {
            finished -> Box(Modifier.testTag(TIMER_DONE)) {
                CheckIcon(MaterialTheme.colorScheme.onPrimary)
            }

            else -> {
                ProgressRing(row.amount.toFloat() / goalSeconds)
                StrokeIcon(if (row.running) R.drawable.ic_pause else R.drawable.ic_play, blue)
            }
        }
    }
}

@Composable
private fun StrokeIcon(icon: Int, tint: Color) {
    Icon(
        painterResource(icon),
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(18.dp)
    )
}

/** "4:12" or, from one hour, "1:04:12". */
internal fun clockText(seconds: Int): String {
    val hours = seconds / SECONDS_PER_HOUR
    val minutes = seconds % SECONDS_PER_HOUR / SECONDS_PER_MINUTE
    val secs = seconds % SECONDS_PER_MINUTE
    return if (hours > 0) {
        "%d:%02d:%02d".format(Locale.ROOT, hours, minutes, secs)
    } else {
        "%d:%02d".format(Locale.ROOT, minutes, secs)
    }
}

private const val SECONDS_PER_MINUTE = 60
private const val SECONDS_PER_HOUR = 3600
