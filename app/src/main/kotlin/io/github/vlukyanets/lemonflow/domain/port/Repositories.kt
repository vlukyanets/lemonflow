package io.github.vlukyanets.lemonflow.domain.port

import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.SaveResult
import io.github.vlukyanets.lemonflow.domain.model.TodayState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Habits and their lifecycle (contracts/domain-ports.md). */
interface HabitRepository {
    /** Active habits, core first, then by creation (FR-006). */
    fun observeActive(): Flow<List<Habit>>

    /** Archived habits, most recently archived first. */
    fun observeArchived(): Flow<List<Habit>>

    suspend fun get(id: HabitId): Habit?

    suspend fun create(draft: HabitDraft): SaveResult<HabitId>

    /** The type never changes; a draft with another type keeps the stored one (FR-002a). */
    suspend fun update(id: HabitId, draft: HabitDraft): SaveResult<Unit>

    suspend fun archive(id: HabitId)

    suspend fun restore(id: HabitId)

    /** Removes the habit and all its daily records (FR-004b). */
    suspend fun deletePermanently(id: HabitId)
}

/** Today, the week strip and past days. Only today's data is ever written (FR-006b). */
interface DayRepository {
    /** [today] is the current user date, supplied by the caller's own day clock. */
    fun observeToday(today: LocalDate): Flow<TodayState>

    fun observeWeek(today: LocalDate): Flow<List<DaySummary>>

    suspend fun pastDay(date: LocalDate): PastDay

    suspend fun setHardDay(on: Boolean)

    /** CHECK habits only; [Mark.OPEN] undoes the mark. */
    suspend fun markCheck(id: HabitId, mark: Mark)

    /**
     * [steps] is +1 or -1. The repository applies the size: COUNT `step` units,
     * TIMER `step` x 60 seconds, AVOID 1. The amount never drops below 0.
     */
    suspend fun changeAmount(id: HabitId, steps: Int)

    suspend fun startTimer(id: HabitId)

    suspend fun pauseTimer(id: HabitId)

    /** Stops every running timer whose day has ended, keeping the time up to the day end. */
    suspend fun settleTimers()
}

interface SettingsRepository {
    /** Default 3 (FR-018). */
    fun observeDayEndHour(): Flow<Int>

    /** Accepts 0..6 only. */
    suspend fun setDayEndHour(hour: Int)
}
