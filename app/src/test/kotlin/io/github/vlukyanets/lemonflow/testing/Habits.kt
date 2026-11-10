package io.github.vlukyanets.lemonflow.testing

import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import java.time.Instant

/** Ready-made habits matching the design canvas. */
object Habits {
    private val created: Instant = Instant.parse("2026-11-01T08:00:00Z")

    fun reading(core: Boolean = true) = Habit(
        HabitId("reading"),
        "Reading",
        HabitType.CHECK,
        Amount(30, AmountUnit.MINUTES),
        Amount(1, AmountUnit.PAGES),
        step = null,
        isCore = core,
        createdAt = created,
        archivedAt = null
    )

    fun water() = Habit(
        HabitId("water"),
        "Water",
        HabitType.COUNT,
        Amount(2500, AmountUnit.CUSTOM, "ml"),
        Amount(500, AmountUnit.CUSTOM, "ml"),
        step = 250,
        isCore = true,
        createdAt = created.plusSeconds(1),
        archivedAt = null
    )

    fun meditation() = Habit(
        HabitId("meditation"),
        "Meditation",
        HabitType.TIMER,
        Amount(10, AmountUnit.MINUTES),
        Amount(1, AmountUnit.MINUTES),
        step = 1,
        isCore = false,
        createdAt = created.plusSeconds(2),
        archivedAt = null
    )

    fun sugar() = Habit(
        HabitId("sugar"),
        "Added sugar",
        HabitType.AVOID,
        Amount(0, AmountUnit.TIMES),
        Amount(1, AmountUnit.TIMES),
        step = null,
        isCore = false,
        createdAt = created.plusSeconds(3),
        archivedAt = null
    )

    fun journaling(archivedAt: Instant = Instant.parse("2026-10-14T08:00:00Z")) = Habit(
        HabitId("journaling"),
        "Journaling",
        HabitType.CHECK,
        Amount(1, AmountUnit.PAGES),
        Amount(1, AmountUnit.CUSTOM, "line"),
        step = null,
        isCore = false,
        createdAt = created.minusSeconds(100),
        archivedAt = archivedAt
    )
}
