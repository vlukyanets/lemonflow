package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.model.ProjectId
import io.github.vlukyanets.lemonflow.domain.model.TimeEntryId
import java.time.Duration
import java.time.Instant

/** A focus entry. [end] is null while it runs. */
data class TimeEntry(
    val id: TimeEntryId,
    val kind: EntryKind,
    val start: Instant,
    val end: Instant?,
    /** Set for [EntryKind.PLANNED] only. */
    val plannedItemId: PlannedItemId? = null,
    val title: String? = null,
    val projectId: ProjectId? = null,
    /** For [EntryKind.INTERRUPTION]: the planned entry it paused. */
    val interruptedEntryId: TimeEntryId? = null,
) {
    init {
        require(end == null || end >= start) { "Entry ends before it starts" }
        require((kind == EntryKind.PLANNED) == (plannedItemId != null)) {
            "Only planned entries reference a planned item"
        }
        require(interruptedEntryId == null || kind == EntryKind.INTERRUPTION) {
            "Only interruptions reference an interrupted entry"
        }
    }

    val isRunning: Boolean get() = end == null

    /** Length so far; a running entry counts up to [now]. */
    fun duration(now: Instant): Duration = Duration.between(start, end ?: maxOf(start, now))
}

/** Tracked time per planned item; running entries count up to [now]. */
fun trackedByItem(entries: List<TimeEntry>, now: Instant): Map<PlannedItemId, Duration> =
    entries
        .filter { it.kind == EntryKind.PLANNED }
        .groupBy { it.plannedItemId!! }
        .mapValues { (_, list) -> list.fold(Duration.ZERO) { sum, entry -> sum + entry.duration(now) } }
