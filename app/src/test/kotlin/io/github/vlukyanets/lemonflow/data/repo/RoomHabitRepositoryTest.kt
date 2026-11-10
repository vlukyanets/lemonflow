package io.github.vlukyanets.lemonflow.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.vlukyanets.lemonflow.data.db.LemonFlowDatabase
import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.domain.model.AmountUnit
import io.github.vlukyanets.lemonflow.domain.model.HabitDraft
import io.github.vlukyanets.lemonflow.domain.model.HabitId
import io.github.vlukyanets.lemonflow.domain.model.HabitType
import io.github.vlukyanets.lemonflow.domain.model.Mark
import io.github.vlukyanets.lemonflow.domain.model.SaveResult
import io.github.vlukyanets.lemonflow.domain.model.ValidationError
import io.github.vlukyanets.lemonflow.testing.MutableClock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RoomHabitRepositoryTest {
    private lateinit var db: LemonFlowDatabase
    private val clock = MutableClock.atLocal("2026-11-09T10:00")
    private lateinit var repo: RoomHabitRepository

    @Before
    fun setUp() {
        db =
            Room.inMemoryDatabaseBuilder(
                ApplicationProvider.getApplicationContext(),
                LemonFlowDatabase::class.java
            )
                .allowMainThreadQueries()
                .build()
        repo = RoomHabitRepository({ db }, clock)
    }

    @After
    fun tearDown() = db.close()

    private fun draft(name: String, type: HabitType = HabitType.CHECK, core: Boolean = false) =
        HabitDraft(
            name = name,
            type = type,
            targetValue = 30,
            targetUnit = AmountUnit.MINUTES,
            baselineValue = 1,
            baselineUnit = if (type == HabitType.CHECK) AmountUnit.PAGES else AmountUnit.MINUTES,
            isCore = core
        )

    private suspend fun create(name: String, core: Boolean = false): HabitId {
        val result = repo.create(draft(name, core = core))
        clock.advance(Duration.ofMinutes(1))
        return (result as SaveResult.Saved).value
    }

    @Test
    fun createAssignsUuidAndCreationInstantFromClock() = runTest {
        val id = create("Reading")

        UUID.fromString(id.value)
        val habit = repo.get(id)!!
        assertEquals("Reading", habit.name)
        assertEquals(Instant.parse("2026-11-09T08:00:00Z"), habit.createdAt)
        assertNull(habit.archivedAt)
    }

    @Test
    fun invalidDraftIsRejectedAndNothingWritten() = runTest {
        val result = repo.create(draft("  "))

        assertEquals(SaveResult.Invalid(listOf(ValidationError.NAME_EMPTY)), result)
        assertTrue(repo.observeActive().first().isEmpty())
    }

    @Test
    fun updateKeepsTheType() = runTest {
        val id = create("Reading")

        val result = repo.update(id, draft("Reading more", type = HabitType.TIMER))

        assertEquals(SaveResult.Saved(Unit), result)
        val habit = repo.get(id)!!
        assertEquals("Reading more", habit.name)
        assertEquals(HabitType.CHECK, habit.type)
        assertNull("still a Check habit, so no step", habit.step)
    }

    @Test
    fun activeHabitsListCoreFirstThenByCreation() = runTest {
        create("Run")
        create("Reading", core = true)
        create("Meditation")

        assertEquals(
            listOf("Reading", "Run", "Meditation"),
            repo.observeActive().first().map {
                it.name
            }
        )
    }

    @Test
    fun archiveKeepsRecordsAndRestoreBringsHabitBack() = runTest {
        val id = create("Reading")
        db.dailyRecordDao().upsert(record(id))

        repo.archive(id)
        assertTrue(repo.observeActive().first().isEmpty())
        assertEquals(listOf("Reading"), repo.observeArchived().first().map { it.name })
        assertNotNull(db.dailyRecordDao().get(id.value, "2026-11-08"))

        repo.restore(id)
        assertEquals(listOf("Reading"), repo.observeActive().first().map { it.name })
        assertTrue(repo.observeArchived().first().isEmpty())
    }

    @Test
    fun archiveListIsNewestFirst() = runTest {
        val first = create("First")
        val second = create("Second")
        repo.archive(first)
        clock.advance(Duration.ofMinutes(5))
        repo.archive(second)

        assertEquals(listOf("Second", "First"), repo.observeArchived().first().map { it.name })
    }

    @Test
    fun deletePermanentlyRemovesHabitAndRecords() = runTest {
        val id = create("Reading")
        db.dailyRecordDao().upsert(record(id))

        repo.deletePermanently(id)

        assertNull(repo.get(id))
        assertNull(db.dailyRecordDao().get(id.value, "2026-11-08"))
    }

    private fun record(id: HabitId) = DailyRecordEntity(
        id = UUID.randomUUID().toString(),
        habitId = id.value,
        date = "2026-11-08",
        mark = Mark.TARGET,
        amount = 0,
        timerStartedAt = null,
        updatedAt = 0L,
        zoneId = "Europe/Kyiv"
    )
}
