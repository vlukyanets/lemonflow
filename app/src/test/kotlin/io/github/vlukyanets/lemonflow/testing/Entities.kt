package io.github.vlukyanets.lemonflow.testing

import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.domain.model.Habit

/** Stores a ready-made domain habit directly, bypassing the repository. */
fun Habit.toEntity() = HabitEntity(
    id = id.value,
    name = name,
    type = type,
    targetAmount = target.value,
    targetUnit = target.unit,
    targetUnitLabel = target.customLabel,
    baselineAmount = baseline.value,
    baselineUnit = baseline.unit,
    baselineUnitLabel = baseline.customLabel,
    step = step,
    isCore = isCore,
    createdAt = createdAt.toEpochMilli(),
    archivedAt = archivedAt?.toEpochMilli()
)
