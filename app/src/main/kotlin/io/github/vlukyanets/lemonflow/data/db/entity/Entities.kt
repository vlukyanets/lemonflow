package io.github.vlukyanets.lemonflow.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark

// Conventions (constitution I, data-model.md): UUID TEXT keys, instants as UTC epoch millis,
// user dates as ISO yyyy-MM-dd TEXT. Enums are stored by name (Room's built-in converter).

/** `name`: trimmed, 1..120 chars; custom unit labels: trimmed, 1..30 chars. */
@Entity(
    tableName = "habit",
    indices = [Index("archivedAt"), Index("isCore", "createdAt")]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: HabitType,
    val targetAmount: Int,
    val targetUnit: AmountUnit,
    val targetUnitLabel: String?,
    val baselineAmount: Int,
    val baselineUnit: AmountUnit,
    val baselineUnitLabel: String?,
    val step: Int?,
    val isCore: Boolean,
    val createdAt: Long,
    val archivedAt: Long?
)

/** `amount`: COUNT units, TIMER accumulated seconds, AVOID count, CHECK 0. */
@Entity(
    tableName = "daily_record",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habitId", "date"], unique = true), Index("date")]
)
data class DailyRecordEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val date: String,
    val mark: Mark,
    val amount: Int,
    val timerStartedAt: Long?,
    val updatedAt: Long,
    val zoneId: String
)

/** A missing row means hard day is off (FR-010). */
@Entity(tableName = "day", indices = [Index(value = ["date"], unique = true)])
data class DayEntity(
    @PrimaryKey val id: String,
    val date: String,
    val hardDay: Boolean,
    val updatedAt: Long,
    val zoneId: String
)

/** Single row; `dayEndHour`: 0..6, default 3. */
@Entity(tableName = "settings")
data class SettingsEntity(@PrimaryKey val id: String, val dayEndHour: Int, val updatedAt: Long)
