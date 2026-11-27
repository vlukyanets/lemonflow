package io.github.vlukyanets.lemonflow.domain.schedule

import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.min
import io.github.vlukyanets.lemonflow.domain.plannedId
import java.time.Duration
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScheduleEngineTest {

    private val a = plannedId("A")
    private val b = plannedId("B")
    private val c = plannedId("C")
    private val d = plannedId("D")

    private fun item(
        id: io.github.vlukyanets.lemonflow.domain.model.PlannedItemId,
        planned: Long,
        tracked: Long = 0,
        before: Long = 5,
        after: Long = 5,
    ) = SchedulableItem(id, min(planned), min(tracked), min(before), min(after))

    private fun build(
        now: String,
        items: List<SchedulableItem>,
        focus: FocusState = FocusState.NotStarted,
        dayStart: String = "09:00",
        close: String = "19:00",
    ) = ScheduleEngine.build(ScheduleInput(at(now), at(dayStart), at(close), items, focus))

    private fun Schedule.times(): List<Pair<Instant, Instant>> = slots.map { it.start to it.end }

    @Test
    fun `spec example - max of buffers, overflow with partial fit`() {
        val schedule = build(
            now = "16:00",
            items = listOf(item(b, 60, before = 5, after = 5), item(c, 90, before = 15), item(d, 45)),
            focus = FocusState.Stopped(end = at("15:55"), lastItem = a, bufferAfter = min(10), paused = null),
        )

        assertEquals(listOf(at("16:05") to at("17:05"), at("17:20") to at("18:50")), schedule.times())
        assertEquals(listOf(OverflowEntry(d, remaining = min(45), fits = min(5))), schedule.overflow)
        assertEquals(min(40), schedule.overflowAmount)
        assertEquals(Duration.ZERO, schedule.freeTime)
    }

    @Test
    fun `nothing tracked yet - starts at day start with no buffer before the first item`() {
        val schedule = build(now = "07:00", items = listOf(item(b, 60, before = 15), item(c, 30, before = 10)))

        assertEquals(listOf(at("09:00") to at("10:00"), at("10:10") to at("10:40")), schedule.times())
    }

    @Test
    fun `nothing tracked yet and day start passed - starts now`() {
        val schedule = build(now = "11:20", items = listOf(item(b, 60, before = 15)))

        assertEquals(listOf(at("11:20") to at("12:20")), schedule.times())
    }

    @Test
    fun `running item goes first wherever it sits in the order`() {
        val schedule = build(
            now = "10:00",
            items = listOf(item(b, 30, after = 5), item(c, 60, tracked = 20, after = 10), item(d, 30, before = 5)),
            focus = FocusState.RunningItem(c),
        )

        assertEquals(listOf(c, b, d), schedule.slots.map { it.item })
        assertEquals(listOf(at("10:00") to at("10:40"), at("10:50") to at("11:20"), at("11:25") to at("11:55")), schedule.times())
        assertTrue(schedule.slots.first().running)
    }

    @Test
    fun `running item over its estimate takes no time and reports the overrun`() {
        val schedule = build(
            now = "10:00",
            items = listOf(item(b, 30, tracked = 45, after = 10), item(c, 30, before = 5)),
            focus = FocusState.RunningItem(b),
        )

        assertEquals(Slot(b, at("10:00"), at("10:00"), running = true, overBy = min(15)), schedule.slots[0])
        assertEquals(at("10:10") to at("10:40"), schedule.times()[1])
    }

    @Test
    fun `idle item with used-up estimate takes no time and no buffers`() {
        val schedule = build(
            now = "10:00",
            items = listOf(item(b, 30, after = 5), item(c, 30, tracked = 30, before = 30, after = 30), item(d, 30, before = 5)),
        )

        assertEquals(listOf(c), schedule.estimateUsedUp)
        assertEquals(listOf(at("10:00") to at("10:30"), at("10:35") to at("11:05")), schedule.times())
    }

    @Test
    fun `running interruption - the paused item goes first and resumes with no gap`() {
        val schedule = build(
            now = "10:35",
            items = listOf(item(c, 30, before = 15), item(b, 60, tracked = 20, before = 15, after = 5)),
            focus = FocusState.RunningOther(paused = b),
        )

        assertEquals(listOf(b, c), schedule.slots.map { it.item })
        assertEquals(listOf(at("10:35") to at("11:15"), at("11:30") to at("12:00")), schedule.times())
    }

    @Test
    fun `ended interruption not resumed yet - the paused item still goes first with no gap`() {
        val schedule = build(
            now = "10:40",
            items = listOf(item(c, 30), item(b, 60, tracked = 20, before = 15)),
            focus = FocusState.Stopped(end = at("10:35"), lastItem = null, bufferAfter = Duration.ZERO, paused = b),
        )

        assertEquals(at("10:40") to at("11:20"), schedule.times().first())
        assertEquals(b, schedule.slots.first().item)
    }

    @Test
    fun `after unplanned work a different item gets only its own before-buffer`() {
        val schedule = build(
            now = "10:00",
            items = listOf(item(c, 30, before = 15)),
            focus = FocusState.RunningOther(paused = null),
        )

        assertEquals(listOf(at("10:15") to at("10:45")), schedule.times())
    }

    @Test
    fun `interruption whose paused item is done - next item gets its before-buffer`() {
        val schedule = build(
            now = "10:35",
            items = listOf(item(c, 30, before = 10)),
            focus = FocusState.Stopped(end = at("10:35"), lastItem = null, bufferAfter = Duration.ZERO, paused = b),
        )

        assertEquals(listOf(at("10:45") to at("11:15")), schedule.times())
    }

    @Test
    fun `resuming the item that was last worked on has no gap`() {
        val schedule = build(
            now = "10:30",
            items = listOf(item(b, 60, tracked = 30, before = 10, after = 10)),
            focus = FocusState.Stopped(end = at("10:30"), lastItem = b, bufferAfter = min(10), paused = null),
        )

        assertEquals(listOf(at("10:30") to at("11:00")), schedule.times())
    }

    @Test
    fun `idle time absorbs the buffer`() {
        val schedule = build(
            now = "10:30",
            items = listOf(item(c, 30, before = 5)),
            focus = FocusState.Stopped(end = at("10:00"), lastItem = b, bufferAfter = min(10), paused = null),
        )

        assertEquals(listOf(at("10:30") to at("11:00")), schedule.times())
    }

    @Test
    fun `item ending exactly at close fits, and free time is what is left`() {
        val fits = build(now = "18:00", items = listOf(item(b, 60)))
        assertEquals(listOf(at("18:00") to at("19:00")), fits.times())
        assertTrue(fits.overflow.isEmpty())
        assertEquals(Duration.ZERO, fits.freeTime)

        val roomy = build(now = "17:00", items = listOf(item(b, 30)))
        assertEquals(min(90), roomy.freeTime)
        assertEquals(Duration.ZERO, roomy.overflowAmount)
    }

    @Test
    fun `everything after the first overflowing item overflows, even if it would fit`() {
        val schedule = build(now = "18:00", items = listOf(item(b, 90, after = 0), item(c, 5, before = 0)))

        assertTrue(schedule.slots.isEmpty())
        assertEquals(listOf(OverflowEntry(b, min(90), min(60)), OverflowEntry(c, min(5), Duration.ZERO)), schedule.overflow)
        assertEquals(min(35), schedule.overflowAmount)
    }

    @Test
    fun `empty plan - all time until close is free`() {
        val schedule = build(now = "15:00", items = emptyList())

        assertEquals(min(240), schedule.freeTime)
    }

    @Test
    fun `tomorrow's plan is laid out from its day start up to its close`() {
        // Planned tonight for tomorrow: day start 09:00, close 23:00.
        val schedule = ScheduleEngine.build(
            ScheduleInput(
                now = at("21:00"),
                dayStart = at("+1 09:00"),
                close = at("+1 23:00"),
                items = listOf(item(b, 600, after = 15), item(c, 300, before = 15)),
                focus = FocusState.NotStarted,
            ),
        )

        assertEquals(listOf(at("+1 09:00") to at("+1 19:00")), schedule.times())
        assertEquals(listOf(OverflowEntry(c, min(300), min(225))), schedule.overflow)
        assertEquals(min(75), schedule.overflowAmount)
    }

    @Test
    fun `layout start already past close - everything overflows`() {
        val schedule = build(now = "19:30", items = listOf(item(b, 30)), dayStart = "09:00", close = "19:00")

        assertEquals(listOf(OverflowEntry(b, min(30), Duration.ZERO)), schedule.overflow)
        assertEquals(min(60), schedule.overflowAmount)
    }
}
