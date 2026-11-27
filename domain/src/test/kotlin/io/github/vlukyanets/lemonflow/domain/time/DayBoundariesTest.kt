package io.github.vlukyanets.lemonflow.domain.time

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class DayBoundariesTest {

    private val utc = DayBoundaries(ZoneOffset.UTC, dayStart = LocalTime.of(9, 0), closeTime = LocalTime.of(3, 0))
    private val berlin = ZoneId.of("Europe/Berlin")

    private fun utc(dateTime: String): Instant = Instant.parse("${dateTime}Z")

    @Test
    fun `instant before close belongs to the previous logical day`() {
        assertEquals(LocalDate.of(2026, 11, 27), utc.dateOf(utc("2026-11-28T02:30:00")))
        assertEquals(LocalDate.of(2026, 11, 28), utc.dateOf(utc("2026-11-28T03:00:00")))
        assertEquals(LocalDate.of(2026, 11, 28), utc.dateOf(utc("2026-11-28T23:59:00")))
    }

    @Test
    fun `open and close of a logical day`() {
        val date = LocalDate.of(2026, 11, 27)
        assertEquals(utc("2026-11-27T03:00:00"), utc.openOf(date))
        assertEquals(utc("2026-11-28T03:00:00"), utc.closeOf(date))
    }

    @Test
    fun `close at midnight makes logical days calendar days`() {
        val midnight = utc.copy(closeTime = LocalTime.MIDNIGHT)
        assertEquals(LocalDate.of(2026, 11, 28), midnight.dateOf(utc("2026-11-28T00:00:00")))
        assertEquals(LocalDate.of(2026, 11, 27), midnight.dateOf(utc("2026-11-27T23:59:59")))
    }

    @Test
    fun `day start after close falls on the same date, before close on the next one`() {
        val date = LocalDate.of(2026, 11, 27)
        assertEquals(utc("2026-11-27T09:00:00"), utc.dayStartOf(date))
        assertEquals(utc("2026-11-28T02:00:00"), utc.copy(dayStart = LocalTime.of(2, 0)).dayStartOf(date))
    }

    @Test
    fun `DST spring forward makes the logical day 23 hours`() {
        val days = DayBoundaries(berlin, LocalTime.of(9, 0), LocalTime.of(5, 0))
        val date = LocalDate.of(2026, 3, 28) // clocks go 02:00 -> 03:00 on 29 March
        assertEquals(Duration.ofHours(23), Duration.between(days.openOf(date), days.closeOf(date)))
    }

    @Test
    fun `DST fall back makes the logical day 25 hours`() {
        val days = DayBoundaries(berlin, LocalTime.of(9, 0), LocalTime.of(5, 0))
        val date = LocalDate.of(2026, 10, 24) // clocks go 03:00 -> 02:00 on 25 October
        assertEquals(Duration.ofHours(25), Duration.between(days.openOf(date), days.closeOf(date)))
    }

    @Test
    fun `close time inside a DST gap stays consistent between dateOf and closeOf`() {
        // 02:30 does not exist on 29 March in Berlin; java.time shifts it to 03:30 CEST (01:30Z).
        val days = DayBoundaries(berlin, LocalTime.of(9, 0), LocalTime.of(2, 30))
        val date = LocalDate.of(2026, 3, 28)
        val close = days.closeOf(date)

        assertEquals(utc("2026-03-29T01:30:00"), close)
        // 03:10 CEST on the clock is already past 02:30, but the day has not closed yet.
        assertEquals(date, days.dateOf(utc("2026-03-29T01:10:00")))
        assertEquals(date, days.dateOf(close.minusNanos(1)))
        assertEquals(date.plusDays(1), days.dateOf(close))
    }
}
