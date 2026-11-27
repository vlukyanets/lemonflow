package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.SequentialEntryIds
import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.entryId
import io.github.vlukyanets.lemonflow.domain.min
import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.plannedId
import io.github.vlukyanets.lemonflow.domain.schedule.FocusState
import io.github.vlukyanets.lemonflow.domain.schedule.ScheduleEngine
import io.github.vlukyanets.lemonflow.domain.schedule.ScheduleInput
import io.github.vlukyanets.lemonflow.domain.schedule.SchedulableItem
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals

class FocusStatesTest {

    private val b = plannedId("B")
    private val c = plannedId("C")
    private val buffers = mapOf(b to min(10), c to min(5))
    private fun of(vararg entries: TimeEntry) = FocusStates.of(entries.toList()) { buffers.getValue(it) }

    private val bDone = TimeEntry(entryId("b"), EntryKind.PLANNED, at("10:00"), at("10:20"), plannedItemId = b)

    @Test
    fun `no entries - not started`() {
        assertEquals(FocusState.NotStarted, of())
    }

    @Test
    fun `running planned entry`() {
        assertEquals(FocusState.RunningItem(b), of(bDone.copy(end = null)))
    }

    @Test
    fun `running interruption knows the item it paused`() {
        val interruption = TimeEntry(entryId("i"), EntryKind.INTERRUPTION, at("10:20"), null, interruptedEntryId = bDone.id)
        assertEquals(FocusState.RunningOther(paused = b), of(bDone, interruption))
    }

    @Test
    fun `stopped after a planned entry carries its after-buffer`() {
        assertEquals(FocusState.Stopped(at("10:20"), lastItem = b, bufferAfter = min(10), paused = null), of(bDone))
    }

    @Test
    fun `stopped after an interruption that was not resumed`() {
        val interruption = TimeEntry(entryId("i"), EntryKind.INTERRUPTION, at("10:20"), at("10:35"), interruptedEntryId = bDone.id)
        assertEquals(FocusState.Stopped(at("10:35"), lastItem = null, bufferAfter = Duration.ZERO, paused = b), of(bDone, interruption))
    }

    @Test
    fun `stopped after unplanned work has no buffer and nothing paused`() {
        val unplanned = TimeEntry(entryId("u"), EntryKind.UNPLANNED, at("11:00"), at("11:30"))
        assertEquals(FocusState.Stopped(at("11:30"), lastItem = null, bufferAfter = Duration.ZERO, paused = null), of(bDone, unplanned))
    }

    @Test
    fun `an interruption shifts the schedule by its length and adds no buffers`() {
        // B (60 min) runs from 10:00, a call interrupts it at 10:20 and ends at 10:35.
        val tracker = Tracker(SequentialEntryIds())
        var entries = emptyList<TimeEntry>()
        fun apply(change: TrackingChange) {
            entries = entries.map { entry -> change.stopped?.takeIf { it.id == entry.id } ?: entry } +
                listOfNotNull(change.started)
        }
        apply(tracker.start(entries, b, at("10:00")))
        apply(tracker.interrupt(entries, at("10:20"), title = "Call"))
        apply(tracker.endInterruption(entries, at("10:35")).first)

        val now = at("10:35")
        val tracked = trackedByItem(entries, now)
        val schedule = ScheduleEngine.build(
            ScheduleInput(
                now = now,
                dayStart = at("09:00"),
                close = at("19:00"),
                items = listOf(
                    SchedulableItem(b, min(60), tracked.getValue(b), min(10), min(10)),
                    SchedulableItem(c, min(30), Duration.ZERO, min(5), min(5)),
                ),
                focus = FocusStates.of(entries) { buffers.getValue(it) },
            ),
        )

        // Without the call B would end at 11:00; with it, 11:15.
        assertEquals(listOf(at("10:35") to at("11:15"), at("11:25") to at("11:55")), schedule.slots.map { it.start to it.end })
    }
}
