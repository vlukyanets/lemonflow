package io.github.vlukyanets.lemonflow.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Maps instants to logical days and back (spec: Data model, Logical day; Day start).
 *
 * Logical date D runs from D at [closeTime] (the "open") to D+1 at [closeTime] (the "close"),
 * in [zone]. With close 03:00, 02:30 on 28 Nov belongs to 27 Nov.
 *
 * Every boundary is computed through [closeOf], so the mapping stays consistent across DST
 * gaps and overlaps: a local time inside a gap is shifted later by the gap's length, an
 * ambiguous one takes the earlier offset (java.time rules).
 */
data class DayBoundaries(
    val zone: ZoneId,
    val dayStart: LocalTime,
    val closeTime: LocalTime,
) {
    /** The instant logical [date] ends and the next one begins. */
    fun closeOf(date: LocalDate): Instant =
        date.plusDays(1).atTime(closeTime).atZone(zone).toInstant()

    /** The instant logical [date] begins. */
    fun openOf(date: LocalDate): Instant = closeOf(date.minusDays(1))

    /** The logical date [instant] belongs to. */
    fun dateOf(instant: Instant): LocalDate {
        val calendarDate = instant.atZone(zone).toLocalDate()
        return if (instant >= openOf(calendarDate)) calendarDate else calendarDate.minusDays(1)
    }

    /**
     * When the owner usually starts on logical [date]. A day start earlier on the clock than
     * the close time falls on the next calendar date (start 02:00, close 03:00 -> D+1 02:00).
     */
    fun dayStartOf(date: LocalDate): Instant {
        val calendarDate = if (dayStart >= closeTime) date else date.plusDays(1)
        return calendarDate.atTime(dayStart).atZone(zone).toInstant()
    }
}
