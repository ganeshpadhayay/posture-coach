package com.posturecoach.data.repository

import com.posturecoach.data.db.ActivityLogDao
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.firebase.FirestoreSync
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
open class ActivityRepository @Inject constructor(
    private val dao: ActivityLogDao,
    private val firestore: FirestoreSync,
) {

    open fun observeToday(): Flow<List<ActivityLogEntity>> =
        dao.observeSince(startOfTodayMs())

    open fun observeStillMillisToday(): Flow<Long> =
        observeToday().map { logs -> stillDurationMs(logs, System.currentTimeMillis()) }

    open suspend fun beginTransition(type: String, atMs: Long = System.currentTimeMillis()) {
        val open = dao.getOpen()
        if (open != null) {
            if (open.type == type) return
            dao.update(open.copy(endTs = atMs))
            firestore.upsertActivity(open.copy(endTs = atMs))
        }
        val newEntity = ActivityLogEntity(startTs = atMs, endTs = null, type = type)
        val newId = dao.insert(newEntity)
        firestore.upsertActivity(newEntity.copy(id = newId))
    }

    open suspend fun currentStillDurationMs(nowMs: Long = System.currentTimeMillis()): Long {
        val open = dao.getOpen() ?: return 0L
        if (open.type != ActivityType.STILL) return 0L
        return (nowMs - open.startTs).coerceAtLeast(0L)
    }

    open suspend fun stillDurationTodayMs(nowMs: Long = System.currentTimeMillis()): Long {
        val logs = dao.getSince(startOfTodayMs())
        return stillDurationMs(logs, nowMs)
    }

    open suspend fun pruneOlderThan(days: Int) {
        val cutoff = System.currentTimeMillis() - days * 24L * 60 * 60 * 1000
        dao.prune(cutoff)
    }

    private fun stillDurationMs(logs: List<ActivityLogEntity>, nowMs: Long): Long {
        var total = 0L
        logs.forEach { log ->
            if (log.type != ActivityType.STILL) return@forEach
            val end = log.endTs ?: nowMs
            total += (end - log.startTs).coerceAtLeast(0L)
        }
        return total
    }

    private fun startOfTodayMs(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}
