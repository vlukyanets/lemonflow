package io.github.vlukyanets.lemonflow.ui.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.SaveResult
import io.github.vlukyanets.lemonflow.domain.model.ValidationError
import io.github.vlukyanets.lemonflow.domain.port.HabitRepository
import io.github.vlukyanets.lemonflow.domain.rules.HabitValidator
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Field { NAME, TARGET, BASELINE, TARGET_LABEL, BASELINE_LABEL, STEP }

/** The editor form. Number fields hold text so the user can clear them while typing. */
data class EditorUiState(
    val isEdit: Boolean = false,
    val type: HabitType = HabitType.CHECK,
    val name: String = "",
    val targetText: String = "",
    val targetUnit: AmountUnit = AmountUnit.MINUTES,
    val targetLabel: String = "",
    val baselineText: String = "",
    val baselineUnit: AmountUnit = AmountUnit.MINUTES,
    val baselineLabel: String = "",
    val stepText: String = "1",
    val isCore: Boolean = false,
    val touched: Set<Field> = emptySet(),
    val confirmDelete: Boolean = false,
    val closed: Boolean = false
) {
    val draft: HabitDraft
        get() = HabitDraft(
            name = name,
            type = type,
            targetValue = targetText.toIntOrNull(),
            targetUnit = targetUnit,
            targetLabel = targetLabel,
            baselineValue = baselineText.toIntOrNull(),
            baselineUnit = baselineUnit,
            baselineLabel = baselineLabel,
            step = stepText.toIntOrNull(),
            isCore = isCore
        )

    val errors: List<ValidationError> get() = HabitValidator.validate(draft)

    val canSave: Boolean get() = errors.isEmpty()

    /** Errors worth showing now: "empty" ones only after the user touched that field. */
    fun visibleError(field: Field): ValidationError? = errors.firstOrNull { error ->
        error.field() == field && (!error.isEmptyKind() || field in touched)
    }
}

private fun ValidationError.field(): Field = when (this) {
    ValidationError.NAME_EMPTY, ValidationError.NAME_TOO_LONG -> Field.NAME

    ValidationError.TARGET_EMPTY -> Field.TARGET

    ValidationError.BASELINE_EMPTY,
    ValidationError.BASELINE_ABOVE_TARGET,
    ValidationError.LIMIT_BELOW_TARGET -> Field.BASELINE

    ValidationError.CUSTOM_UNIT_EMPTY, ValidationError.CUSTOM_UNIT_TOO_LONG -> Field.TARGET_LABEL

    ValidationError.STEP_INVALID -> Field.STEP
}

private fun ValidationError.isEmptyKind(): Boolean = this == ValidationError.NAME_EMPTY ||
    this == ValidationError.TARGET_EMPTY ||
    this == ValidationError.BASELINE_EMPTY ||
    this == ValidationError.CUSTOM_UNIT_EMPTY

@HiltViewModel
class EditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: HabitRepository
) : ViewModel() {
    private val habitId: HabitId? = savedStateHandle.get<String>(HABIT_ID_KEY)?.let(::HabitId)

    private val _state = MutableStateFlow(EditorUiState(isEdit = habitId != null))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        if (habitId != null) {
            viewModelScope.launch {
                repository.get(habitId)?.let { habit -> _state.update { it.filledFrom(habit) } }
            }
        }
    }

    fun setType(type: HabitType) = _state.update { if (it.isEdit) it else it.copy(type = type) }

    /** Any text field; number fields keep digits only. */
    fun setText(field: Field, value: String) = _state.update {
        val touched = it.touched + field
        when (field) {
            Field.NAME -> it.copy(name = value, touched = touched)
            Field.TARGET -> it.copy(targetText = value.digits(), touched = touched)
            Field.BASELINE -> it.copy(baselineText = value.digits(), touched = touched)
            Field.STEP -> it.copy(stepText = value.digits(), touched = touched)
            Field.TARGET_LABEL -> it.copy(targetLabel = value, touched = touched)
            Field.BASELINE_LABEL -> it.copy(baselineLabel = value, touched = touched)
        }
    }

    /** [forBaseline] picks the Minimum unit of a Check habit; other types share the Target unit. */
    fun setUnit(forBaseline: Boolean, unit: AmountUnit) = _state.update {
        if (forBaseline) it.copy(baselineUnit = unit) else it.copy(targetUnit = unit)
    }

    fun toggleCore() = _state.update { it.copy(isCore = !it.isCore) }

    fun askDelete(show: Boolean) = _state.update { it.copy(confirmDelete = show) }

    fun save() {
        val draft = _state.value.draft
        viewModelScope.launch {
            val result = if (habitId !=
                null
            ) {
                repository.update(habitId, draft)
            } else {
                repository.create(draft)
            }
            _state.update {
                if (result is SaveResult.Invalid) {
                    it.copy(
                        touched = Field.entries.toSet()
                    )
                } else {
                    it.copy(closed = true)
                }
            }
        }
    }

    fun archive() {
        val id = habitId ?: return
        viewModelScope.launch {
            repository.archive(id)
            _state.update { it.copy(closed = true) }
        }
    }

    fun deletePermanently() {
        val id = habitId ?: return
        viewModelScope.launch {
            repository.deletePermanently(id)
            _state.update { it.copy(confirmDelete = false, closed = true) }
        }
    }

    private fun EditorUiState.filledFrom(habit: Habit) = copy(
        type = habit.type,
        name = habit.name,
        targetText = habit.target.value.toString(),
        targetUnit = habit.target.unit,
        targetLabel = habit.target.customLabel.orEmpty(),
        baselineText = habit.baseline.value.toString(),
        baselineUnit = habit.baseline.unit,
        baselineLabel = habit.baseline.customLabel.orEmpty(),
        stepText = (habit.step ?: 1).toString(),
        isCore = habit.isCore
    )

    private fun String.digits(): String = filter(Char::isDigit).take(MAX_DIGITS)

    private companion object {
        /** Matches the `habitId` property of the navigation route. */
        const val HABIT_ID_KEY = "habitId"
        const val MAX_DIGITS = 6
    }
}
