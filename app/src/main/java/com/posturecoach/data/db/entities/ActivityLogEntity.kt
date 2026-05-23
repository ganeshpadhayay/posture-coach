package com.posturecoach.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTs: Long,
    val endTs: Long?,
    val type: String,
)

object ActivityType {
    const val STILL = "STILL"
    const val MOVING = "MOVING"
}
