package com.posturecoach.testing

import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.domain.model.PostureScan
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

/**
 * In-memory [ScanRepository] for ViewModel tests. Persists scans in a [MutableStateFlow]
 * so collectors observe insertions immediately. All Firestore/DAO/Json dependencies are
 * relaxed mocks because none of the overridden methods touch them.
 */
class FakeScanRepository : ScanRepository(
    dao = mockk(relaxed = true),
    firestore = mockk<FirestoreSync>(relaxed = true),
    json = Json,
) {
    private val scans = MutableStateFlow<List<PostureScan>>(emptyList())
    var countTodayResult: Int = 0

    override fun observeLatest(): Flow<PostureScan?> =
        scans.asStateFlow().map { list -> list.maxByOrNull { it.timestampMs } }

    override fun observeAll(): Flow<List<PostureScan>> =
        scans.asStateFlow().map { list -> list.sortedByDescending { it.timestampMs } }

    override suspend fun getById(id: String): PostureScan? =
        scans.value.firstOrNull { it.id == id }

    override suspend fun save(scan: PostureScan) {
        scans.value = scans.value.filter { it.id != scan.id } + scan
    }

    override suspend fun countToday(): Int = countTodayResult

    fun seed(vararg scans: PostureScan) {
        this.scans.value = scans.toList()
    }
}
