package io.github.vlukyanets.lemonflow.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.ui.common.ConfirmDeleteDialog
import io.github.vlukyanets.lemonflow.ui.common.habitSummary
import io.github.vlukyanets.lemonflow.ui.navigation.LocalBottomBarSpace
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(
                start = 8.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 16.dp + LocalBottomBarSpace.current
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painterResource(R.drawable.ic_back),
                contentDescription = stringResource(R.string.back)
            )
        }
        Column(Modifier.padding(start = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(R.string.habits_archive),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.archive_body),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted
            )
            if (state.archived.isEmpty()) {
                Text(
                    stringResource(R.string.archive_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LemonFlow.colors.muted
                )
            }
            state.archived.forEach { habit ->
                ArchivedCard(habit, onRestore = {
                    viewModel.restore(habit.id)
                }, onDelete = { viewModel.askDelete(habit) })
            }
        }
    }
    state.pendingDelete?.let { habit ->
        ConfirmDeleteDialog(
            habitName = habit.name,
            onConfirm = viewModel::confirmDelete,
            onDismiss = { viewModel.askDelete(null) }
        )
    }
}

@Composable
private fun ArchivedCard(habit: Habit, onRestore: () -> Unit, onDelete: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    habit.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                habit.archivedAt?.let { at ->
                    val date = DATE_FORMAT.format(at.atZone(ZoneId.systemDefault()))
                    Text(
                        stringResource(R.string.archived_on, date),
                        style = MaterialTheme.typography.labelSmall,
                        color = LemonFlow.colors.muted
                    )
                }
            }
            Text(
                habitSummary(habit),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
        }
        ArchivedActions(habit.name, onRestore, onDelete)
    }
}

@Composable
private fun ArchivedActions(name: String, onRestore: () -> Unit, onDelete: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val restoreLabel = stringResource(R.string.archive_restore_cd, name)
        OutlinedButton(
            onClick = onRestore,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics { contentDescription = restoreLabel }
        ) {
            Icon(
                painterResource(R.drawable.ic_restore),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Text(
                stringResource(R.string.archive_restore),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
        val deleteLabel = stringResource(R.string.delete_permanently_cd, name)
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .semantics { contentDescription = deleteLabel }
        ) {
            Text(stringResource(R.string.delete_permanently), color = LemonFlow.colors.danger)
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
