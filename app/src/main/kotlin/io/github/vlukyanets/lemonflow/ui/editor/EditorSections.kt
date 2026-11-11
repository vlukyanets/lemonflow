package io.github.vlukyanets.lemonflow.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.ValidationError
import io.github.vlukyanets.lemonflow.ui.common.FieldHint
import io.github.vlukyanets.lemonflow.ui.common.typeHint
import io.github.vlukyanets.lemonflow.ui.common.typeLabel
import io.github.vlukyanets.lemonflow.ui.theme.LemonFlow

@Composable
internal fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
internal fun TypeSection(state: EditorUiState, onType: (HabitType) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(R.string.editor_type))
        if (state.isEdit) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .background(LemonFlow.colors.surface2, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(typeLabel(state.type)),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    stringResource(typeHint(state.type)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LemonFlow.colors.muted
                )
            }
            Text(
                stringResource(R.string.editor_type_locked),
                style = MaterialTheme.typography.bodySmall,
                color = LemonFlow.colors.muted
            )
        } else {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag(Tags.TYPE_PICKER)) {
                HabitType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = state.type == type,
                        onClick = { onType(type) },
                        shape = SegmentedButtonDefaults.itemShape(index, HabitType.entries.size),
                        icon = {},
                        label = {
                            Text(
                                stringResource(typeLabel(type)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
            Text(
                stringResource(typeHint(state.type)),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted
            )
        }
    }
}

@Composable
internal fun NameField(state: EditorUiState, onName: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorTextField(
            value = state.name,
            onValueChange = onName,
            spec = FieldSpec(
                label = stringResource(R.string.editor_name),
                tag = Tags.NAME,
                numeric = false,
                placeholder = stringResource(R.string.editor_name_placeholder)
            )
        )
        state.visibleError(Field.NAME)?.let { FieldHint(errorText(it, state.type)) }
    }
}

@Composable
internal fun StepField(state: EditorUiState, onStep: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        EditorTextField(
            value = state.stepText,
            onValueChange = onStep,
            spec = FieldSpec(
                label = stringResource(R.string.editor_step),
                tag = Tags.STEP,
                numeric = true,
                suffix = stepSuffix(state)
            )
        )
        Text(
            stringResource(R.string.editor_step_hint),
            style = MaterialTheme.typography.bodySmall,
            color = LemonFlow.colors.muted
        )
        state.visibleError(Field.STEP)?.let { FieldHint(errorText(it, state.type)) }
    }
}

@Composable
internal fun CoreSwitch(isCore: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, LemonFlow.colors.line, shape)
            .toggleable(value = isCore, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            painterResource(R.drawable.ic_core),
            contentDescription = null,
            tint = LemonFlow.colors.muted,
            modifier = Modifier.size(20.dp)
        )
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.core_habit), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.editor_core_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = LemonFlow.colors.muted
            )
        }
        Switch(checked = isCore, onCheckedChange = null)
    }
}

@Composable
internal fun EditActions(onArchive: () -> Unit, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedButton(
            onClick = onArchive,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Icon(
                painterResource(R.drawable.ic_archive),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Text(stringResource(R.string.editor_archive), modifier = Modifier.padding(start = 8.dp))
        }
        OutlinedButton(
            onClick = onDelete,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)
        ) {
            Text(stringResource(R.string.delete_permanently), color = LemonFlow.colors.danger)
        }
    }
}

@Composable
internal fun errorText(error: ValidationError, type: HabitType): String {
    val avoid = type == HabitType.AVOID
    return stringResource(
        when (error) {
            ValidationError.NAME_EMPTY -> R.string.error_name_empty

            ValidationError.NAME_TOO_LONG -> R.string.error_name_too_long

            ValidationError.TARGET_EMPTY ->
                if (avoid) R.string.error_limit_good_empty else R.string.error_target_empty

            ValidationError.BASELINE_EMPTY ->
                if (avoid) R.string.error_limit_hard_empty else R.string.error_baseline_empty

            ValidationError.BASELINE_ABOVE_TARGET -> R.string.error_baseline_above_target

            ValidationError.LIMIT_BELOW_TARGET -> R.string.error_limit_below_target

            ValidationError.CUSTOM_UNIT_EMPTY -> R.string.error_custom_unit_empty

            ValidationError.CUSTOM_UNIT_TOO_LONG -> R.string.error_custom_unit_too_long

            ValidationError.STEP_INVALID -> R.string.error_step_invalid
        }
    )
}
