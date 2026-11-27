package io.github.vlukyanets.lemonflow.domain.schedule

import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import java.time.Duration
import java.time.Instant

/**
 * Lays out a day's unfinished timed items in the owner's order until the day closes
 * (spec: Live schedule). Pure and deterministic: all time comes from [ScheduleInput.now].
 *
 * Rules:
 * - The running item goes first, from now. An item paused by an interruption (running or
 *   ended, not resumed since) goes first too. Everything else keeps the owner's order.
 * - The gap between two items is max(after-buffer of the previous, before-buffer of the
 *   next), never the sum. Unplanned work and interruptions have no buffers.
 * - No gap before the first item of the day, nor when resuming the item that was last worked
 *   on or that an interruption paused.
 * - While nothing has been tracked that day, layout starts at max(now, day start);
 *   afterwards it starts from now (idle time absorbs buffers).
 * - The first item ending after close, and every item after it, overflow in order.
 */
object ScheduleEngine {

    fun build(input: ScheduleInput): Schedule {
        val now = input.now
        val focus = input.focus
        val ids = input.items.mapTo(HashSet()) { it.id }

        val running = (focus as? FocusState.RunningItem)?.item?.takeIf { it in ids }
        val paused = when (focus) {
            is FocusState.RunningOther -> focus.paused
            is FocusState.Stopped -> focus.paused
            else -> null
        }?.takeIf { it in ids }
        val first = running ?: paused
        val ordered = if (first == null) {
            input.items
        } else {
            input.items.filter { it.id == first } + input.items.filter { it.id != first }
        }

        // State of "the previous thing" the next item's gap is measured from.
        var previousEnd: Instant
        var previousAfter: Duration
        var hasPrevious: Boolean
        var noGapFor: Set<PlannedItemId>
        when (focus) {
            FocusState.NotStarted -> {
                previousEnd = maxOf(now, input.dayStart)
                previousAfter = Duration.ZERO
                hasPrevious = false
                noGapFor = emptySet()
            }
            is FocusState.RunningItem -> {
                previousEnd = now
                previousAfter = Duration.ZERO
                hasPrevious = false
                noGapFor = emptySet()
            }
            is FocusState.RunningOther -> {
                previousEnd = now
                previousAfter = Duration.ZERO
                hasPrevious = true
                noGapFor = setOfNotNull(focus.paused)
            }
            is FocusState.Stopped -> {
                previousEnd = focus.end
                previousAfter = focus.bufferAfter
                hasPrevious = true
                noGapFor = setOfNotNull(focus.lastItem, focus.paused)
            }
        }

        val slots = mutableListOf<Slot>()
        val overflow = mutableListOf<OverflowEntry>()
        val usedUp = mutableListOf<PlannedItemId>()
        var overflowing = false
        var lastEnd: Instant? = null

        for (item in ordered) {
            val isRunning = item.id == running
            if (!isRunning && item.remaining.isZero) {
                usedUp += item.id
                continue
            }
            val start = when {
                isRunning -> now
                !hasPrevious -> previousEnd
                else -> {
                    val gap = if (item.id in noGapFor) Duration.ZERO else maxOf(previousAfter, item.bufferBefore)
                    maxOf(now, previousEnd + gap)
                }
            }
            val end = start + item.remaining

            if (!overflowing && end <= input.close) {
                val overBy = if (isRunning) (item.tracked - item.planned).coerceAtLeast(Duration.ZERO) else Duration.ZERO
                slots += Slot(item.id, start, end, isRunning, overBy)
            } else {
                val fits = if (overflowing) {
                    Duration.ZERO
                } else {
                    Duration.between(start, input.close).coerceIn(Duration.ZERO, item.remaining)
                }
                overflowing = true
                overflow += OverflowEntry(item.id, item.remaining, fits)
            }

            previousEnd = end
            previousAfter = item.bufferAfter
            hasPrevious = true
            noGapFor = emptySet()
            lastEnd = end
        }

        val overflowAmount = if (overflowing) Duration.between(input.close, lastEnd) else Duration.ZERO
        val freeFrom = slots.lastOrNull()?.end ?: when (focus) {
            FocusState.NotStarted -> maxOf(now, input.dayStart)
            else -> now
        }
        val freeTime = if (overflowing) {
            Duration.ZERO
        } else {
            Duration.between(freeFrom, input.close).coerceAtLeast(Duration.ZERO)
        }

        return Schedule(slots, overflow, usedUp, overflowAmount, freeTime)
    }
}
