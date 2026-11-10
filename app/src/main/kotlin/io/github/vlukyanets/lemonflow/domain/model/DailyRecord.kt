package io.github.vlukyanets.lemonflow.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How a habit went on one user date. BASELINE always counts as complete (FR-009). */
enum class Mark { OPEN, TARGET, BASELINE }

/**
 * One habit on one user date (the spec's DailyHabitInstance). [amount] is units for COUNT,
 * accumulated seconds for TIMER and the count for AVOID; CHECK keeps 0.
 */
data class DailyRecord(
    val id: String,
    val habitId: HabitId,
    val date: LocalDate,
    val mark: Mark,
    val amount: Int,
    val timerStartedAt: Instant?,
    val updatedAt: Instant,
    val zoneId: ZoneId
)
