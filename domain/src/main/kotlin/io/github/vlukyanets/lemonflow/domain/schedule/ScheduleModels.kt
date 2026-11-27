package io.github.vlukyanets.lemonflow.domain.schedule

import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import java.time.Duration
import java.time.Instant

/** A timed focus item with status PLANNED, as the engine sees it. */
data class SchedulableItem(
    val id: PlannedItemId,
    val planned: Duration,
    val tracked: Duration,
    val bufferBefore: Duration,
    val bufferAfter: Duration,
) {
    val remaining: Duration get() = (planned - tracked).coerceAtLeast(Duration.ZERO)
}

/** What the owner's focus is doing right now, derived from the day's time entries. */
sealed interface FocusState {
    /** No focus entry has been recorded on this day yet. */
    data object NotStarted : FocusState

    /** A planned item's timer is running. */
    data class RunningItem(val item: PlannedItemId) : FocusState

    /** Unplanned work or an interruption is running; [paused] is the item it interrupted. */
    data class RunningOther(val paused: PlannedItemId?) : FocusState

    /**
     * Nothing is running; the last focus entry ended at [end].
     * [lastItem] is that entry's planned item (null for unplanned work or an interruption),
     * [bufferAfter] its after-buffer (zero when [lastItem] is null), and [paused] the item an
     * interruption paused and that was not resumed since.
     */
    data class Stopped(
        val end: Instant,
        val lastItem: PlannedItemId?,
        val bufferAfter: Duration,
        val paused: PlannedItemId?,
    ) : FocusState
}

data class ScheduleInput(
    val now: Instant,
    /** The day's day-start instant; used only while nothing has been tracked that day. */
    val dayStart: Instant,
    /** The day's close instant: the horizon. */
    val close: Instant,
    /** Timed focus items with status PLANNED, in the owner's order. */
    val items: List<SchedulableItem>,
    val focus: FocusState,
)

data class Slot(
    val item: PlannedItemId,
    val start: Instant,
    val end: Instant,
    val running: Boolean,
    /** Tracked time beyond the estimate; non-zero only for a running item over its estimate. */
    val overBy: Duration,
)

data class OverflowEntry(
    val item: PlannedItemId,
    val remaining: Duration,
    /** How much of [remaining] still fits before close; non-zero only for the item crossing it. */
    val fits: Duration,
)

data class Schedule(
    val slots: List<Slot>,
    val overflow: List<OverflowEntry>,
    /** Items that are not running and have used up their estimate: no time, no buffers. */
    val estimateUsedUp: List<PlannedItemId>,
    /** Where the last item would end without the limit, minus close; zero when all fits. */
    val overflowAmount: Duration,
    /** Close minus the end of the last slot; zero when something overflows. */
    val freeTime: Duration,
)
