package com.posturecoach.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.ScanFactory
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostureScanDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PostureScanDao
    private val json = Json { ignoreUnknownKeys = true }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.postureScanDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsertThenGetById() = runTest {
        val entity = ScanFactory.sample(id = "x").toEntity(json)
        dao.upsert(entity)
        assertThat(dao.getById("x")).isEqualTo(entity)
    }

    @Test
    fun upsertReplacesExistingRow() = runTest {
        val a = ScanFactory.sample(id = "same", timestampMs = 1L).toEntity(json)
        val b = ScanFactory.sample(id = "same", timestampMs = 2L).toEntity(json)
        dao.upsert(a)
        dao.upsert(b)
        assertThat(dao.getById("same")?.timestampMs).isEqualTo(2L)
        assertThat(dao.countSince(0L)).isEqualTo(1)
    }

    @Test
    fun observeLatestEmitsMostRecentByTimestamp() = runTest {
        dao.upsert(ScanFactory.sample(id = "old", timestampMs = 1L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "new", timestampMs = 100L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "mid", timestampMs = 50L).toEntity(json))

        dao.observeLatest().test {
            val latest = awaitItem()
            assertThat(latest?.id).isEqualTo("new")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeAllReturnsDescendingByTimestamp() = runTest {
        dao.upsert(ScanFactory.sample(id = "a", timestampMs = 1L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "b", timestampMs = 3L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "c", timestampMs = 2L).toEntity(json))

        dao.observeAll().test {
            val list = awaitItem()
            assertThat(list.map { it.id }).containsExactly("b", "c", "a").inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun countSinceIncludesOnlyMatchingTimestamps() = runTest {
        dao.upsert(ScanFactory.sample(id = "yesterday", timestampMs = 0L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "today1", timestampMs = 100L).toEntity(json))
        dao.upsert(ScanFactory.sample(id = "today2", timestampMs = 200L).toEntity(json))

        assertThat(dao.countSince(0L)).isEqualTo(3)
        assertThat(dao.countSince(100L)).isEqualTo(2)
        assertThat(dao.countSince(201L)).isEqualTo(0)
    }

    @Test
    fun observeLatestEmitsNullWhenTableIsEmpty() = runTest {
        dao.observeLatest().test {
            assertThat(awaitItem()).isNull()
            cancelAndIgnoreRemainingEvents()
        }
    }
}
