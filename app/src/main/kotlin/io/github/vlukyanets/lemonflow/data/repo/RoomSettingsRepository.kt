package io.github.vlukyanets.lemonflow.data.repo

import io.github.vlukyanets.lemonflow.data.db.entity.SettingsEntity
import io.github.vlukyanets.lemonflow.domain.port.SettingsRepository
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import java.time.Clock
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Settings live in the encrypted database, not in DataStore (research R4). Changing the day end
 * only affects how "now" maps to a date; stored dates are never rewritten (FR-019).
 */
@Singleton
class RoomSettingsRepository @Inject constructor(
    private val provider: DatabaseProvider,
    private val clock: Clock
) : SettingsRepository {
    private val dao get() = provider.database().settingsDao()

    override fun observeDayEndHour(): Flow<Int> = dao.observe()
        .map { it?.dayEndHour ?: UserDayCalculator.DEFAULT_DAY_END_HOUR }
        .distinctUntilChanged()

    override suspend fun setDayEndHour(hour: Int) {
        require(hour in UserDayCalculator.DAY_END_HOURS) { "Day end hour must be 0..6" }
        val existing = dao.get()
        dao.upsert(
            SettingsEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                dayEndHour = hour,
                updatedAt = clock.millis()
            )
        )
    }
}
