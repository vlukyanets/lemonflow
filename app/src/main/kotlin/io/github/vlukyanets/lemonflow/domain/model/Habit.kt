package io.github.vlukyanets.lemonflow.domain.model

import java.time.Instant

/** Client-generated UUID of a habit (constitution I). */
@JvmInline
value class HabitId(val value: String)

/** Fixed when the habit is created (FR-002a). */
enum class HabitType { CHECK, COUNT, TIMER, AVOID }

enum class AmountUnit { MINUTES, TIMES, PAGES, STEPS, CUSTOM }

/** A whole number plus its unit; [customLabel] is set only for [AmountUnit.CUSTOM]. */
data class Amount(val value: Int, val unit: AmountUnit, val customLabel: String? = null)

/**
 * A habit with a Target (a good day) and a Baseline (the rescue minimum for a hard day).
 * For [HabitType.AVOID] both are limits: Target is the good-day limit, Baseline the hard-day one.
 * [step] is set for COUNT (in the habit's unit) and TIMER (in minutes) only.
 */
data class Habit(
    val id: HabitId,
    val name: String,
    val type: HabitType,
    val target: Amount,
    val baseline: Amount,
    val step: Int?,
    val isCore: Boolean,
    val createdAt: Instant,
    val archivedAt: Instant?
) {
    val isArchived: Boolean get() = archivedAt != null
}

/** What the editor submits; empty number fields are null so validation can point at them. */
data class HabitDraft(
    val name: String,
    val type: HabitType,
    val targetValue: Int?,
    val targetUnit: AmountUnit,
    val targetLabel: String? = null,
    val baselineValue: Int?,
    val baselineUnit: AmountUnit,
    val baselineLabel: String? = null,
    val step: Int? = null,
    val isCore: Boolean = false
)
