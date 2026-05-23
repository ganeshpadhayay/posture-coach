package com.posturecoach.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "posture_scans")
data class PostureScanEntity(
    @PrimaryKey val id: String,
    val timestampMs: Long,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val issuesCsv: String,
    val anglesJson: String,
    val landmarksJson: String,
)
