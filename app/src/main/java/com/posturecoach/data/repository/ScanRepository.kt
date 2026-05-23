package com.posturecoach.data.repository

import com.posturecoach.data.db.PostureScanDao
import com.posturecoach.data.db.toDomain
import com.posturecoach.data.db.toEntity
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.domain.model.PostureScan
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

@Singleton
open class ScanRepository @Inject constructor(
    private val dao: PostureScanDao,
    private val firestore: FirestoreSync,
    private val json: Json,
) {

    open fun observeLatest(): Flow<PostureScan?> =
        dao.observeLatest().map { it?.toDomain(json) }

    open fun observeAll(): Flow<List<PostureScan>> =
        dao.observeAll().map { list -> list.map { it.toDomain(json) } }

    open suspend fun getById(id: String): PostureScan? = dao.getById(id)?.toDomain(json)

    open suspend fun save(scan: PostureScan) {
        val entity = scan.toEntity(json)
        dao.upsert(entity)
        firestore.upsertScan(entity)
    }

    open suspend fun countToday(): Int {
        val start = startOfTodayMs()
        return dao.countSince(start)
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
