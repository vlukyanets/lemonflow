package io.github.vlukyanets.lemonflow.domain.plan

import io.github.vlukyanets.lemonflow.domain.model.ItemId
import io.github.vlukyanets.lemonflow.domain.model.ItemKind
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.model.PlannedStatus
import io.github.vlukyanets.lemonflow.domain.tracking.BackgroundRun
import io.github.vlukyanets.lemonflow.domain.tracking.TimeEntry
import java.time.Instant

/** A planned item as day close sees it. Covers timed and CHECK items alike (DAY-3). */
data class PlannedItemSnapshot(
    val id: PlannedItemId,
    val itemId: ItemId,
    val itemKind: ItemKind,
    val status: PlannedStatus,
)

data class DayCloseResult(
    /** Planned items whose status becomes RETURNED. */
    val returned: List<PlannedItemId>,
    /** One-off catalog items that go back to BACKLOG with their estimate unchanged. */
    val backToBacklog: List<ItemId>,
    /** Running focus entries cut at the close instant. */
    val cutEntries: List<TimeEntry>,
    /** Running background runs cut at the close instant (BG-8). */
    val cutRuns: List<BackgroundRun>,
)

/** Day close (spec: DAY-2...DAY-4, BG-8). Pure; applying the result twice changes nothing. */
object DayClose {

    fun close(
        close: Instant,
        items: List<PlannedItemSnapshot>,
        entries: List<TimeEntry>,
        runs: List<BackgroundRun>,
    ): DayCloseResult {
        val open = items.filter { it.status == PlannedStatus.PLANNED }
        return DayCloseResult(
            returned = open.map { it.id },
            backToBacklog = open.filter { it.itemKind == ItemKind.ONE_OFF }.map { it.itemId },
            // Entries and runs started at or after close belong to the next day.
            cutEntries = entries.filter { it.isRunning && it.start < close }.map { it.copy(end = close) },
            cutRuns = runs.filter { it.isRunning && it.start < close }.map { it.copy(end = close) },
        )
    }
}
