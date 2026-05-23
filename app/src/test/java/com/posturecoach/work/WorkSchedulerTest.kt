package com.posturecoach.work

import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.posturecoach.service.ActivityRecognitionManager
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import java.util.Calendar
import java.util.concurrent.TimeUnit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class WorkSchedulerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val activityRecognition = mockk<ActivityRecognitionManager>(relaxed = true)

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build(),
        )
        justRun { activityRecognition.start() }
    }

    @Test
    fun `scheduleAll enqueues both periodic workers and starts activity recognition`() {
        val scheduler = WorkScheduler(context, activityRecognition)

        scheduler.scheduleAll()

        val wm = WorkManager.getInstance(context)
        val sitting = wm.getWorkInfosForUniqueWork(SittingCheckWorker.NAME).get()
        val daily = wm.getWorkInfosForUniqueWork(DailyScanReminderWorker.NAME).get()

        assertThat(sitting).isNotEmpty()
        assertThat(sitting[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        assertThat(daily).isNotEmpty()
        assertThat(daily[0].state).isEqualTo(WorkInfo.State.ENQUEUED)
        verify(exactly = 1) { activityRecognition.start() }
    }

    @Test
    fun `scheduleAll uses KEEP policy so second call leaves first request in place`() {
        val scheduler = WorkScheduler(context, activityRecognition)
        scheduler.scheduleAll()
        val first = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SittingCheckWorker.NAME).get().first().id

        scheduler.scheduleAll()
        val second = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork(SittingCheckWorker.NAME).get().first().id

        assertThat(second).isEqualTo(first)
    }

    @Test
    fun `initialDelayUntilEvening at noon equals six hours`() {
        val noon = calendar(hour = 12, minute = 0)
        val delay = WorkScheduler(context, activityRecognition).initialDelayUntilEvening(noon.timeInMillis)

        assertThat(delay).isEqualTo(TimeUnit.HOURS.toMillis(6))
    }

    @Test
    fun `initialDelayUntilEvening at 6pm rolls to tomorrow`() {
        val sixPm = calendar(hour = 18, minute = 0)
        val delay = WorkScheduler(context, activityRecognition).initialDelayUntilEvening(sixPm.timeInMillis)

        assertThat(delay).isEqualTo(TimeUnit.HOURS.toMillis(24))
    }

    @Test
    fun `initialDelayUntilEvening at 7pm rolls to tomorrow 6pm`() {
        val sevenPm = calendar(hour = 19, minute = 0)
        val delay = WorkScheduler(context, activityRecognition).initialDelayUntilEvening(sevenPm.timeInMillis)

        assertThat(delay).isEqualTo(TimeUnit.HOURS.toMillis(23))
    }

    private fun calendar(hour: Int, minute: Int): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.JANUARY, 15, hour, minute, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
