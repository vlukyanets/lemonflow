package io.github.vlukyanets.lemonflow.domain.model

import java.time.Instant
import java.time.LocalDate

/**
 * One habit on today's date as stored: [amount] excludes a running timer's current run, which
 * started at [timerStartedAt]; the screen adds it with `TimerRules.elapsedSeconds`.
 * A habit without a record has amount 0 and its default mark.
 */
data class TodayRow(
    val habit: Habit,
    val mark: Mark,
    val amount: Int,
    val timerStartedAt: Instant? = null
) {
    val timerRunning: Boolean get() = timerStartedAt != null
}

/** Everything the Today screen needs for the current user date. */
data class TodayState(
    val date: LocalDate,
    val dayEnd: Instant,
    val dayEndHour: Int,
    val hardDay: Boolean,
    val rows: List<TodayRow>
)

/** One cell of the 7-day strip (FR-006a). */
data class DaySummary(
    val date: LocalDate,
    val completed: Int,
    val active: Int,
    val nothingRecorded: Boolean,
    val isToday: Boolean
)

/** A past date, read-only (FR-006b). [nothingRecorded] is true when no records exist at all. */
data class PastDay(val date: LocalDate, val rows: List<PastDayRow>, val nothingRecorded: Boolean)

data class PastDayRow(val habit: Habit, val mark: Mark, val amount: Int)
