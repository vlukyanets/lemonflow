package io.github.vlukyanets.lemonflow.domain.model

import java.util.UUID

@JvmInline
value class ProjectId(val value: UUID)

@JvmInline
value class ItemId(val value: UUID)

@JvmInline
value class PlannedItemId(val value: UUID)

@JvmInline
value class TimeEntryId(val value: UUID)

@JvmInline
value class BackgroundRunId(val value: UUID)
