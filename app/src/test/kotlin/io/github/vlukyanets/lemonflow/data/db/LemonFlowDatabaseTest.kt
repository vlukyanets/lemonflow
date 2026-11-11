package io.github.vlukyanets.lemonflow.data.db

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.data.db.entity.DayEntity
import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.data.db.entity.SettingsEntity
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import java.io.File
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Plain Room without SQLCipher: its native library does not load on the JVM (research R9). */
@RunWith(AndroidJUnit4::class)
class LemonFlowDatabaseTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: LemonFlowDatabase

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                context,
                LemonFlowDatabase::class.java
            ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun recordForUnknownHabitIsRejected() = runTest {
        try {
            db.dailyRecordDao().upsert(record(habitId = "missing"))
            fail("foreign key must reject the record")
        } catch (expected: SQLiteConstraintException) {
            assertTrue(expected.message.orEmpty().contains("FOREIGN KEY"))
        }
    }

    @Test
    fun deletingHabitCascadesToRecords() = runTest {
        db.habitDao().insert(habit("h1"))
        db.dailyRecordDao().upsert(record(habitId = "h1"))

        db.habitDao().deleteById("h1")

        assertNull(db.dailyRecordDao().get("h1", "2026-11-09"))
    }

    @Test
    fun oneRecordPerHabitAndDate() = runTest {
        db.habitDao().insert(habit("h1"))
        db.dailyRecordDao().upsert(record(id = "r1", habitId = "h1"))
        try {
            db.dailyRecordDao().insertOrAbort(record(id = "r2", habitId = "h1"))
            fail("(habitId, date) must be unique")
        } catch (expected: SQLiteConstraintException) {
            assertTrue(expected.message.orEmpty().contains("UNIQUE"))
        }
    }

    @Test
    fun dayDateIsUnique() = runTest {
        db.dayDao().insertOrAbort(day(id = "d1"))
        try {
            db.dayDao().insertOrAbort(day(id = "d2"))
            fail("day.date must be unique")
        } catch (expected: SQLiteConstraintException) {
            assertTrue(expected.message.orEmpty().contains("UNIQUE"))
        }
    }

    @Test
    fun dataSurvivesCloseAndReopen() = runTest {
        val file = File(context.cacheDir, "persist-test.db").apply { delete() }
        fun open() = Room.databaseBuilder(context, LemonFlowDatabase::class.java, file.absolutePath)
            .allowMainThreadQueries()
            .build()

        open().apply {
            habitDao().insert(habit("h1"))
            dailyRecordDao().upsert(record(habitId = "h1", amount = 750))
            dayDao().upsert(day(id = "d1", hardDay = true))
            settingsDao().upsert(SettingsEntity(id = "s1", dayEndHour = 5, updatedAt = 1L))
            close()
        }

        open().apply {
            assertEquals("Reading", habitDao().getById("h1")?.name)
            assertEquals(750, dailyRecordDao().get("h1", "2026-11-09")?.amount)
            assertEquals(true, dayDao().get("2026-11-09")?.hardDay)
            assertEquals(5, settingsDao().get()?.dayEndHour)
            close()
        }
        file.delete()
    }

    private fun habit(id: String) = HabitEntity(
        id = id,
        name = "Reading",
        type = HabitType.CHECK,
        targetAmount = 30,
        targetUnit = AmountUnit.MINUTES,
        targetUnitLabel = null,
        baselineAmount = 1,
        baselineUnit = AmountUnit.PAGES,
        baselineUnitLabel = null,
        step = null,
        isCore = false,
        createdAt = 0L,
        archivedAt = null
    )

    private fun record(id: String = "r1", habitId: String, amount: Int = 0) = DailyRecordEntity(
        id = id,
        habitId = habitId,
        date = "2026-11-09",
        mark = Mark.OPEN,
        amount = amount,
        timerStartedAt = null,
        updatedAt = 0L,
        zoneId = "Europe/Kyiv"
    )

    private fun day(id: String, hardDay: Boolean = false) = DayEntity(
        id = id,
        date = "2026-11-09",
        hardDay = hardDay,
        updatedAt = 0L,
        zoneId = "Europe/Kyiv"
    )
}
