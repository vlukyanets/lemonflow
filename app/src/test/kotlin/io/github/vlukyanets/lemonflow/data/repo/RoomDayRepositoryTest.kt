package io.github.vlukyanets.lemonflow.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.TodayRow
import io.github.vlukyanets.lemonflow.testing.Habits
import io.github.vlukyanets.lemonflow.testing.MutableClock
import io.github.vlukyanets.lemonflow.testing.toEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomDayRepositoryTest {
    private lateinit var db: LemonFlowDatabase
    private val clock = MutableClock.atLocal("2026-11-09T10:00") // Monday
    private lateinit var repo: RoomDayRepository

    private val monday = LocalDate.parse("2026-11-09")
    private val tuesday = LocalDate.parse("2026-11-10")
    private val reading = Habits.reading()
    private val water = Habits.water()
    private val meditation = Habits.meditation()
    private val sugar = Habits.sugar()

    @Before
    fun setUp() = runTest {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LemonFlowDatabase::class.java
        ).allowMainThreadQueries().build()
        listOf(reading, water, meditation, sugar).forEach { db.habitDao().insert(it.toEntity()) }
        repo = RoomDayRepository({ db }, clock)
    }

    @After
    fun tearDown() = db.close()

    private suspend fun row(id: HabitId, date: LocalDate = monday): TodayRow =
        repo.observeToday(date).first().rows.single { it.habit.id == id }

    private suspend fun stored(id: HabitId, date: LocalDate = monday) =
        db.dailyRecordDao().get(id.value, date.toString())

    @Test
    fun todayListsActiveHabitsWithDefaults() = runTest {
        val today = repo.observeToday(monday).first()

        assertEquals(monday, today.date)
        assertEquals(3, today.dayEndHour)
        assertEquals(Instant.parse("2026-11-10T01:00:00Z"), today.dayEnd)
        assertFalse(today.hardDay)
        assertEquals(
            listOf("reading", "water", "meditation", "sugar"),
            today.rows.map {
                it.habit.id.value
            }
        )
        assertEquals(Mark.OPEN, row(reading.id).mark)
        assertEquals(Mark.TARGET, row(sugar.id).mark) // the Avoid default (FR-008)
    }

    @Test
    fun markCheckAndUndo() = runTest {
        repo.markCheck(reading.id, Mark.TARGET)
        assertEquals(Mark.TARGET, row(reading.id).mark)

        repo.markCheck(reading.id, Mark.BASELINE)
        assertEquals(Mark.BASELINE, row(reading.id).mark)

        repo.markCheck(reading.id, Mark.OPEN)
        assertEquals(Mark.OPEN, row(reading.id).mark)
    }

    @Test
    fun changeAmountAppliesTheStepPerType() = runTest {
        repo.changeAmount(water.id, +1)
        assertEquals(250, row(water.id).amount)
        assertEquals(Mark.OPEN, row(water.id).mark)
        repo.changeAmount(water.id, +1)
        assertEquals(500, row(water.id).amount)
        assertEquals(Mark.BASELINE, row(water.id).mark)

        repo.changeAmount(meditation.id, +1)
        assertEquals(60, row(meditation.id).amount)
        assertEquals(Mark.BASELINE, row(meditation.id).mark)

        repo.changeAmount(sugar.id, +1)
        assertEquals(1, row(sugar.id).amount)
        assertEquals(Mark.BASELINE, row(sugar.id).mark)
        repo.changeAmount(sugar.id, +1)
        assertEquals(Mark.OPEN, row(sugar.id).mark)
    }

    @Test
    fun amountNeverDropsBelowZeroAndTheMarkFollows() = runTest {
        repo.changeAmount(water.id, +1)
        repo.changeAmount(water.id, +1)
        repo.changeAmount(water.id, -1)
        assertEquals(250, row(water.id).amount)
        assertEquals(Mark.OPEN, row(water.id).mark)

        repo.changeAmount(water.id, -1)
        repo.changeAmount(water.id, -1)
        assertEquals(0, row(water.id).amount)

        repo.changeAmount(sugar.id, -1)
        assertEquals(0, row(sugar.id).amount)
        assertEquals(Mark.TARGET, row(sugar.id).mark)
    }

    @Test
    fun startAndPauseTimer() = runTest {
        repo.startTimer(meditation.id)
        val running = row(meditation.id)
        assertEquals(clock.now, running.timerStartedAt)
        assertTrue(running.timerRunning)

        clock.advance(Duration.ofSeconds(90))
        repo.pauseTimer(meditation.id)

        val paused = row(meditation.id)
        assertNull(paused.timerStartedAt)
        assertEquals(90, paused.amount)
        assertEquals(Mark.BASELINE, paused.mark)
    }

    @Test
    fun changingARunningTimerKeepsTheRunningTime() = runTest {
        repo.startTimer(meditation.id)
        clock.advance(Duration.ofSeconds(30))

        repo.changeAmount(meditation.id, +1)

        val record = stored(meditation.id)!!
        assertEquals(90, record.amount)
        assertEquals(clock.now.toEpochMilli(), record.timerStartedAt)
    }

    @Test
    fun onlyTodaysRecordsChange() = runTest {
        val sunday = DailyRecordEntity(
            id = UUID.randomUUID().toString(),
            habitId = reading.id.value,
            date = "2026-11-08",
            mark = Mark.BASELINE,
            amount = 0,
            timerStartedAt = null,
            updatedAt = 1L,
            zoneId = "Europe/Kyiv"
        )
        db.dailyRecordDao().upsert(sunday)

        repo.markCheck(reading.id, Mark.TARGET)
        repo.changeAmount(water.id, +1)

        assertEquals(sunday, stored(reading.id, LocalDate.parse("2026-11-08")))
        assertEquals(Mark.TARGET, stored(reading.id)!!.mark)
    }

    @Test
    fun recordsTakeUpdatedAtAndZoneFromTheClock() = runTest {
        repo.markCheck(reading.id, Mark.TARGET)

        val record = stored(reading.id)!!
        assertEquals(clock.now.toEpochMilli(), record.updatedAt)
        assertEquals("Europe/Kyiv", record.zoneId)
        UUID.fromString(record.id)
    }

    @Test
    fun oneRecordPerHabitAndDate() = runTest {
        repo.markCheck(reading.id, Mark.TARGET)
        repo.markCheck(reading.id, Mark.OPEN)
        repo.markCheck(reading.id, Mark.BASELINE)
        repo.changeAmount(water.id, +1)
        repo.changeAmount(water.id, +1)

        val records = db.dailyRecordDao().getForDate(monday.toString())
        assertEquals(2, records.size)
    }

    @Test
    fun settleTimersStopsAtTheDayEnd() = runTest {
        clock.setLocal("2026-11-09T23:00")
        repo.startTimer(meditation.id)
        clock.setLocal("2026-11-10T09:00")

        repo.settleTimers()

        val record = stored(meditation.id)!!
        assertNull(record.timerStartedAt)
        assertEquals(4 * 3600, record.amount) // 23:00 -> 03:00
        assertEquals(Mark.TARGET, record.mark)
        assertNull("the new day starts at 0", stored(meditation.id, tuesday))
    }

    @Test
    fun settleTimersKeepsTodaysTimersRunning() = runTest {
        repo.startTimer(meditation.id)
        clock.advance(Duration.ofMinutes(5))

        repo.settleTimers()

        assertNotNull(stored(meditation.id)!!.timerStartedAt)
    }

    @Test
    fun markAfterMidnightBelongsToThePreviousDate() = runTest {
        clock.setLocal("2026-11-10T01:30")
        repo.markCheck(reading.id, Mark.TARGET)

        assertEquals(Mark.TARGET, stored(reading.id, monday)?.mark)
        assertNull(stored(reading.id, tuesday))
    }

    @Test
    fun dayEndBoundaryIsExact() = runTest {
        clock.setLocal("2026-11-10T02:59:59")
        repo.markCheck(reading.id, Mark.TARGET)
        clock.setLocal("2026-11-10T03:00:00")
        repo.changeAmount(water.id, +1)

        assertNotNull(stored(reading.id, monday))
        assertNull(stored(reading.id, tuesday))
        assertNotNull(stored(water.id, tuesday))
        assertNull(stored(water.id, monday))
    }

    @Test
    fun pastDayIsReadOnlyData() = runTest {
        repo.markCheck(reading.id, Mark.BASELINE)
        repo.changeAmount(water.id, +1)
        clock.setLocal("2026-11-10T10:00")

        val past = repo.pastDay(monday)

        assertEquals(monday, past.date)
        assertFalse(past.nothingRecorded)
        val marks = past.rows.associate { it.habit.id to it.mark }
        assertEquals(Mark.BASELINE, marks[reading.id])
        assertEquals(Mark.OPEN, marks[water.id])
        assertEquals(Mark.TARGET, marks[sugar.id]) // another habit has a record that day
        assertTrue(repo.pastDay(LocalDate.parse("2026-11-05")).nothingRecorded)
    }

    @Test
    fun weekHasSevenDatesEndingToday() = runTest {
        repo.markCheck(reading.id, Mark.TARGET)

        val week = repo.observeWeek(monday).first()

        assertEquals(7, week.size)
        assertEquals(monday, week.last().date)
        assertTrue(week.last().isToday)
        assertEquals(2, week.last().completed) // Reading + the Avoid default
        assertEquals(4, week.last().active)
    }

    // Hard day (US3)

    @Test
    fun setHardDayWritesOnlyTodaysDayRow() = runTest {
        repo.setHardDay(true)

        val day = db.dayDao().get(monday.toString())!!
        UUID.fromString(day.id)
        assertTrue(day.hardDay)
        assertEquals(clock.now.toEpochMilli(), day.updatedAt)
        assertEquals("Europe/Kyiv", day.zoneId)
        assertNull(db.dayDao().get(tuesday.toString()))
        assertTrue(repo.observeToday(monday).first().hardDay)

        repo.setHardDay(false)
        assertFalse(repo.observeToday(monday).first().hardDay)
        assertEquals(day.id, db.dayDao().get(monday.toString())!!.id)
    }

    @Test
    fun nextUserDateDefaultsToOff() = runTest {
        repo.setHardDay(true)
        clock.setLocal("2026-11-10T03:00")

        assertFalse(repo.observeToday(tuesday).first().hardDay)
        assertTrue(repo.observeToday(monday).first().hardDay)
    }

    @Test
    fun togglingHardDayKeepsAllMarks() = runTest {
        repo.markCheck(reading.id, Mark.BASELINE)
        repo.changeAmount(water.id, +1)
        val before = db.dailyRecordDao().getForDate(monday.toString()).toSet()

        repo.setHardDay(true)
        repo.setHardDay(false)
        repo.setHardDay(true)

        assertEquals(before, db.dailyRecordDao().getForDate(monday.toString()).toSet())
    }
}
