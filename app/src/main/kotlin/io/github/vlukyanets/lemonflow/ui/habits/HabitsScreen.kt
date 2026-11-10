package io.github.vlukyanets.lemonflow.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.ui.common.ScreenHeader
import io.github.vlukyanets.lemonflow.ui.common.habitSummary
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

@Composable
fun HabitsScreen(
    viewModel: HabitsViewModel,
    openEditor: (String?) -> Unit,
    openArchive: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = 24.dp,
                bottom = 16.dp + LocalBottomBarSpace.current
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // A FlowRow: when the title and the button don't fit (ru "Привычки" + "Добавить
        // привычку"), the button moves under the title instead of breaking the title's word.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.Bottom
        ) {
            ScreenHeader(
                stringResource(R.string.tab_habits),
                dateLine = null,
                modifier = Modifier.padding(end = 12.dp)
            )
            Button(onClick = { openEditor(null) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(
                    painterResource(R.drawable.ic_plus),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(stringResource(R.string.habits_add), modifier = Modifier.padding(start = 6.dp))
            }
        }
        if (state.active.isEmpty()) {
            Text(
                stringResource(R.string.habits_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted
            )
        } else {
            ActiveList(state.active, openEditor)
        }
        ArchiveEntry(state.archivedCount, openArchive)
    }
}

@Composable
private fun ActiveList(habits: List<Habit>, openEditor: (String?) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.habits_active),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f).semantics { heading() }
            )
            Text(
                habits.size.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = LemonFlow.colors.muted
            )
        }
        val shape = RoundedCornerShape(18.dp)
        Column(
            Modifier
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, LemonFlow.colors.line, shape)
        ) {
            habits.forEachIndexed { index, habit ->
                if (index > 0) HorizontalDivider(color = LemonFlow.colors.line)
                HabitListRow(habit) { openEditor(habit.id.value) }
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painterResource(R.drawable.ic_core),
                contentDescription = null,
                tint = LemonFlow.colors.muted,
                modifier = Modifier.size(13.dp)
            )
            Text(
                stringResource(R.string.habits_core_hint),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
        }
    }
}

@Composable
private fun HabitListRow(habit: Habit, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (habit.isCore) {
                    Icon(
                        painterResource(R.drawable.ic_core),
                        contentDescription = stringResource(R.string.core_habit),
                        tint = LemonFlow.colors.muted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Text(
                habitSummary(habit),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
        }
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            tint = LemonFlow.colors.muted
        )
    }
}

@Composable
private fun ArchiveEntry(count: Int, openArchive: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, LemonFlow.colors.line, shape)
            .clickable(onClick = openArchive)
            .padding(start = 16.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_archive),
            contentDescription = null,
            tint = LemonFlow.colors.muted
        )
        Text(
            stringResource(R.string.habits_archive),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = LemonFlow.colors.muted
        )
        Icon(
            painterResource(R.drawable.ic_chevron),
            contentDescription = null,
            tint = LemonFlow.colors.muted
        )
    }
}
