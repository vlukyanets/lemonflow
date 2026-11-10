package io.github.vlukyanets.lemonflow.data.repo

import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.data.db.entity.DayEntity
import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.TodayRow
import io.github.vlukyanets.lemonflow.domain.model.TodayState
import io.github.vlukyanets.lemonflow.domain.port.DayRepository
import io.github.vlukyanets.lemonflow.domain.rules.DayBounds
import io.github.vlukyanets.lemonflow.domain.rules.MarkRules
import io.github.vlukyanets.lemonflow.domain.rules.TimerRules
import io.github.vlukyanets.lemonflow.domain.rules.WeekProgress
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Today, the week strip and past days on top of Room. Every write goes to the clock's current
 * user date only (FR-006b) and stores the clock's instant and zone.
 */
@Singleton
class RoomDayRepository @Inject constructor(
    private val provider: DatabaseProvider,
    private val clock: Clock
) : DayRepository {
    private val db get() = provider.database()

    /** Read-modify-write steps must not interleave, e.g. two quick taps on "+". */
    private val writeLock = Mutex()

    override fun observeToday(today: LocalDate): Flow<TodayState> {
        val date = today.toString()
        return combine(
            db.habitDao().observeActive(),
            db.dailyRecordDao().observeForDate(date),
            db.dayDao().observe(date),
            db.settingsDao().observe()
        ) { habits, records, day, settings ->
            val hour = settings?.dayEndHour ?: UserDayCalculator.DEFAULT_DAY_END_HOUR
            val byHabit = records.associateBy { it.habitId }
            TodayState(
                date = today,
                dayEnd = UserDayCalculator.dayEnd(today, clock.zone, hour),
                dayEndHour = hour,
                hardDay = day?.hardDay == true,
                rows = habits.map { entity ->
                    val habit = entity.toDomain()
                    val record = byHabit[entity.id]
                    TodayRow(
                        habit = habit,
                        mark = record?.mark ?: MarkRules.markFor(habit, 0),
                        amount = record?.amount ?: 0,
                        timerStartedAt = record?.timerStartedAt?.let(Instant::ofEpochMilli)
                    )
                }
            )
        }
    }

    override fun observeWeek(today: LocalDate): Flow<List<DaySummary>> = combine(
        db.habitDao().observeAll(),
        db.dailyRecordDao().observeBetween(
            today.minusDays(WEEK_DAYS - 1).toString(),
            today.toString()
        ),
        db.settingsDao().observe()
    ) { habits, records, settings ->
        val hour = settings?.dayEndHour ?: UserDayCalculator.DEFAULT_DAY_END_HOUR
        WeekProgress.week(
            today,
            habits.map { it.toDomain() },
            records.map { it.toDomain() },
            DayBounds.of(clock.zone, hour)
        )
    }

    override suspend fun pastDay(date: LocalDate): PastDay = WeekProgress.pastDay(
        date,
        db.habitDao().getAll().map { it.toDomain() },
        db.dailyRecordDao().getForDate(date.toString()).map { it.toDomain() },
        DayBounds.of(clock.zone, db.dayEndHour())
    )

    override suspend fun setHardDay(on: Boolean) = writeLock.withLock {
        val now = clock.instant()
        val date = UserDayCalculator.userDate(now, clock.zone, db.dayEndHour()).toString()
        val existing = db.dayDao().get(date)
        db.dayDao().upsert(
            DayEntity(
                id = existing?.id ?: UUID.randomUUID().toString(),
                date = date,
                hardDay = on,
                updatedAt = now.toEpochMilli(),
                zoneId = clock.zone.id
            )
        )
    }

    override suspend fun markCheck(id: HabitId, mark: Mark) = writeToday(id) { habit, record, _ ->
        if (habit.type == HabitType.CHECK) record.copy(mark = mark) else null
    }

    override suspend fun changeAmount(id: HabitId, steps: Int) =
        writeToday(id) { habit, record, dayEnd ->
            if (habit.type == HabitType.CHECK) return@writeToday null
            val now = clock.instant()
            // A running timer keeps running: fold its time in and restart it from now.
            val current =
                TimerRules.elapsedSeconds(record.amount, record.timerStartedAt, now, dayEnd)
            val amount = (current + steps * MarkRules.stepSize(habit)).coerceAtLeast(0)
            record.copy(
                amount = amount,
                mark = MarkRules.markFor(habit, amount),
                timerStartedAt = record.timerStartedAt?.let { now }
            )
        }

    override suspend fun startTimer(id: HabitId) = writeToday(id) { habit, record, _ ->
        if (habit.type != HabitType.TIMER || record.timerStartedAt != null) {
            null
        } else {
            record.copy(timerStartedAt = clock.instant())
        }
    }

    override suspend fun pauseTimer(id: HabitId) = writeToday(id) { habit, record, dayEnd ->
        if (record.timerStartedAt == null) return@writeToday null
        val amount =
            TimerRules.elapsedSeconds(record.amount, record.timerStartedAt, clock.instant(), dayEnd)
        record.copy(amount = amount, mark = MarkRules.markFor(habit, amount), timerStartedAt = null)
    }

    override suspend fun settleTimers() = writeLock.withLock {
        val now = clock.instant()
        val bounds = DayBounds.of(clock.zone, db.dayEndHour())
        val running = db.dailyRecordDao().getRunningTimers().map { it.toDomain() }
        TimerRules.settle(running, now) { bounds.of(it).second }.forEach { settled ->
            val habit = db.habitDao().getById(settled.habitId.value)?.toDomain() ?: return@forEach
            db.dailyRecordDao().upsert(
                settled.copy(
                    mark = MarkRules.markFor(habit, settled.amount),
                    updatedAt = now,
                    zoneId = clock.zone
                ).toEntity()
            )
        }
    }

    /**
     * Applies [change] to the habit's record for the current user date, creating it if needed.
     * [change] gets the day's end instant and returns null to leave everything untouched.
     */
    private suspend fun writeToday(
        id: HabitId,
        change: (Habit, DailyRecord, Instant) -> DailyRecord?
    ) = writeLock.withLock {
        val habit = db.habitDao().getById(id.value)?.toDomain() ?: return@withLock
        val now = clock.instant()
        val hour = db.dayEndHour()
        val date = UserDayCalculator.userDate(now, clock.zone, hour)
        val current = db.dailyRecordDao().get(id.value, date.toString())?.toDomain()
            ?: DailyRecord(
                id = UUID.randomUUID().toString(),
                habitId = id,
                date = date,
                mark = MarkRules.markFor(habit, 0),
                amount = 0,
                timerStartedAt = null,
                updatedAt = now,
                zoneId = clock.zone
            )
        val changed = change(habit, current, UserDayCalculator.dayEnd(date, clock.zone, hour))
            ?: return@withLock
        db.dailyRecordDao().upsert(changed.copy(updatedAt = now, zoneId = clock.zone).toEntity())
    }

    private companion object {
        const val WEEK_DAYS = 7L
    }
}

private suspend fun LemonFlowDatabase.dayEndHour(): Int =
    settingsDao().get()?.dayEndHour ?: UserDayCalculator.DEFAULT_DAY_END_HOUR
