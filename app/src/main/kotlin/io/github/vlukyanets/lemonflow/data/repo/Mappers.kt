package io.github.vlukyanets.lemonflow.data.repo

import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

internal fun HabitEntity.toDomain(): Habit = Habit(
    id = HabitId(id),
    name = name,
    type = type,
    target = Amount(targetAmount, targetUnit, targetUnitLabel),
    baseline = Amount(baselineAmount, baselineUnit, baselineUnitLabel),
    step = step,
    isCore = isCore,
    createdAt = Instant.ofEpochMilli(createdAt),
    archivedAt = archivedAt?.let(Instant::ofEpochMilli)
)

internal fun DailyRecordEntity.toDomain(): DailyRecord = DailyRecord(
    id = id,
    habitId = HabitId(habitId),
    date = LocalDate.parse(date),
    mark = mark,
    amount = amount,
    timerStartedAt = timerStartedAt?.let(Instant::ofEpochMilli),
    updatedAt = Instant.ofEpochMilli(updatedAt),
    zoneId = ZoneId.of(zoneId)
)

internal fun DailyRecord.toEntity(): DailyRecordEntity = DailyRecordEntity(
    id = id,
    habitId = habitId.value,
    date = date.toString(),
    mark = mark,
    amount = amount,
    timerStartedAt = timerStartedAt?.toEpochMilli(),
    updatedAt = updatedAt.toEpochMilli(),
    zoneId = zoneId.id
)
