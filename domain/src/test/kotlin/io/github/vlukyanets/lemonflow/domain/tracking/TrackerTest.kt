package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.SequentialEntryIds
import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.entryId
import io.github.vlukyanets.lemonflow.domain.min
import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.plannedId
import io.github.vlukyanets.lemonflow.domain.projectId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class TrackerTest {

    private val tracker = Tracker(SequentialEntryIds())
    private val b = plannedId("B")
    private val c = plannedId("C")

    private val runningB = TimeEntry(entryId("b1"), EntryKind.PLANNED, at("10:00"), null, plannedItemId = b)

    @Test
    fun `start with nothing running starts a planned entry`() {
        val change = tracker.start(emptyList(), b, at("10:00"))

        assertNull(change.stopped)
        assertEquals(TimeEntry(entryId("e1"), EntryKind.PLANNED, at("10:00"), null, plannedItemId = b), change.started)
    }

    @Test
    fun `starting another item stops the running one`() {
        val change = tracker.start(listOf(runningB), c, at("10:30"))

        assertEquals(at("10:30"), change.stopped?.end)
        assertEquals(c, change.started?.plannedItemId)
    }

    @Test
    fun `starting the item that already runs changes nothing`() {
        assertEquals(TrackingChange(), tracker.start(listOf(runningB), b, at("10:30")))
    }

    @Test
    fun `interrupt pauses the planned entry and starts a linked interruption`() {
        val change = tracker.interrupt(listOf(runningB), at("10:20"), title = "Call")

        assertEquals(runningB.copy(end = at("10:20")), change.stopped)
        val interruption = change.started!!
        assertEquals(EntryKind.INTERRUPTION, interruption.kind)
        assertEquals(runningB.id, interruption.interruptedEntryId)
        assertEquals("Call", interruption.title)
    }

    @Test
    fun `interrupt needs a running planned entry`() {
        assertFailsWith<IllegalStateException> { tracker.interrupt(emptyList(), at("10:20")) }
        val unplanned = TimeEntry(entryId("u"), EntryKind.UNPLANNED, at("10:00"), null)
        assertFailsWith<IllegalStateException> { tracker.interrupt(listOf(unplanned), at("10:20")) }
    }

    @Test
    fun `ending an interruption offers the paused item for resume`() {
        val pausedB = runningB.copy(end = at("10:20"))
        val interruption = TimeEntry(
            entryId("i1"), EntryKind.INTERRUPTION, at("10:20"), null, interruptedEntryId = pausedB.id,
        )

        val (change, resume) = tracker.endInterruption(listOf(pausedB, interruption), at("10:35"))

        assertEquals(at("10:35"), change.stopped?.end)
        assertNull(change.started)
        assertEquals(b, resume)
    }

    @Test
    fun `unplanned work stops whatever runs`() {
        val change = tracker.startUnplanned(listOf(runningB), at("11:00"), title = "Helped a colleague", projectId = projectId("Work"))

        assertEquals(at("11:00"), change.stopped?.end)
        assertEquals(EntryKind.UNPLANNED, change.started?.kind)
        assertEquals(projectId("Work"), change.started?.projectId)
    }

    @Test
    fun `done stops the timer only when that item is running`() {
        assertEquals(at("10:40"), tracker.done(listOf(runningB), b, at("10:40")).stopped?.end)
        assertEquals(TrackingChange(), tracker.done(listOf(runningB), c, at("10:40")))
    }

    @Test
    fun `pause stops the running entry`() {
        assertEquals(at("10:15"), tracker.pause(listOf(runningB), at("10:15")).stopped?.end)
        assertEquals(TrackingChange(), tracker.pause(emptyList(), at("10:15")))
    }

    @Test
    fun `tracked time sums planned entries per item, running ones up to now`() {
        val entries = listOf(
            TimeEntry(entryId("1"), EntryKind.PLANNED, at("09:00"), at("09:30"), plannedItemId = b),
            TimeEntry(entryId("2"), EntryKind.INTERRUPTION, at("09:30"), at("09:45"), interruptedEntryId = entryId("1")),
            TimeEntry(entryId("3"), EntryKind.PLANNED, at("09:45"), null, plannedItemId = b),
            TimeEntry(entryId("4"), EntryKind.PLANNED, at("08:00"), at("08:20"), plannedItemId = c),
        )

        assertEquals(mapOf(b to min(45), c to min(20)), trackedByItem(entries, at("10:00")))
    }
}
