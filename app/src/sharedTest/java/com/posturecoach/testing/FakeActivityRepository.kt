package com.posturecoach.testing

import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.data.repository.ActivityRepository
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeActivityRepository : ActivityRepository(
    dao = mockk(relaxed = true),
    firestore = mockk<FirestoreSync>(relaxed = true),
) {
    private val logs = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
    var currentStillMs: Long = 0L
    var stillTodayMs: Long = 0L
    val recordedTransitions = mutableListOf<Pair<String, Long>>()

    override fun observeToday(): Flow<List<ActivityLogEntity>> = logs.asStateFlow()

    override fun observeStillMillisToday(): Flow<Long> =
        logs.asStateFlow().map { stillTodayMs }

    override suspend fun beginTransition(type: String, atMs: Long) {
        recordedTransitions += type to atMs
    }

    override suspend fun currentStillDurationMs(nowMs: Long): Long = currentStillMs

    override suspend fun stillDurationTodayMs(nowMs: Long): Long = stillTodayMs

    override suspend fun pruneOlderThan(days: Int) = Unit

    fun seed(vararg logs: ActivityLogEntity) {
        this.logs.value = logs.toList()
    }
}
