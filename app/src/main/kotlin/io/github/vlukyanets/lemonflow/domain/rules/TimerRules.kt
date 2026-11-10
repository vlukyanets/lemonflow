package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import java.time.Instant
import java.time.LocalDate

/**
 * Timers without background work (research R5): a running timer is a record with a start
 * instant, and its time is always computed from that instant.
 */
object TimerRules {
    /**
     * Stored seconds plus the running time, measured up to [now] but never past [dayEnd].
     * A clock jump backwards adds nothing, so the result never drops below [amount].
     */
    fun elapsedSeconds(amount: Int, startedAt: Instant?, now: Instant, dayEnd: Instant): Int {
        if (startedAt == null) return amount
        val until = if (now.isAfter(dayEnd)) dayEnd else now
        val running = (until.epochSecond - startedAt.epochSecond).coerceAtLeast(0)
        return (amount + running).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    /**
     * Stops every running timer whose day ended by [now], folding in the time up to that day's
     * end. Returns only the records that changed; the caller re-derives their marks.
     */
    fun settle(
        records: List<DailyRecord>,
        now: Instant,
        dayEnd: (LocalDate) -> Instant
    ): List<DailyRecord> = records.mapNotNull { record ->
        val startedAt = record.timerStartedAt ?: return@mapNotNull null
        val end = dayEnd(record.date)
        if (now.isBefore(end)) return@mapNotNull null
        record.copy(
            amount = elapsedSeconds(record.amount, startedAt, end, end),
            timerStartedAt = null
        )
    }
}
