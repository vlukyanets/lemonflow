package io.github.vlukyanets.lemonflow.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.vlukyanets.lemonflow.domain.model.Attention
import io.github.vlukyanets.lemonflow.domain.model.EntryKind
import io.github.vlukyanets.lemonflow.domain.model.ItemKind
import io.github.vlukyanets.lemonflow.domain.model.ItemState
import io.github.vlukyanets.lemonflow.domain.model.Measure
import io.github.vlukyanets.lemonflow.domain.model.PlannedStatus

/*
 * Storage conventions (spec: Constraints):
 * - ids are UUID strings;
 * - instants are UTC epoch milliseconds (`...Millis`);
 * - logical dates are ISO `yyyy-MM-dd`, local times are ISO `HH:mm`, zones are IANA ids;
 * - durations are whole minutes.
 */

/** Decorative grouping only; no logic depends on it. */
@Entity(tableName = "area")
data class AreaEntity(
    @PrimaryKey val id: String,
    val name: String,
    val color: Int,
    val sortOrder: Int,
    val archived: Boolean,
)

/** [areaId] is set on top-level projects only; nested projects take their root's area. */
@Entity(
    tableName = "project",
    foreignKeys = [
        ForeignKey(entity = AreaEntity::class, parentColumns = ["id"], childColumns = ["areaId"]),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["parentProjectId"]),
    ],
    indices = [Index("areaId"), Index("parentProjectId")],
)
data class ProjectEntity(
    @PrimaryKey val id: String,
    val areaId: String?,
    val parentProjectId: String?,
    val name: String,
    val sortOrder: Int,
    val archived: Boolean,
)

enum class RecurrenceType { DAILY, WEEKDAYS, EVERY_N_DAYS }

/**
 * Catalog item. Recurrence columns are used only for [ItemKind.RECURRING]:
 * [recurrenceWeekdays] is a bitmask with bit 0 = Monday ... bit 6 = Sunday.
 */
@Entity(
    tableName = "item",
    foreignKeys = [
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"]),
    ],
    indices = [Index("projectId")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val title: String,
    val kind: ItemKind,
    val measure: Measure,
    val attention: Attention,
    val defaultMinutes: Int?,
    val bufferBeforeMinutes: Int,
    val bufferAfterMinutes: Int,
    val recurrenceType: RecurrenceType?,
    val recurrenceWeekdays: Int?,
    val recurrenceIntervalDays: Int?,
    val recurrenceAnchorDate: String?,
    val state: ItemState,
    val notes: String?,
)

/** Zone, day start and close time are fixed when the plan is created (DAY-1). */
@Entity(
    tableName = "day_plan",
    indices = [Index(value = ["logicalDate"], unique = true)],
)
data class DayPlanEntity(
    @PrimaryKey val id: String,
    val logicalDate: String,
    val zoneId: String,
    val dayStart: String,
    val closeTime: String,
    val closedAtMillis: Long?,
)

@Entity(
    tableName = "planned_item",
    foreignKeys = [
        ForeignKey(entity = DayPlanEntity::class, parentColumns = ["id"], childColumns = ["dayPlanId"]),
        ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"]),
    ],
    indices = [Index(value = ["dayPlanId", "itemId"], unique = true), Index("itemId")],
)
data class PlannedItemEntity(
    @PrimaryKey val id: String,
    val dayPlanId: String,
    val itemId: String,
    val sortOrder: Int,
    val plannedMinutes: Int?,
    val bufferBeforeMinutes: Int,
    val bufferAfterMinutes: Int,
    val status: PlannedStatus,
)

/** A focus entry. [logicalDate] is the logical day the entry starts in (TRK-11). */
@Entity(
    tableName = "time_entry",
    foreignKeys = [
        ForeignKey(entity = PlannedItemEntity::class, parentColumns = ["id"], childColumns = ["plannedItemId"]),
        ForeignKey(entity = ProjectEntity::class, parentColumns = ["id"], childColumns = ["projectId"]),
        ForeignKey(entity = TimeEntryEntity::class, parentColumns = ["id"], childColumns = ["interruptedEntryId"]),
    ],
    indices = [Index("plannedItemId"), Index("projectId"), Index("interruptedEntryId"), Index("logicalDate")],
)
data class TimeEntryEntity(
    @PrimaryKey val id: String,
    val kind: EntryKind,
    val plannedItemId: String?,
    val title: String?,
    val projectId: String?,
    val interruptedEntryId: String?,
    val logicalDate: String,
    val startMillis: Long,
    val endMillis: Long?,
)

/** A background item's run; kept apart from focus entries (BG-3). */
@Entity(
    tableName = "background_run",
    foreignKeys = [
        ForeignKey(entity = PlannedItemEntity::class, parentColumns = ["id"], childColumns = ["plannedItemId"]),
    ],
    indices = [Index("plannedItemId")],
)
data class BackgroundRunEntity(
    @PrimaryKey val id: String,
    val plannedItemId: String,
    val startMillis: Long,
    val expectedEndMillis: Long,
    val endMillis: Long?,
)
