package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.model.ProjectId
import io.github.vlukyanets.lemonflow.domain.model.TimeEntryId
import java.time.Instant

/** The entries a tracking command changes: [stopped] gets an end, [started] is new. */
data class TrackingChange(
    val stopped: TimeEntry? = null,
    val started: TimeEntry? = null,
)

/**
 * Focus-timer commands (spec: Tracking TRK-1...TRK-7). At most one focus entry runs at a
 * time; every command takes the current entries and returns only what changes.
 */
class Tracker(private val newId: () -> TimeEntryId) {

    /** TRK-1/TRK-2: start or resume a planned item; stops whatever runs. */
    fun start(entries: List<TimeEntry>, item: PlannedItemId, now: Instant): TrackingChange {
        val running = entries.running()
        if (running?.kind == EntryKind.PLANNED && running.plannedItemId == item) return TrackingChange()
        return TrackingChange(
            stopped = running?.stoppedAt(now),
            started = TimeEntry(newId(), EntryKind.PLANNED, now, null, plannedItemId = item),
        )
    }

    /** TRK-2: stop the running entry, of any kind. */
    fun pause(entries: List<TimeEntry>, now: Instant): TrackingChange =
        TrackingChange(stopped = entries.running()?.stoppedAt(now))

    /** TRK-3: done stops the item's timer if it is the one running. */
    fun done(entries: List<TimeEntry>, item: PlannedItemId, now: Instant): TrackingChange {
        val running = entries.running()
        return if (running?.plannedItemId == item) TrackingChange(stopped = running.stoppedAt(now)) else TrackingChange()
    }

    /** TRK-6: pause the running planned entry and start an interruption. */
    fun interrupt(entries: List<TimeEntry>, now: Instant, title: String? = null): TrackingChange {
        val running = entries.running()
        check(running != null && running.kind == EntryKind.PLANNED) { "Only a running planned item can be interrupted" }
        return TrackingChange(
            stopped = running.stoppedAt(now),
            started = TimeEntry(
                newId(),
                EntryKind.INTERRUPTION,
                now,
                null,
                title = title,
                interruptedEntryId = running.id,
            ),
        )
    }

    /**
     * TRK-6: end the running interruption. Returns the change and the planned item to offer
     * for one-tap resume.
     */
    fun endInterruption(entries: List<TimeEntry>, now: Instant): Pair<TrackingChange, PlannedItemId?> {
        val running = entries.running()
        check(running != null && running.kind == EntryKind.INTERRUPTION) { "No interruption is running" }
        val resume = entries.firstOrNull { it.id == running.interruptedEntryId }?.plannedItemId
        return TrackingChange(stopped = running.stoppedAt(now)) to resume
    }

    /** TRK-7: start unplanned work; stops whatever runs. */
    fun startUnplanned(
        entries: List<TimeEntry>,
        now: Instant,
        title: String? = null,
        projectId: ProjectId? = null,
    ): TrackingChange = TrackingChange(
        stopped = entries.running()?.stoppedAt(now),
        started = TimeEntry(newId(), EntryKind.UNPLANNED, now, null, title = title, projectId = projectId),
    )

    private fun List<TimeEntry>.running(): TimeEntry? = firstOrNull { it.isRunning }

    private fun TimeEntry.stoppedAt(now: Instant): TimeEntry = copy(end = maxOf(start, now))
}
