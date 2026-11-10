package io.github.vlukyanets.lemonflow.testing

import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.SaveResult
import io.github.vlukyanets.lemonflow.domain.port.HabitRepository
import io.github.vlukyanets.lemonflow.domain.rules.HabitValidator
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** In-memory HabitRepository for screen tests; uses the real validator. */
class FakeHabitRepository(initial: List<Habit> = emptyList()) : HabitRepository {
    val habits = MutableStateFlow(initial)
    private var nextId = 1
    private var tick = 0L

    override fun observeActive(): Flow<List<Habit>> = habits.map { list ->
        list.filter {
            it.archivedAt == null
        }.sortedWith(compareByDescending<Habit> { it.isCore }.thenBy { it.createdAt })
    }

    override fun observeArchived(): Flow<List<Habit>> = habits.map { list ->
        list.filter { it.archivedAt != null }.sortedByDescending { it.archivedAt }
    }

    override suspend fun get(id: HabitId): Habit? = habits.value.firstOrNull { it.id == id }

    override suspend fun create(draft: HabitDraft): SaveResult<HabitId> {
        val errors = HabitValidator.validate(draft)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        val id = HabitId("habit-${nextId++}")
        habits.value = habits.value + draft.toHabit(id, Instant.ofEpochSecond(tick++))
        return SaveResult.Saved(id)
    }

    override suspend fun update(id: HabitId, draft: HabitDraft): SaveResult<Unit> {
        val old = get(id) ?: return SaveResult.Saved(Unit)
        val typed = draft.copy(type = old.type)
        val errors = HabitValidator.validate(typed)
        if (errors.isEmpty()) {
            habits.value =
                habits.value.map { if (it.id == id) typed.toHabit(id, old.createdAt) else it }
        }
        return if (errors.isEmpty()) SaveResult.Saved(Unit) else SaveResult.Invalid(errors)
    }

    override suspend fun archive(id: HabitId) {
        habits.value =
            habits.value.map {
                if (it.id ==
                    id
                ) {
                    it.copy(archivedAt = Instant.ofEpochSecond(tick++))
                } else {
                    it
                }
            }
    }

    override suspend fun restore(id: HabitId) {
        habits.value = habits.value.map { if (it.id == id) it.copy(archivedAt = null) else it }
    }

    override suspend fun deletePermanently(id: HabitId) {
        habits.value = habits.value.filterNot { it.id == id }
    }

    private fun HabitDraft.toHabit(id: HabitId, createdAt: Instant): Habit {
        val n = HabitValidator.normalize(this)
        return Habit(
            id = id,
            name = n.name,
            type = n.type,
            target = Amount(n.targetValue!!, n.targetUnit, n.targetLabel),
            baseline = Amount(n.baselineValue!!, n.baselineUnit, n.baselineLabel),
            step = n.step,
            isCore = n.isCore,
            createdAt = createdAt,
            archivedAt = null
        )
    }
}
