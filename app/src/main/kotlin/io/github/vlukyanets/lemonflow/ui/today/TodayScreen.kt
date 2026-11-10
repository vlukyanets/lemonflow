package io.github.vlukyanets.lemonflow.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.ui.common.ScreenHeader
import io.github.vlukyanets.lemonflow.ui.common.hourText
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun TodayScreen(viewModel: TodayViewModel, openEditor: (String) -> Unit, addHabit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }
    val actions = remember(viewModel) {
        TodayActions(
            toggleDone = viewModel::toggleDone,
            changeAmount = viewModel::changeAmount,
            toggleTimer = viewModel::toggleTimer
        )
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (!state.loading) {
            TodayContent(state, actions, viewModel, openEditor, addHabit)
        }
    }
    state.pastDay?.let { PastDaySheet(it, viewModel::closePastDay) }
}

@Composable
private fun TodayContent(
    state: TodayUiState,
    actions: TodayActions,
    viewModel: TodayViewModel,
    openEditor: (String) -> Unit,
    addHabit: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 16.dp + LocalBottomBarSpace.current
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(stringResource(R.string.tab_today), formatDate(state.date, "EEE · d MMM yyyy"))
        DayLine(state)
        if (state.rows.isEmpty()) {
            EmptyToday(addHabit)
            return@Column
        }
        HardDaySwitch(state.hardDay, viewModel::setHardDay)
        WeekStrip(state.week, viewModel::openPastDay)
        Text(
            stringResource(R.string.tab_habits),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp).semantics { heading() }
        )
        HabitRows(state, actions, openEditor)
    }
}

/** A FlowRow: with large text the done count moves to its own line instead of squeezing. */
@Composable
private fun DayLine(state: TodayUiState) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_clock),
                contentDescription = null,
                tint = LemonFlow.colors.muted,
                modifier = Modifier.size(16.dp)
            )
            Text(
                stringResource(R.string.today_day_closes, hourText(state.dayEndHour)),
                style = MaterialTheme.typography.labelMedium,
                color = LemonFlow.colors.muted,
                modifier = Modifier.padding(start = 6.dp, end = 12.dp)
            )
        }
        if (state.rows.isNotEmpty()) {
            Text(
                pluralStringResource(
                    R.plurals.today_done_count,
                    state.doneCount,
                    state.doneCount,
                    state.rows.size
                ),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

/** One tap turns today's goals into Baselines (FR-010, SC-002); no dialog, no warning. */
@Composable
private fun HardDaySwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .background(if (on) LemonFlow.colors.blueSoft else MaterialTheme.colorScheme.surface)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.today_hard_day),
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                stringResource(R.string.today_hard_day_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted
            )
        }
        Switch(
            checked = on,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                uncheckedTrackColor = LemonFlow.colors.trackOff,
                uncheckedBorderColor = LemonFlow.colors.trackOff,
                uncheckedThumbColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun HabitRows(state: TodayUiState, actions: TodayActions, openEditor: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LemonFlow.colors.line, shape)
    ) {
        state.rows.forEachIndexed { index, row ->
            if (index > 0) HorizontalDivider(color = LemonFlow.colors.line)
            when (row.habit.type) {
                HabitType.CHECK -> CheckRow(row, state.hardDay, actions, openEditor)
                HabitType.COUNT -> CountRow(row, state.hardDay, actions, openEditor)
                HabitType.TIMER -> TimerRow(row, state.hardDay, actions, openEditor)
                HabitType.AVOID -> AvoidRow(row, state.hardDay, actions, openEditor)
            }
        }
    }
}

@Composable
private fun EmptyToday(addHabit: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        val shape = RoundedCornerShape(22.dp)
        Box(
            Modifier
                .size(72.dp)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LemonFlow.colors.line, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painterResource(R.drawable.ic_sprout),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }
        Text(
            stringResource(R.string.today_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.today_empty_body),
            style = MaterialTheme.typography.bodyLarge,
            color = LemonFlow.colors.muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )
        Button(onClick = addHabit, modifier = Modifier.heightIn(min = 52.dp)) {
            Icon(
                painterResource(R.drawable.ic_plus),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(
                stringResource(R.string.today_add_first),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/** A date in the current locale, first letter capitalised ("Mon · 9 Nov 2026", "Вс, 8 нояб."). */
@Composable
internal fun formatDate(date: LocalDate, pattern: String): String {
    val locale = LocalConfiguration.current.locales[0]
    val text = DateTimeFormatter.ofPattern(pattern, locale).format(date)
    return text.replaceFirstChar { it.titlecase(locale) }
}
