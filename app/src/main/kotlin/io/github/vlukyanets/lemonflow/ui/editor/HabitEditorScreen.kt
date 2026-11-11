package io.github.vlukyanets.lemonflow.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.ui.common.ConfirmDeleteDialog

@Composable
fun HabitEditorScreen(viewModel: EditorViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.closed) { if (state.closed) onClose() }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            // Edge-to-edge (targetSdk 35+) ignores adjustResize: shrink above the keyboard
            // ourselves, so the scroll can bring the focused field into view.
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        EditorTopBar(
            isEdit = state.isEdit,
            canSave = state.canSave,
            onCancel = onClose,
            onSave = viewModel::save
        )
        TypeSection(state, viewModel::setType)
        NameField(state) { viewModel.setText(Field.NAME, it) }
        AmountSection(state, viewModel)
        if (state.type == HabitType.COUNT || state.type == HabitType.TIMER) {
            StepField(state) { viewModel.setText(Field.STEP, it) }
        }
        CoreSwitch(state.isCore, viewModel::toggleCore)
        if (state.isEdit) {
            EditActions(onArchive = viewModel::archive, onDelete = { viewModel.askDelete(true) })
        }
    }

    if (state.confirmDelete) {
        ConfirmDeleteDialog(
            habitName = state.name.trim(),
            onConfirm = viewModel::deletePermanently,
            onDismiss = { viewModel.askDelete(false) }
        )
    }
}

@Composable
private fun EditorTopBar(
    isEdit: Boolean,
    canSave: Boolean,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        Text(
            stringResource(if (isEdit) R.string.editor_edit else R.string.editor_new),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f).semantics { heading() }
        )
        TextButton(onClick = onSave, enabled = canSave, modifier = Modifier.testTag(Tags.SAVE)) {
            Text(stringResource(R.string.editor_save))
        }
    }
}
