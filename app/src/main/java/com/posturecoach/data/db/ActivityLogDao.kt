package com.posturecoach.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.posturecoach.data.db.entities.ActivityLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: ActivityLogEntity): Long

    @Update
    suspend fun update(log: ActivityLogEntity)

    @Query("SELECT * FROM activity_logs WHERE endTs IS NULL ORDER BY startTs DESC LIMIT 1")
    suspend fun getOpen(): ActivityLogEntity?

    @Query("SELECT * FROM activity_logs WHERE startTs >= :sinceMs ORDER BY startTs ASC")
    fun observeSince(sinceMs: Long): Flow<List<ActivityLogEntity>>

    @Query("SELECT * FROM activity_logs WHERE startTs >= :sinceMs ORDER BY startTs ASC")
    suspend fun getSince(sinceMs: Long): List<ActivityLogEntity>

    @Query("DELETE FROM activity_logs WHERE startTs < :olderThanMs")
    suspend fun prune(olderThanMs: Long)
}
