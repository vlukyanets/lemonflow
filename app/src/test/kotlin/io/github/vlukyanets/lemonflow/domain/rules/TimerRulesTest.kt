package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.Mark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerRulesTest {
    private val start = Instant.parse("2026-11-09T08:00:00Z")
    private val dayEnd = Instant.parse("2026-11-10T01:00:00Z") // 03:00 Kyiv

    @Test
    fun elapsedAddsTheRunningTimeToTheStoredAmount() {
        assertEquals(
            120 + 252,
            TimerRules.elapsedSeconds(120, start, start.plusSeconds(252), dayEnd)
        )
    }

    @Test
    fun pausedTimerReturnsTheStoredAmount() {
        assertEquals(90, TimerRules.elapsedSeconds(90, null, start, dayEnd))
    }

    @Test
    fun clockJumpBackwardsAddsNothingAndNeverGoesNegative() {
        assertEquals(30, TimerRules.elapsedSeconds(30, start, start.minusSeconds(3600), dayEnd))
        assertEquals(0, TimerRules.elapsedSeconds(0, start, start.minusSeconds(1), dayEnd))
    }

    @Test
    fun elapsedIsCappedAtTheDayEnd() {
        val late = dayEnd.plusSeconds(7200)
        val expected = (dayEnd.epochSecond - start.epochSecond).toInt()
        assertEquals(expected, TimerRules.elapsedSeconds(0, start, late, dayEnd))
    }

    @Test
    fun settleStopsOnlyTimersWhoseDayEnded() {
        val yesterdayStart = Instant.parse("2026-11-08T20:00:00Z")
        val yesterday = record("a", LocalDate.parse("2026-11-08"), yesterdayStart, 60)
        val today = record("b", LocalDate.parse("2026-11-09"), start, 0)
        val yesterdayEnd = Instant.parse("2026-11-09T01:00:00Z")
        val now = start.plusSeconds(600)

        val settled = TimerRules.settle(listOf(yesterday, today), now) { date ->
            if (date == yesterday.date) yesterdayEnd else dayEnd
        }

        assertEquals(1, settled.size)
        val stopped = settled.single()
        assertEquals(HabitId("a"), stopped.habitId)
        assertNull(stopped.timerStartedAt)
        // Folds in only the 5 hours up to the end of its own day.
        assertEquals(60 + 5 * 3600, stopped.amount)
    }

    @Test
    fun settleIgnoresPausedRecords() {
        val paused = record("a", LocalDate.parse("2026-11-08"), null, 60)
        assertTrue(TimerRules.settle(listOf(paused), dayEnd.plusSeconds(1)) { dayEnd }.isEmpty())
    }

    @Test
    fun twoRunningTimersStayIndependent() {
        val first = record("a", LocalDate.parse("2026-11-09"), start, 100)
        val second = record("b", LocalDate.parse("2026-11-09"), start.plusSeconds(60), 0)
        val now = start.plusSeconds(120)

        assertEquals(
            220,
            TimerRules.elapsedSeconds(first.amount, first.timerStartedAt, now, dayEnd)
        )
        assertEquals(
            60,
            TimerRules.elapsedSeconds(second.amount, second.timerStartedAt, now, dayEnd)
        )

        val settled = TimerRules.settle(listOf(first, second), dayEnd.plusSeconds(5)) { dayEnd }
        assertEquals(
            listOf(
                100 + (dayEnd.epochSecond - start.epochSecond).toInt(),
                (dayEnd.epochSecond - start.epochSecond - 60).toInt()
            ),
            settled.map { it.amount }
        )
    }

    private fun record(habit: String, date: LocalDate, startedAt: Instant?, amount: Int) =
        DailyRecord(
            id = "rec-$habit",
            habitId = HabitId(habit),
            date = date,
            mark = Mark.OPEN,
            amount = amount,
            timerStartedAt = startedAt,
            updatedAt = start,
            zoneId = ZoneId.of("Europe/Kyiv")
        )
}
