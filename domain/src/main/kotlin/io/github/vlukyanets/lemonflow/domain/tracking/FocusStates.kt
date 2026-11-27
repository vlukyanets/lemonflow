package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.schedule.FocusState
import java.time.Duration

/** Derives the engine's [FocusState] from one day's focus entries. */
object FocusStates {

    /**
     * @param entries the day's focus entries, any order.
     * @param bufferAfter the after-buffer of a planned item on that day.
     */
    fun of(entries: List<TimeEntry>, bufferAfter: (PlannedItemId) -> Duration): FocusState {
        if (entries.isEmpty()) return FocusState.NotStarted
        val byId = entries.associateBy { it.id }

        fun pausedBy(entry: TimeEntry): PlannedItemId? =
            entry.interruptedEntryId?.let { byId[it] }?.plannedItemId

        entries.firstOrNull { it.isRunning }?.let { running ->
            return when (running.kind) {
                EntryKind.PLANNED -> FocusState.RunningItem(running.plannedItemId!!)
                else -> FocusState.RunningOther(pausedBy(running))
            }
        }

        val last = entries.maxWith(compareBy<TimeEntry> { it.end }.thenBy { it.start })
        val lastItem = last.plannedItemId
        return FocusState.Stopped(
            end = last.end!!,
            lastItem = lastItem,
            bufferAfter = lastItem?.let(bufferAfter) ?: Duration.ZERO,
            paused = if (last.kind == EntryKind.INTERRUPTION) pausedBy(last) else null,
        )
    }
}
