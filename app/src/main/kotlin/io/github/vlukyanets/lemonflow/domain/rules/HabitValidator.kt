package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.ValidationError

/** FR-002, FR-002b and FR-005: what a habit may look like, per type. */
object HabitValidator {
    /** Characters, after trimming (FR-002b). */
    const val NAME_MAX_LENGTH = 120

    /** Characters of a custom unit name, after trimming (FR-002b). */
    const val UNIT_LABEL_MAX_LENGTH = 30
    private const val DEFAULT_STEP = 1

    /**
     * Applies the type rules that are not choices: trimmed text, one unit for Count/Timer/Avoid,
     * minutes for Timer, step only for Count/Timer (default 1).
     */
    fun normalize(draft: HabitDraft): HabitDraft {
        val trimmed = draft.copy(
            name = draft.name.trim(),
            targetLabel = draft.targetLabel?.trim(),
            baselineLabel = draft.baselineLabel?.trim()
        )
        return when (draft.type) {
            HabitType.CHECK -> trimmed.copy(step = null)

            HabitType.COUNT -> trimmed.sharedUnit().copy(step = draft.step ?: DEFAULT_STEP)

            HabitType.TIMER -> trimmed.copy(
                targetUnit = AmountUnit.MINUTES,
                targetLabel = null,
                baselineUnit = AmountUnit.MINUTES,
                baselineLabel = null,
                step = draft.step ?: DEFAULT_STEP
            )

            HabitType.AVOID -> trimmed.sharedUnit().copy(step = null)
        }
    }

    /** Errors in field order; empty when the draft can be saved. */
    fun validate(draft: HabitDraft): List<ValidationError> {
        val d = normalize(draft)
        return buildList {
            when {
                d.name.isEmpty() -> add(ValidationError.NAME_EMPTY)
                d.name.length > NAME_MAX_LENGTH -> add(ValidationError.NAME_TOO_LONG)
            }
            addAll(amountErrors(d))
            addAll(unitLabelErrors(d))
            if ((d.type == HabitType.COUNT || d.type == HabitType.TIMER) && (d.step ?: 0) < 1) {
                add(ValidationError.STEP_INVALID)
            }
        }
    }

    private fun HabitDraft.sharedUnit() =
        copy(baselineUnit = targetUnit, baselineLabel = targetLabel)

    private fun amountErrors(d: HabitDraft): List<ValidationError> {
        val minimum = if (d.type == HabitType.AVOID) 0 else 1
        val target = d.targetValue?.takeIf { it >= minimum }
        val baseline = d.baselineValue?.takeIf { it >= minimum }
        return buildList {
            if (target == null) add(ValidationError.TARGET_EMPTY)
            if (baseline == null) add(ValidationError.BASELINE_EMPTY)
            if (target != null && baseline != null) {
                when (d.type) {
                    HabitType.COUNT, HabitType.TIMER ->
                        if (baseline > target) add(ValidationError.BASELINE_ABOVE_TARGET)

                    HabitType.AVOID ->
                        if (baseline < target) add(ValidationError.LIMIT_BELOW_TARGET)

                    HabitType.CHECK -> Unit
                }
            }
        }
    }

    private fun unitLabelErrors(d: HabitDraft): List<ValidationError> {
        val labels = buildList {
            if (d.targetUnit == AmountUnit.CUSTOM) add(d.targetLabel.orEmpty())
            if (d.type == HabitType.CHECK &&
                d.baselineUnit == AmountUnit.CUSTOM
            ) {
                add(d.baselineLabel.orEmpty())
            }
        }
        return labels.mapNotNull { label ->
            when {
                label.isEmpty() -> ValidationError.CUSTOM_UNIT_EMPTY
                label.length > UNIT_LABEL_MAX_LENGTH -> ValidationError.CUSTOM_UNIT_TOO_LONG
                else -> null
            }
        }.distinct()
    }
}
