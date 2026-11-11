package io.github.vlukyanets.lemonflow.data.repo

import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.SaveResult
import io.github.vlukyanets.lemonflow.domain.port.HabitRepository
import io.github.vlukyanets.lemonflow.domain.rules.HabitValidator
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomHabitRepository @Inject constructor(
    private val provider: DatabaseProvider,
    private val clock: Clock
) : HabitRepository {
    private val dao get() = provider.database().habitDao()

    override fun observeActive(): Flow<List<Habit>> = dao.observeActive().map { list ->
        list.map { it.toDomain() }
    }

    override fun observeArchived(): Flow<List<Habit>> =
        dao.observeArchived().map { list -> list.map { it.toDomain() } }

    override suspend fun get(id: HabitId): Habit? = dao.getById(id.value)?.toDomain()

    override suspend fun create(draft: HabitDraft): SaveResult<HabitId> {
        val errors = HabitValidator.validate(draft)
        if (errors.isNotEmpty()) return SaveResult.Invalid(errors)
        val id = UUID.randomUUID().toString()
        dao.insert(entity(id, HabitValidator.normalize(draft), clock.millis(), archivedAt = null))
        return SaveResult.Saved(HabitId(id))
    }

    override suspend fun update(id: HabitId, draft: HabitDraft): SaveResult<Unit> {
        val stored = dao.getById(id.value) ?: return SaveResult.Saved(Unit)
        // The type is fixed at creation (FR-002a); validate against the stored one.
        val typed = draft.copy(type = stored.type)
        val errors = HabitValidator.validate(typed)
        if (errors.isEmpty()) {
            dao.update(
                entity(
                    id.value,
                    HabitValidator.normalize(typed),
                    stored.createdAt,
                    stored.archivedAt
                )
            )
        }
        return if (errors.isEmpty()) SaveResult.Saved(Unit) else SaveResult.Invalid(errors)
    }

    override suspend fun archive(id: HabitId) {
        val stored = dao.getById(id.value) ?: return
        dao.update(stored.copy(archivedAt = clock.millis()))
    }

    override suspend fun restore(id: HabitId) {
        val stored = dao.getById(id.value) ?: return
        dao.update(stored.copy(archivedAt = null))
    }

    override suspend fun deletePermanently(id: HabitId) = dao.deleteById(id.value)

    private fun entity(id: String, d: HabitDraft, createdAt: Long, archivedAt: Long?) = HabitEntity(
        id = id,
        name = d.name,
        type = d.type,
        targetAmount = requireNotNull(d.targetValue),
        targetUnit = d.targetUnit,
        targetUnitLabel = d.targetLabel.takeIf { d.targetUnit == AmountUnit.CUSTOM },
        baselineAmount = requireNotNull(d.baselineValue),
        baselineUnit = d.baselineUnit,
        baselineUnitLabel = d.baselineLabel.takeIf { d.baselineUnit == AmountUnit.CUSTOM },
        step = d.step,
        isCore = d.isCore,
        createdAt = createdAt,
        archivedAt = archivedAt
    )
}
