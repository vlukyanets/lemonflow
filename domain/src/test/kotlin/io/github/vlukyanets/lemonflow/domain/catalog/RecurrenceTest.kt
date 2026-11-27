package io.github.vlukyanets.lemonflow.domain.catalog

import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RecurrenceTest {

    private val monday = LocalDate.of(2026, 11, 23)
    private fun week(recurrence: Recurrence) = (0L..6L).map { recurrence.isDue(monday.plusDays(it)) }

    @Test
    fun `daily is always due`() {
        assertEquals(List(7) { true }, week(Recurrence.Daily))
    }

    @Test
    fun `weekdays are due on the chosen days`() {
        val workdays = Recurrence.Weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.FRIDAY))
        assertEquals(listOf(true, false, true, false, true, false, false), week(workdays))
    }

    @Test
    fun `every N days counts from the anchor and never before it`() {
        val everyThird = Recurrence.EveryNDays(3, anchor = monday.plusDays(1))
        assertEquals(listOf(false, true, false, false, true, false, false), week(everyThird))
    }

    @Test
    fun `invalid rules are rejected`() {
        assertFailsWith<IllegalArgumentException> { Recurrence.Weekdays(emptySet()) }
        assertFailsWith<IllegalArgumentException> { Recurrence.EveryNDays(0, monday) }
    }
}
