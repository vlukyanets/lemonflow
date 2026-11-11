package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import io.github.vlukyanets.lemonflow.testing.Habits
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeekProgressTest {
    private val zone = ZoneId.of("Europe/Kyiv")
    private val today = LocalDate.parse("2026-11-09")
    private val bounds = DayBounds { date ->
        UserDayCalculator.dayStart(date, zone, 3) to UserDayCalculator.dayEnd(date, zone, 3)
    }
    private val reading = Habits.reading()
    private val sugar = Habits.sugar()

    @Test
    fun avoidDefaultCountsAsTargetToday() {
        val day = WeekProgress.summary(today, listOf(reading, sugar), emptyList(), today, bounds)

        assertEquals(1, day.completed)
        assertEquals(2, day.active)
        assertFalse(day.nothingRecorded)
        assertTrue(day.isToday)
    }

    @Test
    fun avoidDefaultOnAPastDateNeedsAnotherHabitsRecord() {
        val past = today.minusDays(2)

        val empty = WeekProgress.summary(past, listOf(reading, sugar), emptyList(), today, bounds)
        assertEquals(0, empty.completed)
        assertTrue(empty.nothingRecorded)

        val withReading = WeekProgress.summary(
            past,
            listOf(reading, sugar),
            listOf(record(reading, past, Mark.TARGET)),
            today,
            bounds
        )
        assertEquals(2, withReading.completed)
        assertFalse(withReading.nothingRecorded)
    }

    @Test
    fun openRecordStillMakesTheDayRecorded() {
        val past = today.minusDays(1)
        val day = WeekProgress.summary(
            past,
            listOf(reading, sugar),
            listOf(record(reading, past, Mark.OPEN)),
            today,
            bounds
        )
        assertEquals(1, day.completed) // only the Avoid default
        assertFalse(day.nothingRecorded)
    }

    @Test
    fun avoidAboveTheLimitIsNotCompleted() {
        val day = WeekProgress.summary(
            today,
            listOf(sugar),
            listOf(record(sugar, today, Mark.OPEN, amount = 3)),
            today,
            bounds
        )
        assertEquals(0, day.completed)
    }

    @Test
    fun activeOnDateFollowsCreationAndArchive() {
        val (start, end) = bounds.of(today)
        val createdLater = reading.copy(createdAt = end)
        val createdBeforeEnd = reading.copy(createdAt = end.minusSeconds(1))
        val archivedBefore = reading.copy(archivedAt = start)
        val archivedDuring = reading.copy(archivedAt = start.plusSeconds(1))

        assertFalse(WeekProgress.isActiveOn(createdLater, start, end))
        assertTrue(WeekProgress.isActiveOn(createdBeforeEnd, start, end))
        assertFalse(WeekProgress.isActiveOn(archivedBefore, start, end))
        assertTrue(WeekProgress.isActiveOn(archivedDuring, start, end))
    }

    @Test
    fun weekCoversTheLastSevenUserDatesEndingToday() {
        val records = listOf(
            record(reading, today.minusDays(6), Mark.TARGET),
            record(reading, today.minusDays(1), Mark.BASELINE)
        )

        val week = WeekProgress.week(today, listOf(reading, sugar), records, bounds)

        assertEquals((6L downTo 0L).map { today.minusDays(it) }, week.map { it.date })
        assertEquals(
            listOf(false, true, true, true, true, false, false),
            week.map { it.nothingRecorded }
        )
        assertEquals(
            listOf(false, false, false, false, false, false, true),
            week.map {
                it.isToday
            }
        )
        assertEquals(2, week[0].completed)
        assertEquals(2, week[5].completed) // Baseline counts (FR-009) plus the Avoid default
        assertTrue(week[2].nothingRecorded)
        assertEquals(1, week[6].completed) // today: only the Avoid default
        assertTrue(week[6].isToday)
    }

    @Test
    fun habitsNotActiveOnADateAreLeftOut() {
        val (start, _) = bounds.of(today)
        val newHabit: Habit = reading.copy(createdAt = start.plusSeconds(60))
        val yesterday = today.minusDays(1)

        val day = WeekProgress.summary(
            yesterday,
            listOf(newHabit, sugar),
            listOf(record(sugar, yesterday, Mark.TARGET)),
            today,
            bounds
        )
        assertEquals(1, day.active)
        assertEquals(1, day.completed)
    }

    @Test
    fun pastDayListsActiveHabitsWithEffectiveMarks() {
        val past = today.minusDays(3)
        val day = WeekProgress.pastDay(
            past,
            listOf(reading, sugar, Habits.water()),
            listOf(record(Habits.water(), past, Mark.BASELINE, amount = 750)),
            bounds
        )

        assertFalse(day.nothingRecorded)
        assertEquals(listOf(Mark.OPEN, Mark.TARGET, Mark.BASELINE), day.rows.map { it.mark })
        assertEquals(750, day.rows[2].amount)

        val empty = WeekProgress.pastDay(past, listOf(reading, sugar), emptyList(), bounds)
        assertTrue(empty.nothingRecorded)
        assertEquals(listOf(Mark.OPEN, Mark.OPEN), empty.rows.map { it.mark })
    }

    private fun record(habit: Habit, date: LocalDate, mark: Mark, amount: Int = 0) = DailyRecord(
        id = "rec-${habit.id.value}-$date",
        habitId = habit.id,
        date = date,
        mark = mark,
        amount = amount,
        timerStartedAt = null,
        updatedAt = Instant.parse("2026-11-09T08:00:00Z"),
        zoneId = zone
    )
}
