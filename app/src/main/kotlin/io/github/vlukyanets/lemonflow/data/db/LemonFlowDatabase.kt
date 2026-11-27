package io.github.vlukyanets.lemonflow.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        AreaEntity::class,
        ProjectEntity::class,
        ItemEntity::class,
        DayPlanEntity::class,
        PlannedItemEntity::class,
        TimeEntryEntity::class,
        BackgroundRunEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class LemonFlowDatabase : RoomDatabase()
