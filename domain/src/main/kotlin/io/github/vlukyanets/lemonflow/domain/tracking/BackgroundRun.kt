package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.model.BackgroundRunId
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import java.time.Duration
import java.time.Instant

/** A background item's run (spec: BG-3...BG-8). [end] is null until the owner ticks it done. */
data class BackgroundRun(
    val id: BackgroundRunId,
    val plannedItemId: PlannedItemId,
    val start: Instant,
    val expectedEnd: Instant,
    val end: Instant? = null,
) {
    val isRunning: Boolean get() = end == null

    /** BG-6: past its expected end but not ticked done yet. */
    fun shouldBeReady(now: Instant): Boolean = isRunning && now >= expectedEnd

    companion object {
        fun start(id: BackgroundRunId, item: PlannedItemId, now: Instant, duration: Duration): BackgroundRun =
            BackgroundRun(id, item, now, now + duration)
    }
}

/** BG-7: the run the sticky notification counts down to - the soonest expected end. */
fun List<BackgroundRun>.soonestEnding(): BackgroundRun? = filter { it.isRunning }.minByOrNull { it.expectedEnd }
