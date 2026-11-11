package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.PastDayRow
import io.github.vlukyanets.lemonflow.ui.common.LemonSheet
import io.github.vlukyanets.lemonflow.ui.common.SheetHeader
import io.github.vlukyanets.lemonflow.ui.common.amountText
import io.github.vlukyanets.lemonflow.ui.common.goalLine
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

/** A past date, view only (FR-006b); a day without marks reads "Nothing recorded". */
@Composable
internal fun PastDaySheet(day: PastDay, onClose: () -> Unit) {
    LemonSheet(onDismiss = onClose) {
        SheetHeader(
            title = formatDate(day.date, "EEE, d MMM"),
            subtitle = stringResource(R.string.past_view_only),
            onClose = onClose
        )
        HorizontalDivider(color = LemonFlow.colors.surface2)
        if (day.nothingRecorded) {
            NothingRecorded()
        } else {
            day.rows.forEachIndexed { index, row ->
                if (index > 0) HorizontalDivider(color = LemonFlow.colors.surface2)
                PastRow(row)
            }
        }
    }
}

@Composable
private fun NothingRecorded() {
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            stringResource(R.string.past_nothing),
            style = MaterialTheme.typography.titleMedium,
            color = LemonFlow.colors.muted
        )
        Text(
            stringResource(R.string.past_nothing_body),
            style = MaterialTheme.typography.bodyMedium,
            color = LemonFlow.colors.muted
        )
    }
}

@Composable
private fun PastRow(row: PastDayRow) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(row.habit.name, style = MaterialTheme.typography.titleSmall)
            Text(
                goalLine(row.habit),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
        }
        Text(
            pastLabel(row),
            style = MaterialTheme.typography.labelMedium,
            color = if (row.mark ==
                Mark.OPEN
            ) {
                LemonFlow.colors.muted
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        )
        MarkDot(row.mark)
    }
}

@Composable
private fun MarkDot(mark: Mark) {
    val blue = MaterialTheme.colorScheme.primary
    val modifier = Modifier.size(26.dp).clip(CircleShape)
    when (mark) {
        Mark.TARGET -> Box(modifier.background(blue), Alignment.Center) {
            CheckIcon(MaterialTheme.colorScheme.onPrimary, size = 14)
        }

        Mark.BASELINE -> Box(
            modifier.background(LemonFlow.colors.blueSoft).border(2.dp, blue, CircleShape),
            Alignment.Center
        ) { CheckIcon(blue, size = 14) }

        Mark.OPEN -> Box(
            modifier.background(
                LemonFlow.colors.surface2
            ).border(1.5.dp, LemonFlow.colors.line, CircleShape)
        )
    }
}

/** "Done · 2500 ml", "Minimum · 3 min", "Done · none" or, with no mark, the amount alone. */
@Composable
private fun pastLabel(row: PastDayRow): String {
    val quantity = quantity(row)
    val status = when (row.mark) {
        Mark.TARGET -> stringResource(R.string.past_done)

        Mark.BASELINE -> stringResource(R.string.past_minimum)

        Mark.OPEN -> return quantity?.takeIf { row.amount > 0 }
            ?: stringResource(R.string.past_nothing)
    }
    return if (quantity == null) status else stringResource(R.string.meta_join, status, quantity)
}

@Composable
private fun quantity(row: PastDayRow): String? {
    val habit = row.habit
    return when (habit.type) {
        HabitType.CHECK -> null

        HabitType.COUNT -> amountText(
            Amount(row.amount, habit.target.unit, habit.target.customLabel)
        )

        HabitType.TIMER -> stringResource(
            R.string.amount_minutes,
            (row.amount + HALF_MINUTE) / SECONDS_PER_MINUTE
        )

        HabitType.AVOID -> if (row.amount == 0) {
            stringResource(R.string.past_none)
        } else {
            pluralStringResource(R.plurals.amount_times, row.amount, row.amount)
        }
    }
}

private const val SECONDS_PER_MINUTE = 60
private const val HALF_MINUTE = 30
