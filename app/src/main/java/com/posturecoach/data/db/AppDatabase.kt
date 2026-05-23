package com.posturecoach.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.PostureScanEntity

@Database(
    entities = [PostureScanEntity::class, ActivityLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postureScanDao(): PostureScanDao
    abstract fun activityLogDao(): ActivityLogDao
}
