package io.github.vlukyanets.lemonflow.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The user's day runs from `dayEndHour:00` on one calendar date to the same time on the next
 * and is named by the date it starts on (spec FR-018, research R3).
 *
 * `ZonedDateTime.of` moves a time inside a DST gap forward to the first instant after the gap
 * and picks the earlier offset in an overlap - exactly the spec's day-end rule.
 */
object UserDayCalculator {
    /** "Day ends at" default (FR-018). */
    const val DEFAULT_DAY_END_HOUR = 3

    /** The latest whole hour "Day ends at" may take (FR-018). */
    const val LATEST_DAY_END_HOUR = 6

    /** Whole hours the user may pick for "Day ends at" (FR-018). */
    val DAY_END_HOURS = 0..LATEST_DAY_END_HOUR

    fun userDate(instant: Instant, zone: ZoneId, dayEndHour: Int): LocalDate {
        val local = instant.atZone(zone)
        return if (local.toLocalTime() < LocalTime.of(dayEndHour, 0)) {
            local.toLocalDate().minusDays(1)
        } else {
            local.toLocalDate()
        }
    }

    fun dayStart(date: LocalDate, zone: ZoneId, dayEndHour: Int): Instant =
        ZonedDateTime.of(date, LocalTime.of(dayEndHour, 0), zone).toInstant()

    fun dayEnd(date: LocalDate, zone: ZoneId, dayEndHour: Int): Instant =
        dayStart(date.plusDays(1), zone, dayEndHour)
}
