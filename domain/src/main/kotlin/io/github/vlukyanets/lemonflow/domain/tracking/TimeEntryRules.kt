package io.github.vlukyanets.lemonflow.domain.tracking

import java.time.Instant

sealed interface EntryValidation {
    data object Ok : EntryValidation
    data object Empty : EntryValidation
    data object EndsInFuture : EntryValidation
    data class Overlaps(val conflict: TimeEntry) : EntryValidation
}

/** Checks for entries added or edited by hand (spec: TRK-8, TRK-9). */
object TimeEntryRules {

    /**
     * Validates a finished [candidate] against the other focus entries. An entry being edited
     * is matched by id and ignored; a running entry counts as ending at [now].
     * Intervals are half-open, so one entry may start exactly when another ends.
     */
    fun validate(candidate: TimeEntry, others: List<TimeEntry>, now: Instant): EntryValidation {
        val end = requireNotNull(candidate.end) { "Manual entries must have an end" }
        if (end <= candidate.start) return EntryValidation.Empty
        if (end > now) return EntryValidation.EndsInFuture
        val conflict = others
            .filter { it.id != candidate.id }
            .firstOrNull { other ->
                val otherEnd = other.end ?: now
                candidate.start < otherEnd && other.start < end
            }
        return if (conflict == null) EntryValidation.Ok else EntryValidation.Overlaps(conflict)
    }
}
