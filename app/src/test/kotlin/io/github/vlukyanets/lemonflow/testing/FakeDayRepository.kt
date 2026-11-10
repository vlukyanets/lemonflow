package io.github.vlukyanets.lemonflow.testing

import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.TodayRow
import io.github.vlukyanets.lemonflow.domain.model.TodayState
import io.github.vlukyanets.lemonflow.domain.port.DayRepository
import io.github.vlukyanets.lemonflow.domain.port.SettingsRepository
import io.github.vlukyanets.lemonflow.domain.rules.DayBounds
import io.github.vlukyanets.lemonflow.domain.rules.MarkRules
import io.github.vlukyanets.lemonflow.domain.rules.TimerRules
import io.github.vlukyanets.lemonflow.domain.rules.WeekProgress
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine

/** In-memory settings; "Day ends at" defaults to 3. */
class FakeSettingsRepository(hour: Int = 3) : SettingsRepository {
    val hour = MutableStateFlow(hour)

    override fun observeDayEndHour(): Flow<Int> = hour

    override suspend fun setDayEndHour(hour: Int) {
        require(hour in 0..6)
        this.hour.value = hour
    }
}

/** In-memory DayRepository built on the real domain rules; writes go to the clock's user date. */
class FakeDayRepository(
    private val habits: FakeHabitRepository,
    private val settings: FakeSettingsRepository,
    private val clock: MutableClock
) : DayRepository {
    val records = MutableStateFlow<Map<Pair<HabitId, LocalDate>, DailyRecord>>(emptyMap())
    val hardDays = MutableStateFlow<Set<LocalDate>>(emptySet())
    var settleCalls = 0
        private set

    private val bounds get() = bounds(settings.hour.value)

    private fun bounds(hour: Int) = DayBounds { date ->
        UserDayCalculator.dayStart(date, clock.zone, hour) to
            UserDayCalculator.dayEnd(date, clock.zone, hour)
    }

    fun today(): LocalDate =
        UserDayCalculator.userDate(clock.instant(), clock.zone, settings.hour.value)

    /** Seeds a record directly, e.g. for a past day. */
    fun put(id: HabitId, date: LocalDate, mark: Mark, amount: Int = 0) {
        val record = DailyRecord(
            "rec-${id.value}-$date",
            id,
            date,
            mark,
            amount,
            null,
            clock.instant(),
            clock.zone
        )
        records.value = records.value + ((id to date) to record)
    }

    override fun observeToday(today: LocalDate): Flow<TodayState> =
        combine(habits.observeActive(), records, hardDays, settings.hour) {
                active,
                all,
                hard,
                hour
            ->
            TodayState(
                date = today,
                dayEnd = bounds(hour).of(today).second,
                dayEndHour = hour,
                hardDay = today in hard,
                rows = active.map { habit ->
                    val record = all[habit.id to today]
                    TodayRow(
                        habit = habit,
                        mark = record?.mark ?: MarkRules.markFor(habit, 0),
                        amount = record?.amount ?: 0,
                        timerStartedAt = record?.timerStartedAt
                    )
                }
            )
        }

    override fun observeWeek(today: LocalDate): Flow<List<DaySummary>> =
        combine(habits.habits, records, settings.hour) { all, recs, hour ->
            WeekProgress.week(today, all, recs.values.toList(), bounds(hour))
        }

    override suspend fun pastDay(date: LocalDate): PastDay = WeekProgress.pastDay(
        date,
        habits.habits.value,
        records.value.values.filter { it.date == date },
        bounds
    )

    override suspend fun setHardDay(on: Boolean) {
        hardDays.value = if (on) hardDays.value + today() else hardDays.value - today()
    }

    override suspend fun markCheck(id: HabitId, mark: Mark) = edit(id) { it.copy(mark = mark) }

    override suspend fun changeAmount(id: HabitId, steps: Int) {
        val habit = requireNotNull(habits.get(id))
        edit(id) { record ->
            val now = clock.instant()
            val base = TimerRules.elapsedSeconds(
                record.amount,
                record.timerStartedAt,
                now,
                bounds.of(record.date).second
            )
            val amount = (base + steps * MarkRules.stepSize(habit)).coerceAtLeast(0)
            record.copy(
                amount = amount,
                mark = MarkRules.markFor(habit, amount),
                timerStartedAt = record.timerStartedAt?.let { now }
            )
        }
    }

    override suspend fun startTimer(id: HabitId) = edit(id) {
        if (it.timerStartedAt == null) it.copy(timerStartedAt = clock.instant()) else it
    }

    override suspend fun pauseTimer(id: HabitId) {
        val habit = requireNotNull(habits.get(id))
        edit(id) { record ->
            val elapsed = TimerRules.elapsedSeconds(
                record.amount,
                record.timerStartedAt,
                clock.instant(),
                bounds.of(record.date).second
            )
            record.copy(
                amount = elapsed,
                timerStartedAt = null,
                mark = MarkRules.markFor(habit, elapsed)
            )
        }
    }

    override suspend fun settleTimers() {
        settleCalls++
        val settled = TimerRules.settle(records.value.values.toList(), clock.instant()) { date ->
            bounds.of(date).second
        }
        records.value = records.value + settled.associateBy { it.habitId to it.date }
    }

    private suspend fun edit(id: HabitId, change: (DailyRecord) -> DailyRecord) {
        val habit = requireNotNull(habits.get(id))
        val date = today()
        val current = records.value[id to date] ?: DailyRecord(
            "rec-${id.value}-$date",
            id,
            date,
            if (habit.type == HabitType.CHECK) Mark.OPEN else MarkRules.markFor(habit, 0),
            0,
            null,
            clock.instant(),
            clock.zone
        )
        records.value = records.value + ((id to date) to change(current))
    }
}
