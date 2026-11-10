package io.github.vlukyanets.lemonflow.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.vlukyanets.lemonflow.R
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.ui.common.FieldHint
import io.github.vlukyanets.lemonflow.ui.common.unitName

/** How one text field looks: its label, test tag, keyboard and optional placeholder/suffix. */
internal data class FieldSpec(
    val label: String,
    val tag: String,
    val numeric: Boolean,
    val placeholder: String? = null,
    val suffix: String? = null
)

/** A unit dropdown: the current unit, its callback, test tag and screen-reader label. */
internal data class UnitSpec(
    val unit: AmountUnit,
    val onUnit: (AmountUnit) -> Unit,
    val tag: String,
    val label: String
)

/** Target/Minimum (or the Avoid limits) for the current type. */
@Composable
internal fun AmountSection(state: EditorUiState, viewModel: EditorViewModel) {
    when (state.type) {
        HabitType.CHECK -> {
            CheckAmount(state, viewModel, baseline = false)
            CheckAmount(state, viewModel, baseline = true)
        }

        HabitType.COUNT, HabitType.AVOID -> {
            SharedUnit(state, viewModel)
            AmountPair(state, viewModel, suffix = sharedSuffix(state))
        }

        HabitType.TIMER -> AmountPair(
            state,
            viewModel,
            suffix = stringResource(R.string.unit_min_short)
        )
    }
}

@Composable
private fun CheckAmount(state: EditorUiState, viewModel: EditorViewModel, baseline: Boolean) {
    val unit = if (baseline) state.baselineUnit else state.targetUnit
    val label = if (baseline) state.baselineLabel else state.targetLabel
    val textField = if (baseline) Field.BASELINE else Field.TARGET
    val labelField = if (baseline) Field.BASELINE_LABEL else Field.TARGET_LABEL
    val unitTag = if (baseline) Tags.BASELINE_UNIT else Tags.TARGET_UNIT
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(
            stringResource(if (baseline) R.string.editor_minimum else R.string.editor_target)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorTextField(
                value = if (baseline) state.baselineText else state.targetText,
                onValueChange = { viewModel.setText(textField, it) },
                spec = FieldSpec(
                    label = stringResource(R.string.editor_amount),
                    tag = if (baseline) Tags.BASELINE_AMOUNT else Tags.TARGET_AMOUNT,
                    numeric = true
                ),
                modifier = Modifier.weight(AMOUNT_WEIGHT)
            )
            Box(Modifier.weight(1f - AMOUNT_WEIGHT)) {
                UnitPicker(
                    UnitSpec(
                        unit = unit,
                        onUnit = { viewModel.setUnit(baseline, it) },
                        tag = unitTag,
                        label = stringResource(unitLabelRes(baseline))
                    )
                )
            }
        }
        if (unit == AmountUnit.CUSTOM) {
            LabelField(label, { viewModel.setText(labelField, it) }, unitTag + "_label")
            if (label.isBlank() && labelField in state.touched) {
                FieldHint(stringResource(R.string.error_custom_unit_empty))
            }
        }
        state.visibleError(textField)?.let { FieldHint(errorText(it, state.type)) }
    }
}

@Composable
private fun SharedUnit(state: EditorUiState, viewModel: EditorViewModel) {
    val label = stringResource(R.string.editor_unit)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The field's own label says "Unit"; a section label above it would repeat it.
        UnitPicker(UnitSpec(state.targetUnit, { viewModel.setUnit(false, it) }, Tags.UNIT, label))
        if (state.targetUnit == AmountUnit.CUSTOM) {
            LabelField(
                state.targetLabel,
                { viewModel.setText(Field.TARGET_LABEL, it) },
                Tags.UNIT + "_label"
            )
        }
        state.visibleError(Field.TARGET_LABEL)?.let { FieldHint(errorText(it, state.type)) }
    }
}

@Composable
private fun AmountPair(state: EditorUiState, viewModel: EditorViewModel, suffix: String) {
    AmountField(state, viewModel, baseline = false, suffix = suffix)
    AmountField(state, viewModel, baseline = true, suffix = suffix)
}

/**
 * The long "good day / hard day" title sits above a full-width field with a short label: a
 * long label inside a narrow field wraps while empty and resizes the field when it floats.
 */
@Composable
private fun AmountField(
    state: EditorUiState,
    viewModel: EditorViewModel,
    baseline: Boolean,
    suffix: String
) {
    val avoid = state.type == HabitType.AVOID
    val field = if (baseline) Field.BASELINE else Field.TARGET
    val title = when {
        avoid && baseline -> R.string.editor_limit_hard
        avoid -> R.string.editor_limit_good
        baseline -> R.string.editor_minimum
        else -> R.string.editor_target
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionLabel(stringResource(title))
        EditorTextField(
            value = if (baseline) state.baselineText else state.targetText,
            onValueChange = { viewModel.setText(field, it) },
            spec = FieldSpec(
                label = stringResource(R.string.editor_amount),
                tag = if (baseline) Tags.BASELINE_AMOUNT else Tags.TARGET_AMOUNT,
                numeric = true,
                suffix = suffix
            )
        )
        state.visibleError(field)?.let { FieldHint(errorText(it, state.type)) }
    }
}

@Composable
private fun LabelField(value: String, onValue: (String) -> Unit, tag: String) {
    EditorTextField(
        value = value,
        onValueChange = onValue,
        spec = FieldSpec(
            label = stringResource(R.string.editor_custom_unit),
            tag = tag,
            numeric = false
        )
    )
}

@Composable
internal fun EditorTextField(
    value: String,
    onValueChange: (String) -> Unit,
    spec: FieldSpec,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(spec.label) },
        placeholder = spec.placeholder?.let { { Text(it) } },
        suffix = spec.suffix?.let { { Text(it) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (spec.numeric) KeyboardType.Number else KeyboardType.Text
        ),
        modifier = modifier.fillMaxWidth().testTag(spec.tag)
    )
}

@Composable
private fun sharedSuffix(state: EditorUiState): String = if (state.targetUnit ==
    AmountUnit.CUSTOM
) {
    state.targetLabel.trim()
} else {
    stringResource(unitName(state.targetUnit))
}

@Composable
internal fun stepSuffix(state: EditorUiState): String = if (state.type == HabitType.TIMER) {
    stringResource(R.string.unit_min_short)
} else {
    sharedSuffix(state)
}

private fun unitLabelRes(baseline: Boolean): Int =
    if (baseline) R.string.editor_minimum_unit else R.string.editor_target_unit

private const val AMOUNT_WEIGHT = 0.4f
