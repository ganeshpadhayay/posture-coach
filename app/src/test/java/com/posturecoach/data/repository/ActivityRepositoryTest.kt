package com.posturecoach.data.repository

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.db.ActivityLogDao
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.firebase.FirestoreSync
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coJustRun
import io.mockk.mockk
import io.mockk.slot
import java.util.Calendar
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ActivityRepositoryTest {

    private val firestore = mockk<FirestoreSync>(relaxed = true)

    @Test
    fun `currentStillDurationMs returns delta when open log is STILL`() = runTest {
        val now = 1_000_000L
        val start = now - 10 * 60_000L
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { getOpen() } returns ActivityLogEntity(1, start, null, ActivityType.STILL)
        }
        val repo = ActivityRepository(dao, firestore)
        val ms = repo.currentStillDurationMs(now)
        assertThat(ms).isEqualTo(10 * 60_000L)
    }

    @Test
    fun `currentStillDurationMs returns zero when open log is MOVING`() = runTest {
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { getOpen() } returns ActivityLogEntity(1, 0, null, ActivityType.MOVING)
        }
        val repo = ActivityRepository(dao, firestore)
        assertThat(repo.currentStillDurationMs(1_000)).isEqualTo(0L)
    }

    @Test
    fun `stillDurationTodayMs sums completed and open STILL logs`() = runTest {
        val now = 60 * 60_000L
        val logs = listOf(
            ActivityLogEntity(1, 0, 20 * 60_000L, ActivityType.STILL),
            ActivityLogEntity(2, 20 * 60_000L, 30 * 60_000L, ActivityType.MOVING),
            ActivityLogEntity(3, 30 * 60_000L, null, ActivityType.STILL),
        )
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { getSince(any()) } returns logs
        }
        val repo = ActivityRepository(dao, firestore)
        val total = repo.stillDurationTodayMs(now)
        assertThat(total).isEqualTo(20 * 60_000L + 30 * 60_000L)
    }

    @Test
    fun `beginTransition closes open log of different type then inserts new`() = runTest {
        val openLog = ActivityLogEntity(1, 0, null, ActivityType.MOVING)
        val captured = slot<ActivityLogEntity>()
        val dao = mockk<ActivityLogDao> {
            coEvery { getOpen() } returns openLog
            coJustRun { update(any()) }
            coEvery { insert(capture(captured)) } returns 2L
        }
        val repo = ActivityRepository(dao, firestore)

        repo.beginTransition(ActivityType.STILL, atMs = 100)

        coVerify { dao.update(match { it.id == 1L && it.endTs == 100L }) }
        coVerify { dao.insert(any()) }
        assertThat(captured.captured.type).isEqualTo(ActivityType.STILL)
        assertThat(captured.captured.startTs).isEqualTo(100L)
    }

    @Test
    fun `beginTransition is idempotent when same type`() = runTest {
        val openLog = ActivityLogEntity(1, 0, null, ActivityType.STILL)
        val dao = mockk<ActivityLogDao> {
            coEvery { getOpen() } returns openLog
            coJustRun { update(any()) }
            coEvery { insert(any()) } returns 2L
        }
        val repo = ActivityRepository(dao, firestore)
        repo.beginTransition(ActivityType.STILL)
        coVerify(exactly = 0) { dao.insert(any()) }
        coVerify(exactly = 0) { dao.update(any()) }
    }

    @Test
    fun `observeToday queries DAO with start-of-today timestamp`() = runTest {
        val sinceSlot = slot<Long>()
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { observeSince(capture(sinceSlot)) } returns flowOf(emptyList())
        }
        val repo = ActivityRepository(dao, firestore)

        repo.observeToday().test {
            assertThat(awaitItem()).isEmpty()
            awaitComplete()
        }

        val cal = Calendar.getInstance().apply { timeInMillis = sinceSlot.captured }
        assertThat(cal.get(Calendar.HOUR_OF_DAY)).isEqualTo(0)
        assertThat(cal.get(Calendar.MINUTE)).isEqualTo(0)
        assertThat(cal.get(Calendar.SECOND)).isEqualTo(0)
        assertThat(sinceSlot.captured).isAtMost(System.currentTimeMillis())
    }

    @Test
    fun `observeStillMillisToday re-emits when DAO emits new logs`() = runTest {
        val source = MutableStateFlow<List<ActivityLogEntity>>(emptyList())
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { observeSince(any()) } returns source
        }
        val repo = ActivityRepository(dao, firestore)

        repo.observeStillMillisToday().test {
            assertThat(awaitItem()).isEqualTo(0L)

            val now = System.currentTimeMillis()
            // Closed STILL log of exactly 5 minutes.
            source.value = listOf(
                ActivityLogEntity(1, now - 10 * 60_000L, now - 5 * 60_000L, ActivityType.STILL),
            )
            assertThat(awaitItem()).isEqualTo(5 * 60_000L)
        }
    }

    @Test
    fun `pruneOlderThan computes cutoff days back from now`() = runTest {
        val cutoffSlot = slot<Long>()
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coJustRun { prune(capture(cutoffSlot)) }
        }
        val repo = ActivityRepository(dao, firestore)
        val before = System.currentTimeMillis()

        repo.pruneOlderThan(days = 7)

        val after = System.currentTimeMillis()
        val sevenDays = 7L * 24 * 60 * 60 * 1000
        assertThat(cutoffSlot.captured).isAtLeast(before - sevenDays)
        assertThat(cutoffSlot.captured).isAtMost(after - sevenDays)
    }

    @Test
    fun `stillDurationTodayMs treats open STILL log as still-running until now`() = runTest {
        val now = 60 * 60_000L
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { getSince(any()) } returns listOf(
                ActivityLogEntity(1, now - 30 * 60_000L, null, ActivityType.STILL),
            )
        }
        val repo = ActivityRepository(dao, firestore)
        assertThat(repo.stillDurationTodayMs(now)).isEqualTo(30 * 60_000L)
    }

    @Test
    fun `stillDurationTodayMs returns zero when no STILL logs since midnight`() = runTest {
        val dao = mockk<ActivityLogDao>(relaxed = true) {
            coEvery { getSince(any()) } returns listOf(
                ActivityLogEntity(1, 0, 1_000L, ActivityType.MOVING),
            )
        }
        val repo = ActivityRepository(dao, firestore)
        assertThat(repo.stillDurationTodayMs(60_000L)).isEqualTo(0L)
    }
}
