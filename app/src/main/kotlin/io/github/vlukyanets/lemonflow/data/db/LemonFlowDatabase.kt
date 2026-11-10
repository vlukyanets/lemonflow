package io.github.vlukyanets.lemonflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.vlukyanets.lemonflow.data.db.dao.DailyRecordDao
import io.github.vlukyanets.lemonflow.data.db.dao.DayDao
import io.github.vlukyanets.lemonflow.data.db.dao.HabitDao
import io.github.vlukyanets.lemonflow.data.db.dao.SettingsDao
import io.github.vlukyanets.lemonflow.data.db.entity.DailyRecordEntity
import io.github.vlukyanets.lemonflow.data.db.entity.DayEntity
import io.github.vlukyanets.lemonflow.data.db.entity.HabitEntity
import io.github.vlukyanets.lemonflow.data.db.entity.SettingsEntity

/** Opened over SQLCipher in the app (DatabaseHolder); plain Room in JVM tests. */
@Database(
    entities = [
        HabitEntity::class,
        DailyRecordEntity::class,
        DayEntity::class,
        SettingsEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class LemonFlowDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao

    abstract fun dailyRecordDao(): DailyRecordDao

    abstract fun dayDao(): DayDao

    abstract fun settingsDao(): SettingsDao

    companion object {
        const val FILE_NAME = "lemonflow.db"
    }
}
