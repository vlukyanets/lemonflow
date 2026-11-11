# Contract: domain ports

The `domain` package owns these interfaces; `data` implements them; `ui` consumes them through
use cases or directly from ViewModels. Kotlin signatures are indicative - names and shapes are
the contract, bodies are not.

## HabitRepository

```kotlin
interface HabitRepository {
    fun observeActive(): Flow<List<Habit>>          // core first, then createdAt (FR-006)
    fun observeArchived(): Flow<List<Habit>>        // archivedAt descending
    suspend fun get(id: HabitId): Habit?
    suspend fun create(draft: HabitDraft): SaveResult<HabitId>
    suspend fun update(id: HabitId, draft: HabitDraft): SaveResult<Unit> // type unchanged
    suspend fun archive(id: HabitId)
    suspend fun restore(id: HabitId)
    suspend fun deletePermanently(id: HabitId)      // cascades daily records
}
```

`SaveResult` lives in `domain/model` (Kotlin's own `Result` has a single type parameter and
cannot carry validation errors):

```kotlin
sealed interface SaveResult<out T> {
    data class Saved<T>(val value: T) : SaveResult<T>
    data class Invalid(val errors: List<ValidationError>) : SaveResult<Nothing>
}
```

## DayRepository

```kotlin
interface DayRepository {
    // `today` comes from the caller's day clock (TodayViewModel), which also drives the
    // day-end rollover; the repository never decides on its own which date is "today".
    fun observeToday(today: LocalDate): Flow<TodayState>       // dayEnd, hardDay, rows per active habit
    fun observeWeek(today: LocalDate): Flow<List<DaySummary>>  // last 7 user dates ending today (FR-006a)
    suspend fun pastDay(date: LocalDate): PastDay   // read-only (FR-006b)
    suspend fun setHardDay(on: Boolean)             // today only
    suspend fun markCheck(id: HabitId, mark: Mark)  // CHECK only; OPEN = undo
    suspend fun changeAmount(id: HabitId, steps: Int) // steps = +1 or -1; the repository applies
                                                      // COUNT: step units, TIMER: step x 60 s, AVOID: 1;
                                                      // the stored amount never drops below 0
    suspend fun startTimer(id: HabitId)
    suspend fun pauseTimer(id: HabitId)
    suspend fun settleTimers()                      // stop timers whose day ended (R5)
}
```

## SettingsRepository

```kotlin
interface SettingsRepository {
    fun observeDayEndHour(): Flow<Int>              // default 3
    suspend fun setDayEndHour(hour: Int)            // 0..6 (FR-018)
}
```

## Storage availability

```kotlin
sealed interface StorageState { data object Ready; data object Unavailable }

interface StorageGate {
    suspend fun open(): StorageState                // unwrap key + open DB (research R2)
    suspend fun startFresh()                        // only after user confirmation (FR-015)
}
```

## Time

`java.time.Clock` is injected everywhere the domain needs "now"; no domain class calls
`Instant.now()` or `ZoneId.systemDefault()` directly (constitution IV).

## Invariants every implementation must keep

- Only today's records and today's `Day` row are written (FR-006b).
- Writing a record stores `updatedAt` from the clock and the clock's zone id.
- No habit name, amount or mark is ever passed to `android.util.Log` or crash reporting
  (FR-014).
