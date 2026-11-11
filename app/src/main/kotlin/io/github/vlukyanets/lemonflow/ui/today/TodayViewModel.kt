package io.github.vlukyanets.lemonflow.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.vlukyanets.lemonflow.domain.model.Amount
import io.github.vlukyanets.lemonflow.domain.model.DaySummary
import io.github.vlukyanets.lemonflow.domain.model.Habit
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.PastDay
import io.github.vlukyanets.lemonflow.domain.model.TodayState
import io.github.vlukyanets.lemonflow.domain.port.DayRepository
import io.github.vlukyanets.lemonflow.domain.port.SettingsRepository
import io.github.vlukyanets.lemonflow.domain.rules.MarkRules
import io.github.vlukyanets.lemonflow.domain.rules.TimerRules
import io.github.vlukyanets.lemonflow.domain.time.UserDayCalculator
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One Today row as shown. [amount] includes a running timer's time; [shownMark] is the mark as
 * presented, where a hard day shows a Baseline mark as done (FR-011).
 */
data class TodayRowUi(
    val habit: Habit,
    val mark: Mark,
    val shownMark: Mark,
    val amount: Int,
    val goal: Amount,
    val running: Boolean
) {
    val minTag: Boolean get() = shownMark == Mark.BASELINE
}

data class TodayUiState(
    val loading: Boolean = true,
    val date: LocalDate = LocalDate.MIN,
    val dayEndHour: Int = UserDayCalculator.DEFAULT_DAY_END_HOUR,
    val hardDay: Boolean = false,
    val rows: List<TodayRowUi> = emptyList(),
    val week: List<DaySummary> = emptyList(),
    val pastDay: PastDay? = null
) {
    val doneCount: Int get() = rows.count { MarkRules.isComplete(it.mark) }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TodayViewModel @Inject constructor(
    private val days: DayRepository,
    settings: SettingsRepository,
    private val clock: Clock
) : ViewModel() {
    /** Bumped on every resume so "today" is recomputed after the app was in the background. */
    private val refresh = MutableStateFlow(0)
    private val pastDay = MutableStateFlow<PastDay?>(null)

    /** The current user date; it switches at the exact day-end instant even while open. */
    private val currentDate: Flow<LocalDate> =
        combine(settings.observeDayEndHour(), refresh) { hour, _ -> hour }
            .flatMapLatest { hour -> dayClock(clock, hour) }
            .distinctUntilChanged()

    val state: StateFlow<TodayUiState> = currentDate
        .flatMapLatest { date ->
            flow {
                // Timers whose day ended stop first (research R5): on start, resume and rollover.
                days.settleTimers()
                emitAll(combine(days.observeToday(date), days.observeWeek(date), ::Pair))
            }
        }
        .flatMapLatest { (today, week) ->
            val ticks = if (today.rows.any { it.timerRunning }) secondTicks() else flowOf(Unit)
            combine(ticks, pastDay) { _, past ->
                todayUiState(today, week, past, clock.instant())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), TodayUiState())

    fun onResume() {
        refresh.value++
        viewModelScope.launch { days.settleTimers() }
    }

    /** The Check row's single tap (FR-007); the screen is subscribed, so [state] is current. */
    fun toggleDone(row: TodayRowUi) = launch {
        days.markCheck(row.habit.id, MarkRules.nextCheck(row.mark, state.value.hardDay))
    }

    fun changeAmount(id: HabitId, steps: Int) = launch { days.changeAmount(id, steps) }

    fun toggleTimer(row: TodayRowUi) = launch {
        if (row.running) days.pauseTimer(row.habit.id) else days.startTimer(row.habit.id)
    }

    fun setHardDay(on: Boolean) = launch { days.setHardDay(on) }

    fun openPastDay(date: LocalDate) = launch { pastDay.value = days.pastDay(date) }

    fun closePastDay() {
        pastDay.value = null
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Emits the user date now, then again at each exact day-end instant (one tick per day). */
private fun dayClock(clock: Clock, hour: Int): Flow<LocalDate> = flow {
    while (true) {
        val now = clock.instant()
        val date = UserDayCalculator.userDate(now, clock.zone, hour)
        emit(date)
        val end = UserDayCalculator.dayEnd(date, clock.zone, hour)
        delay(Duration.between(now, end).toMillis().coerceAtLeast(1))
    }
}

/** Once a second; Today only listens while some timer runs. */
private fun secondTicks(): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(TICK_MS)
    }
}

private const val TICK_MS = 1_000L

private fun todayUiState(
    today: TodayState,
    week: List<DaySummary>,
    past: PastDay?,
    now: Instant
): TodayUiState = TodayUiState(
    loading = false,
    date = today.date,
    dayEndHour = today.dayEndHour,
    hardDay = today.hardDay,
    rows = today.rows.map { row ->
        val habit = row.habit
        val isTimer = habit.type == HabitType.TIMER
        val amount = if (isTimer) {
            TimerRules.elapsedSeconds(row.amount, row.timerStartedAt, now, today.dayEnd)
        } else {
            row.amount
        }
        // A running timer's stored mark is from its last change; derive it from live time.
        val liveTimer = isTimer && row.timerRunning
        val mark = if (liveTimer) MarkRules.markFor(habit, amount) else row.mark
        TodayRowUi(
            habit = habit,
            mark = mark,
            shownMark = MarkRules.shown(mark, today.hardDay),
            amount = amount,
            goal = MarkRules.goal(habit, today.hardDay),
            running = row.timerRunning
        )
    },
    week = week,
    pastDay = past
)
