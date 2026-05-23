package com.posturecoach.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.posturecoach.data.db.entities.PostureScanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PostureScanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(scan: PostureScanEntity)

    @Query("SELECT * FROM posture_scans ORDER BY timestampMs DESC LIMIT 1")
    fun observeLatest(): Flow<PostureScanEntity?>

    @Query("SELECT * FROM posture_scans WHERE id = :id")
    suspend fun getById(id: String): PostureScanEntity?

    @Query("SELECT * FROM posture_scans ORDER BY timestampMs DESC")
    fun observeAll(): Flow<List<PostureScanEntity>>

    @Query("SELECT COUNT(*) FROM posture_scans WHERE timestampMs >= :sinceMs")
    suspend fun countSince(sinceMs: Long): Int
}
