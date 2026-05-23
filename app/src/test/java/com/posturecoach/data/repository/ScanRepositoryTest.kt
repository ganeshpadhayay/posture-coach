package com.posturecoach.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.db.PostureScanDao
import com.posturecoach.data.db.entities.PostureScanEntity
import com.posturecoach.data.db.toEntity
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.testing.ScanFactory
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import java.util.Calendar
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Test

class ScanRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val dao = mockk<PostureScanDao>(relaxed = true)
    private val firestore = mockk<FirestoreSync>(relaxed = true)
    private val repo = ScanRepository(dao, firestore, json)

    @Test
    fun `save converts to entity then upserts to DAO and Firestore in order`() = runTest {
        val scan = ScanFactory.sample(id = "to-save")
        val daoSlot = slot<PostureScanEntity>()
        val fsSlot = slot<PostureScanEntity>()
        coEvery { dao.upsert(capture(daoSlot)) } just Runs
        coEvery { firestore.upsertScan(capture(fsSlot)) } just Runs

        repo.save(scan)

        coVerifyOrder {
            dao.upsert(any())
            firestore.upsertScan(any())
        }
        assertThat(daoSlot.captured.id).isEqualTo("to-save")
        assertThat(fsSlot.captured.id).isEqualTo("to-save")
        // The entity payload is what `scan.toEntity(json)` would produce.
        assertThat(daoSlot.captured).isEqualTo(scan.toEntity(json))
    }

    @Test
    fun `observeLatest maps DAO entity through domain via Turbine`() = runTest {
        val scan = ScanFactory.sample(id = "latest")
        coEvery { dao.observeLatest() } returns flowOf(scan.toEntity(json))

        repo.observeLatest().test {
            val emitted = awaitItem()
            assertThat(emitted?.id).isEqualTo("latest")
            awaitComplete()
        }
    }

    @Test
    fun `observeLatest emits null when DAO emits null`() = runTest {
        coEvery { dao.observeLatest() } returns flowOf(null)

        repo.observeLatest().test {
            assertThat(awaitItem()).isNull()
            awaitComplete()
        }
    }

    @Test
    fun `observeAll maps every entity through domain`() = runTest {
        val a = ScanFactory.sample(id = "a", timestampMs = 1L)
        val b = ScanFactory.sample(id = "b", timestampMs = 2L)
        coEvery { dao.observeAll() } returns flowOf(listOf(a.toEntity(json), b.toEntity(json)))

        repo.observeAll().test {
            val items = awaitItem()
            assertThat(items.map { it.id }).containsExactly("a", "b").inOrder()
            awaitComplete()
        }
    }

    @Test
    fun `getById delegates to DAO and maps to domain`() = runTest {
        val scan = ScanFactory.sample(id = "lookup")
        coEvery { dao.getById("lookup") } returns scan.toEntity(json)

        val out = repo.getById("lookup")

        assertThat(out?.id).isEqualTo("lookup")
        coVerify { dao.getById("lookup") }
    }

    @Test
    fun `getById returns null when DAO returns null`() = runTest {
        coEvery { dao.getById("missing") } returns null
        assertThat(repo.getById("missing")).isNull()
    }

    @Test
    fun `countToday queries DAO with start-of-today timestamp`() = runTest {
        val slot = slot<Long>()
        coEvery { dao.countSince(capture(slot)) } returns 3

        val result = repo.countToday()

        assertThat(result).isEqualTo(3)
        val cal = Calendar.getInstance().apply {
            timeInMillis = slot.captured
        }
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(cal.get(Calendar.MILLISECOND)).isEqualTo(0)
        assertThat(slot.captured).isAtMost(System.currentTimeMillis())
    }
}
