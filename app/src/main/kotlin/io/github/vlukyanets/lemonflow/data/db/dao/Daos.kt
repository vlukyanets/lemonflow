package io.github.vlukyanets.lemonflow.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.data.db.entity.DayEntity
import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.data.db.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(habit: HabitEntity)

    @Update
    suspend fun update(habit: HabitEntity)

    @Query("DELETE FROM habit WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT * FROM habit WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Query("SELECT * FROM habit WHERE archivedAt IS NULL ORDER BY isCore DESC, createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit WHERE archivedAt IS NOT NULL ORDER BY archivedAt DESC")
    fun observeArchived(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit ORDER BY isCore DESC, createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habit ORDER BY isCore DESC, createdAt ASC")
    suspend fun getAll(): List<HabitEntity>
}

@Dao
interface DailyRecordDao {
    @Upsert
    suspend fun upsert(record: DailyRecordEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrAbort(record: DailyRecordEntity)

    @Query("SELECT * FROM daily_record WHERE habitId = :habitId AND date = :date")
    suspend fun get(habitId: String, date: String): DailyRecordEntity?

    @Query("SELECT * FROM daily_record WHERE date = :date")
    fun observeForDate(date: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_record WHERE date = :date")
    suspend fun getForDate(date: String): List<DailyRecordEntity>

    /** ISO dates sort lexicographically, so a string range is a date range. */
    @Query("SELECT * FROM daily_record WHERE date >= :from AND date <= :to")
    fun observeBetween(from: String, to: String): Flow<List<DailyRecordEntity>>

    @Query("SELECT * FROM daily_record WHERE timerStartedAt IS NOT NULL")
    suspend fun getRunningTimers(): List<DailyRecordEntity>
}

@Dao
interface DayDao {
    @Upsert
    suspend fun upsert(day: DayEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertOrAbort(day: DayEntity)

    @Query("SELECT * FROM day WHERE date = :date")
    suspend fun get(date: String): DayEntity?

    @Query("SELECT * FROM day WHERE date = :date")
    fun observe(date: String): Flow<DayEntity?>
}

@Dao
interface SettingsDao {
    @Upsert
    suspend fun upsert(settings: SettingsEntity)

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun get(): SettingsEntity?

    @Query("SELECT * FROM settings LIMIT 1")
    fun observe(): Flow<SettingsEntity?>
}
