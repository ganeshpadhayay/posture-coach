package com.posturecoach.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.ActivityType
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ActivityLogDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ActivityLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.activityLogDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndGetOpenReturnsLatestOpenLog() = runTest {
        dao.insert(ActivityLogEntity(0, 100, 200, ActivityType.STILL))
        dao.insert(ActivityLogEntity(0, 300, null, ActivityType.MOVING))

        val open = dao.getOpen()

        assertThat(open).isNotNull()
        assertThat(open?.type).isEqualTo(ActivityType.MOVING)
        assertThat(open?.startTs).isEqualTo(300)
    }

    @Test
    fun getOpenReturnsNullWhenAllLogsClosed() = runTest {
        dao.insert(ActivityLogEntity(0, 0, 50, ActivityType.STILL))
        dao.insert(ActivityLogEntity(0, 60, 100, ActivityType.MOVING))

        assertThat(dao.getOpen()).isNull()
    }

    @Test
    fun observeSinceFiltersAndOrdersAscending() = runTest {
        dao.insert(ActivityLogEntity(0, 50, 100, ActivityType.STILL))
        dao.insert(ActivityLogEntity(0, 200, null, ActivityType.MOVING))
        dao.insert(ActivityLogEntity(0, 150, 175, ActivityType.STILL))

        dao.observeSince(100).test {
            val items = awaitItem()
            assertThat(items.map { it.startTs }).containsExactly(150L, 200L).inOrder()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun pruneRemovesEntriesStartingBeforeCutoff() = runTest {
        dao.insert(ActivityLogEntity(0, 50, 100, ActivityType.STILL))
        dao.insert(ActivityLogEntity(0, 200, 250, ActivityType.MOVING))
        dao.insert(ActivityLogEntity(0, 300, null, ActivityType.STILL))

        dao.prune(200)

        val remaining = dao.getSince(0)
        assertThat(remaining.map { it.startTs }).containsExactly(200L, 300L).inOrder()
    }

    @Test
    fun updateChangesExistingRow() = runTest {
        val id = dao.insert(ActivityLogEntity(0, 100, null, ActivityType.STILL))
        val updated = ActivityLogEntity(id, 100, 500, ActivityType.STILL)

        dao.update(updated)

        assertThat(dao.getOpen()).isNull()
        assertThat(dao.getSince(0).first().endTs).isEqualTo(500)
    }
}
