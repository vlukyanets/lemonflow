package io.github.vlukyanets.lemonflow.domain.plan

import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.entryId
import io.github.vlukyanets.lemonflow.domain.itemId
import io.github.vlukyanets.lemonflow.domain.min
import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.model.ItemKind
import io.github.vlukyanets.lemonflow.domain.model.PlannedStatus
import io.github.vlukyanets.lemonflow.domain.plannedId
import io.github.vlukyanets.lemonflow.domain.runId
import io.github.vlukyanets.lemonflow.domain.tracking.BackgroundRun
import io.github.vlukyanets.lemonflow.domain.tracking.TimeEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DayCloseTest {

    private val close = at("+1 03:00")

    private val items = listOf(
        PlannedItemSnapshot(plannedId("report"), itemId("report"), ItemKind.ONE_OFF, PlannedStatus.PLANNED),
        PlannedItemSnapshot(plannedId("call"), itemId("call"), ItemKind.ONE_OFF, PlannedStatus.DONE),
        PlannedItemSnapshot(plannedId("gym"), itemId("gym"), ItemKind.RECURRING, PlannedStatus.PLANNED),
        PlannedItemSnapshot(plannedId("hobby"), itemId("hobby"), ItemKind.TIME_BOX, PlannedStatus.PLANNED),
    )

    @Test
    fun `unfinished items are returned and one-off items go back to the backlog`() {
        val result = DayClose.close(close, items, emptyList(), emptyList())

        assertEquals(listOf(plannedId("report"), plannedId("gym"), plannedId("hobby")), result.returned)
        assertEquals(listOf(itemId("report")), result.backToBacklog)
    }

    @Test
    fun `running timer and background runs are cut at the close instant`() {
        val running = TimeEntry(entryId("r"), EntryKind.PLANNED, at("23:00"), null, plannedItemId = plannedId("report"))
        val nextDay = TimeEntry(entryId("n"), EntryKind.UNPLANNED, at("+1 03:10"), null)
        val finished = TimeEntry(entryId("f"), EntryKind.UNPLANNED, at("20:00"), at("21:00"))
        val run = BackgroundRun.start(runId("dishwasher"), plannedId("dishwasher"), at("+1 02:00"), min(120))

        val result = DayClose.close(close, items, listOf(running, nextDay, finished), listOf(run))

        assertEquals(listOf(running.copy(end = close)), result.cutEntries)
        assertEquals(listOf(run.copy(end = close)), result.cutRuns)
    }

    @Test
    fun `closing an already closed day changes nothing`() {
        val first = DayClose.close(
            close,
            items,
            listOf(TimeEntry(entryId("r"), EntryKind.PLANNED, at("23:00"), null, plannedItemId = plannedId("report"))),
            emptyList(),
        )
        val closedItems = items.map { if (it.id in first.returned) it.copy(status = PlannedStatus.RETURNED) else it }

        val second = DayClose.close(close, closedItems, first.cutEntries, emptyList())

        assertTrue(second.returned.isEmpty())
        assertTrue(second.backToBacklog.isEmpty())
        assertTrue(second.cutEntries.isEmpty())
    }
}
