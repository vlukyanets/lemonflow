package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow
import java.time.LocalDate

/** The last 7 user dates ending with today (FR-006a); an earlier date opens read-only. */
@Composable
internal fun WeekStrip(week: List<DaySummary>, openDay: (LocalDate) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        week.forEach { day -> DayCell(day, openDay, Modifier.weight(1f)) }
    }
}

private data class CellColors(
    val background: Color,
    val text: Color,
    val sub: Color,
    val track: Color,
    val fill: Color
)

@Composable
private fun cellColors(day: DaySummary): CellColors {
    val c = LemonFlow.colors
    return when {
        day.isToday -> CellColors(
            c.inverse,
            c.onInverse,
            c.inverseMuted,
            c.inverseTrack,
            c.onInverse
        )

        day.nothingRecorded -> CellColors(c.surface2, c.muted, c.muted, c.line, c.line)

        else -> CellColors(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.onSurface,
            c.muted,
            c.blueSoft,
            MaterialTheme.colorScheme.primary
        )
    }
}

/** "Sunday 8, 2 of 4 done" for screen readers. */
@Composable
private fun dayLabel(day: DaySummary): String {
    val status = when {
        day.isToday -> stringResource(R.string.today_status_today)

        day.nothingRecorded -> stringResource(R.string.today_status_nothing)

        else -> pluralStringResource(
            R.plurals.today_status_done,
            day.completed,
            day.completed,
            day.active
        )
    }
    return stringResource(
        R.string.today_day_cd,
        formatDate(day.date, "EEEE"),
        day.date.dayOfMonth,
        status
    )
}

@Composable
private fun DayCell(day: DaySummary, openDay: (LocalDate) -> Unit, modifier: Modifier) {
    val colors = cellColors(day)
    val label = dayLabel(day)
    val clickable = if (day.isToday) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button) { openDay(day.date) }
    }
    Column(
        modifier
            .heightIn(min = 60.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.background)
            .then(clickable)
            .clearAndSetSemantics { contentDescription = label },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically)
    ) {
        Text(
            formatDate(day.date, "EEE"),
            style = MaterialTheme.typography.labelSmall,
            color = colors.sub
        )
        Text(
            day.date.dayOfMonth.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = colors.text
        )
        val share = if (day.active > 0) day.completed.toFloat() / day.active else 0f
        Box(
            Modifier
                .width(22.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.track)
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(share)
                    .background(colors.fill)
            )
        }
    }
}
