package io.github.vlukyanets.lemonflow.domain.model

/** Field problems in a [HabitDraft]; each maps to a gentle string in en/ru/uk (FR-005). */
enum class ValidationError {
    NAME_EMPTY,
    NAME_TOO_LONG,
    TARGET_EMPTY,
    BASELINE_EMPTY,
    BASELINE_ABOVE_TARGET,
    LIMIT_BELOW_TARGET,
    CUSTOM_UNIT_EMPTY,
    CUSTOM_UNIT_TOO_LONG,
    STEP_INVALID
}

/** Result of saving a habit. Kotlin's own Result cannot carry validation errors. */
sealed interface SaveResult<out T> {
    data class Saved<T>(val value: T) : SaveResult<T>

    data class Invalid(val errors: List<ValidationError>) : SaveResult<Nothing>
}
