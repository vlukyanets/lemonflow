package io.github.vlukyanets.lemonflow.domain.time

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class UserDayCalculatorTest {
    private val kyiv = ZoneId.of("Europe/Kyiv")

    private fun kyivInstant(text: String): Instant =
        ZonedDateTime.of(java.time.LocalDateTime.parse(text), kyiv).toInstant()

    @Test
    fun earlyMorningBeforeDayEndBelongsToPreviousDate() {
        val date = UserDayCalculator.userDate(
            kyivInstant("2026-11-10T01:30:00"),
            kyiv,
            dayEndHour = 3
        )
        assertEquals(LocalDate.parse("2026-11-09"), date)
    }

    @Test
    fun lastSecondBeforeDayEndStillPreviousDate() {
        val date = UserDayCalculator.userDate(
            kyivInstant("2026-11-10T02:59:59"),
            kyiv,
            dayEndHour = 3
        )
        assertEquals(LocalDate.parse("2026-11-09"), date)
    }

    @Test
    fun dayEndMomentStartsNewDate() {
        val date = UserDayCalculator.userDate(
            kyivInstant("2026-11-10T03:00:00"),
            kyiv,
            dayEndHour = 3
        )
        assertEquals(LocalDate.parse("2026-11-10"), date)
    }

    @Test
    fun dayEndZeroIsPlainMidnight() {
        assertEquals(
            LocalDate.parse("2026-11-09"),
            UserDayCalculator.userDate(kyivInstant("2026-11-09T23:59:59"), kyiv, dayEndHour = 0)
        )
        assertEquals(
            LocalDate.parse("2026-11-10"),
            UserDayCalculator.userDate(kyivInstant("2026-11-10T00:00:00"), kyiv, dayEndHour = 0)
        )
    }

    @Test
    fun springGapDayEndsAtFirstInstantAfterGap() {
        // On 2027-03-28 Kyiv jumps from 03:00 EET straight to 04:00 EEST (01:00Z).
        val end = UserDayCalculator.dayEnd(LocalDate.parse("2027-03-27"), kyiv, dayEndHour = 3)
        assertEquals(Instant.parse("2027-03-28T01:00:00Z"), end)
        assertEquals(
            LocalDate.parse("2027-03-27"),
            UserDayCalculator.userDate(Instant.parse("2027-03-28T00:59:59Z"), kyiv, dayEndHour = 3)
        )
        assertEquals(
            LocalDate.parse("2027-03-28"),
            UserDayCalculator.userDate(Instant.parse("2027-03-28T01:00:00Z"), kyiv, dayEndHour = 3)
        )
    }

    @Test
    fun autumnOverlapDayEndsAtFirstOccurrence() {
        // On 2026-10-25 Kyiv goes back from 04:00 EEST to 03:00 EET, so 03:00-04:00 happens twice.
        val end = UserDayCalculator.dayEnd(LocalDate.parse("2026-10-24"), kyiv, dayEndHour = 3)
        assertEquals(Instant.parse("2026-10-25T00:00:00Z"), end)
        assertEquals(
            LocalDate.parse("2026-10-24"),
            UserDayCalculator.userDate(Instant.parse("2026-10-24T23:59:59Z"), kyiv, dayEndHour = 3)
        )
        // Second occurrence of 03:30 (EET) is still the new date.
        assertEquals(
            LocalDate.parse("2026-10-25"),
            UserDayCalculator.userDate(Instant.parse("2026-10-25T01:30:00Z"), kyiv, dayEndHour = 3)
        )
    }

    @Test
    fun dayStartEqualsPreviousDayEnd() {
        val date = LocalDate.parse("2026-11-09")
        assertEquals(
            UserDayCalculator.dayEnd(date.minusDays(1), kyiv, dayEndHour = 3),
            UserDayCalculator.dayStart(date, kyiv, dayEndHour = 3)
        )
    }
}
