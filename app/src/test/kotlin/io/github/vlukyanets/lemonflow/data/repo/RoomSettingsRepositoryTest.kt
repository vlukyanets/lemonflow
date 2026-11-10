package io.github.vlukyanets.lemonflow.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.testing.Habits
import io.github.vlukyanets.lemonflow.testing.MutableClock
import io.github.vlukyanets.lemonflow.testing.toEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomSettingsRepositoryTest {
    private lateinit var db: LemonFlowDatabase
    private val clock = MutableClock.atLocal("2026-11-10T01:30")
    private lateinit var settings: RoomSettingsRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            LemonFlowDatabase::class.java
        ).allowMainThreadQueries().build()
        settings = RoomSettingsRepository({ db }, clock)
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun defaultDayEndIsThree() = runTest {
        assertEquals(3, settings.observeDayEndHour().first())
    }

    @Test
    fun acceptsZeroToSix() = runTest {
        (0..6).forEach { hour ->
            settings.setDayEndHour(hour)
            assertEquals(hour, settings.observeDayEndHour().first())
        }
        assertEquals(clock.now.toEpochMilli(), db.settingsDao().get()!!.updatedAt)
    }

    @Test
    fun rejectsOtherHours() = runTest {
        settings.setDayEndHour(5)
        listOf(-1, 7, 12, 23).forEach { hour ->
            assertThrows(IllegalArgumentException::class.java) {
                kotlinx.coroutines.runBlocking { settings.setDayEndHour(hour) }
            }
        }
        assertEquals(5, settings.observeDayEndHour().first())
    }

    @Test
    fun keepsOneSettingsRow() = runTest {
        settings.setDayEndHour(1)
        val id = db.settingsDao().get()!!.id
        settings.setDayEndHour(2)
        assertEquals(id, db.settingsDao().get()!!.id)
    }

    @Test
    fun changingTheDayEndNeverRewritesStoredDates() = runTest {
        db.habitDao().insert(Habits.reading().toEntity())
        val days = RoomDayRepository({ db }, clock)
        // 01:30 with day end 03:00 belongs to Monday.
        days.markCheck(Habits.reading().id, Mark.TARGET)
        days.setHardDay(true)

        settings.setDayEndHour(0)

        val records = db.dailyRecordDao()
        assertNotNull(records.get("reading", "2026-11-09"))
        assertNull(records.get("reading", "2026-11-10"))
        assertTrue(db.dayDao().get("2026-11-09")!!.hardDay)
        assertNull(db.dayDao().get("2026-11-10"))
    }
}
