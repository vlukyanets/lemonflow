package io.github.vlukyanets.lemonflow.domain.rules

import io.github.vlukyanets.lemonflow.domain.model.DailyRecord
import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.PastDayRow
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Start and end instant of a user date (research R3). */
fun interface DayBounds {
    fun of(date: LocalDate): Pair<Instant, Instant>

    companion object {
        /** User dates in [zone] with the day ending at [dayEndHour]. */
        fun of(zone: ZoneId, dayEndHour: Int) = DayBounds { date ->
            UserDayCalculator.dayStart(date, zone, dayEndHour) to
                UserDayCalculator.dayEnd(date, zone, dayEndHour)
        }
    }
}

/** The 7-day strip and past days (FR-006a/b, FR-008, research R10). */
object WeekProgress {
    private const val DAYS = 7L

    /** Created before the day ended and not archived before it started (research R10). */
    fun isActiveOn(habit: Habit, dayStart: Instant, dayEnd: Instant): Boolean {
        val archivedAt = habit.archivedAt
        return habit.createdAt.isBefore(dayEnd) &&
            (archivedAt == null || archivedAt.isAfter(dayStart))
    }

    /**
     * The mark a habit has on a date. An AVOID habit without a record counts as TARGET today,
     * and on an earlier date only if that date has any record at all (FR-008).
     */
    fun effectiveMark(
        habit: Habit,
        record: DailyRecord?,
        isToday: Boolean,
        dateHasRecords: Boolean
    ): Mark = when {
        record != null -> record.mark
        habit.type == HabitType.AVOID && (isToday || dateHasRecords) -> Mark.TARGET
        else -> Mark.OPEN
    }

    /** One strip cell; [records] may contain other dates, which are ignored. */
    fun summary(
        date: LocalDate,
        habits: List<Habit>,
        records: List<DailyRecord>,
        today: LocalDate,
        bounds: DayBounds
    ): DaySummary {
        val onDate = records.filter { it.date == date }
        val byHabit = onDate.associateBy { it.habitId }
        val (start, end) = bounds.of(date)
        val active = habits.filter { isActiveOn(it, start, end) }
        val isToday = date == today
        val completed = active.count { habit ->
            MarkRules.isComplete(
                effectiveMark(habit, byHabit[habit.id], isToday, onDate.isNotEmpty())
            )
        }
        return DaySummary(
            date = date,
            completed = completed,
            active = active.size,
            nothingRecorded = onDate.isEmpty() && completed == 0,
            isToday = isToday
        )
    }

    /** The last 7 user dates ending with [today], oldest first. */
    fun week(
        today: LocalDate,
        habits: List<Habit>,
        records: List<DailyRecord>,
        bounds: DayBounds
    ): List<DaySummary> = (DAYS - 1 downTo 0L).map { back ->
        summary(today.minusDays(back), habits, records, today, bounds)
    }

    /**
     * A past date, read-only: the habits active then, in the given order, with their effective
     * marks.
     */
    fun pastDay(
        date: LocalDate,
        habits: List<Habit>,
        records: List<DailyRecord>,
        bounds: DayBounds
    ): PastDay {
        val onDate = records.filter { it.date == date }
        val byHabit = onDate.associateBy { it.habitId }
        val (start, end) = bounds.of(date)
        val rows = habits
            .filter { isActiveOn(it, start, end) }
            .map { habit ->
                val record = byHabit[habit.id]
                PastDayRow(
                    habit = habit,
                    mark = effectiveMark(habit, record, isToday = false, onDate.isNotEmpty()),
                    amount = record?.amount ?: 0
                )
            }
        return PastDay(date = date, rows = rows, nothingRecorded = onDate.isEmpty())
    }
}
