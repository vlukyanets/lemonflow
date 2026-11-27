package io.github.vlukyanets.lemonflow.domain.tracking

import io.github.vlukyanets.lemonflow.domain.at
import io.github.vlukyanets.lemonflow.domain.entryId
import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.plannedId
import kotlin.test.Test
import kotlin.test.assertEquals

class TimeEntryRulesTest {

    private val now = at("15:00")
    private val existing = TimeEntry(entryId("x"), EntryKind.PLANNED, at("10:00"), at("11:00"), plannedItemId = plannedId("B"))

    private fun manual(start: String, end: String, id: String = "new") =
        TimeEntry(entryId(id), EntryKind.UNPLANNED, at(start), at(end), title = "Call")

    @Test
    fun `entry in a free slot is accepted`() {
        assertEquals(EntryValidation.Ok, TimeEntryRules.validate(manual("14:00", "14:30"), listOf(existing), now))
    }

    @Test
    fun `zero-length entry is refused`() {
        assertEquals(EntryValidation.Empty, TimeEntryRules.validate(manual("14:00", "14:00"), emptyList(), now))
    }

    @Test
    fun `entry ending in the future is refused`() {
        assertEquals(EntryValidation.EndsInFuture, TimeEntryRules.validate(manual("14:30", "15:30"), emptyList(), now))
    }

    @Test
    fun `overlapping entry is refused with the conflict shown`() {
        assertEquals(
            EntryValidation.Overlaps(existing),
            TimeEntryRules.validate(manual("10:30", "11:30"), listOf(existing), now),
        )
    }

    @Test
    fun `entries may touch end to start`() {
        assertEquals(EntryValidation.Ok, TimeEntryRules.validate(manual("11:00", "11:30"), listOf(existing), now))
        assertEquals(EntryValidation.Ok, TimeEntryRules.validate(manual("09:30", "10:00"), listOf(existing), now))
    }

    @Test
    fun `editing an entry ignores its own old version`() {
        val edited = existing.copy(end = at("11:15"))
        assertEquals(EntryValidation.Ok, TimeEntryRules.validate(edited, listOf(existing), now))
    }

    @Test
    fun `a running entry counts as ending now`() {
        val running = TimeEntry(entryId("r"), EntryKind.PLANNED, at("14:00"), null, plannedItemId = plannedId("C"))
        assertEquals(
            EntryValidation.Overlaps(running),
            TimeEntryRules.validate(manual("14:30", "14:45"), listOf(running), now),
        )
    }
}
