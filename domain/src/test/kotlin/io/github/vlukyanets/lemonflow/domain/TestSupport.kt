package io.github.vlukyanets.lemonflow.domain

import io.github.vlukyanets.lemonflow.domain.model.BackgroundRunId
import io.github.vlukyanets.lemonflow.domain.model.ItemId
import io.github.vlukyanets.lemonflow.domain.model.PlannedItemId
import io.github.vlukyanets.lemonflow.domain.model.ProjectId
import io.github.vlukyanets.lemonflow.domain.model.TimeEntryId
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.util.UUID

/** Test day: all `at(...)` times are on this date, in UTC. */
val TEST_DATE: LocalDate = LocalDate.of(2026, 11, 27)

/** "16:05" on [TEST_DATE] in UTC; "+1 02:30" means the next day. */
fun at(time: String): Instant {
    val nextDay = time.startsWith("+1 ")
    val clock = LocalTime.parse(time.removePrefix("+1 "))
    val date = if (nextDay) TEST_DATE.plusDays(1) else TEST_DATE
    return date.atTime(clock).toInstant(ZoneOffset.UTC)
}

fun min(minutes: Long): Duration = Duration.ofMinutes(minutes)

private fun uuid(name: String): UUID = UUID.nameUUIDFromBytes(name.toByteArray())

fun plannedId(name: String) = PlannedItemId(uuid("planned:$name"))
fun itemId(name: String) = ItemId(uuid("item:$name"))
fun projectId(name: String) = ProjectId(uuid("project:$name"))
fun entryId(name: String) = TimeEntryId(uuid("entry:$name"))
fun runId(name: String) = BackgroundRunId(uuid("run:$name"))

/** Deterministic id source for code under test: e1, e2, ... */
class SequentialEntryIds : () -> TimeEntryId {
    private var next = 1
    override fun invoke(): TimeEntryId = entryId("e${next++}")
}
