package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.ValidationError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitValidatorTest {
    private fun check(target: Int? = 30, baseline: Int? = 1) = HabitDraft(
        name = "Reading",
        type = HabitType.CHECK,
        targetValue = target,
        targetUnit = AmountUnit.MINUTES,
        baselineValue = baseline,
        baselineUnit = AmountUnit.PAGES
    )

    private fun count(target: Int? = 2500, baseline: Int? = 500, step: Int? = 250) = HabitDraft(
        name = "Water",
        type = HabitType.COUNT,
        targetValue = target,
        targetUnit = AmountUnit.CUSTOM,
        targetLabel = "ml",
        baselineValue = baseline,
        baselineUnit = AmountUnit.TIMES,
        step = step
    )

    private fun avoid(target: Int? = 0, baseline: Int? = 1) = HabitDraft(
        name = "Added sugar",
        type = HabitType.AVOID,
        targetValue = target,
        targetUnit = AmountUnit.TIMES,
        baselineValue = baseline,
        baselineUnit = AmountUnit.TIMES,
        step = 5
    )

    private fun errors(draft: HabitDraft) = HabitValidator.validate(draft)

    @Test
    fun validDraftsOfEveryTypePass() {
        assertEquals(emptyList<ValidationError>(), errors(check()))
        assertEquals(emptyList<ValidationError>(), errors(count()))
        assertEquals(emptyList<ValidationError>(), errors(avoid()))
        val timer =
            HabitDraft(
                "Meditation",
                HabitType.TIMER,
                10,
                AmountUnit.MINUTES,
                null,
                1,
                AmountUnit.MINUTES
            )
        assertEquals(emptyList<ValidationError>(), errors(timer))
    }

    @Test
    fun nameIsTrimmedAndLimitedTo120Chars() {
        assertEquals(listOf(ValidationError.NAME_EMPTY), errors(check().copy(name = "   ")))
        assertEquals(
            emptyList<ValidationError>(),
            errors(
                check().copy(
                    name =
                        "  " + "a".repeat(120) + "  "
                )
            )
        )
        assertEquals(
            listOf(ValidationError.NAME_TOO_LONG),
            errors(check().copy(name = "a".repeat(121)))
        )
        assertEquals("Reading", HabitValidator.normalize(check().copy(name = "  Reading ")).name)
    }

    @Test
    fun emptyAmountsArePointedOut() {
        assertEquals(
            listOf(ValidationError.TARGET_EMPTY, ValidationError.BASELINE_EMPTY),
            errors(check(target = null, baseline = null))
        )
    }

    @Test
    fun checkAmountsMustBePositiveButUnitsMayDiffer() {
        assertEquals(listOf(ValidationError.TARGET_EMPTY), errors(check(target = 0)))
        assertEquals(listOf(ValidationError.BASELINE_EMPTY), errors(check(baseline = 0)))
        val normalized = HabitValidator.normalize(check())
        assertEquals(AmountUnit.MINUTES, normalized.targetUnit)
        assertEquals(AmountUnit.PAGES, normalized.baselineUnit)
        assertNull("Check habits have no step", normalized.step)
    }

    @Test
    fun countUsesOneUnitAndBaselineNotAboveTarget() {
        val normalized = HabitValidator.normalize(count())
        assertEquals(AmountUnit.CUSTOM, normalized.baselineUnit)
        assertEquals("ml", normalized.baselineLabel)
        assertEquals(
            listOf(ValidationError.BASELINE_ABOVE_TARGET),
            errors(count(target = 2000, baseline = 3000))
        )
        assertEquals(emptyList<ValidationError>(), errors(count(target = 500, baseline = 500)))
    }

    @Test
    fun countStepDefaultsToOneAndMustBePositive() {
        assertEquals(1, HabitValidator.normalize(count(step = null)).step)
        assertEquals(listOf(ValidationError.STEP_INVALID), errors(count(step = 0)))
    }

    @Test
    fun timerAlwaysUsesMinutes() {
        val timer = HabitDraft(
            name = "Meditation",
            type = HabitType.TIMER,
            targetValue = 10,
            targetUnit = AmountUnit.PAGES,
            baselineValue = 20,
            baselineUnit = AmountUnit.STEPS
        )
        val normalized = HabitValidator.normalize(timer)
        assertEquals(AmountUnit.MINUTES, normalized.targetUnit)
        assertEquals(AmountUnit.MINUTES, normalized.baselineUnit)
        assertEquals(1, normalized.step)
        assertEquals(listOf(ValidationError.BASELINE_ABOVE_TARGET), errors(timer))
    }

    @Test
    fun avoidAllowsZeroLimitAndNeedsHardDayLimitNotBelow() {
        assertEquals(emptyList<ValidationError>(), errors(avoid(target = 0, baseline = 0)))
        assertEquals(
            listOf(ValidationError.LIMIT_BELOW_TARGET),
            errors(avoid(target = 2, baseline = 1))
        )
        assertEquals(listOf(ValidationError.TARGET_EMPTY), errors(avoid(target = null)))
        assertNull("Avoid habits always change by 1", HabitValidator.normalize(avoid()).step)
    }

    @Test
    fun customUnitNeedsATrimmedLabelUpTo30Chars() {
        assertEquals(
            listOf(ValidationError.CUSTOM_UNIT_EMPTY),
            errors(count().copy(targetLabel = "  "))
        )
        assertEquals(
            listOf(ValidationError.CUSTOM_UNIT_TOO_LONG),
            errors(count().copy(targetLabel = "u".repeat(31)))
        )
        assertTrue(errors(count().copy(targetLabel = " " + "u".repeat(30) + " ")).isEmpty())
        val checkWithCustomBaseline = check().copy(
            baselineUnit = AmountUnit.CUSTOM,
            baselineLabel = null
        )
        assertEquals(listOf(ValidationError.CUSTOM_UNIT_EMPTY), errors(checkWithCustomBaseline))
    }
}
