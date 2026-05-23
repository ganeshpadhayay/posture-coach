package com.posturecoach.work

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.posturecoach.notification.NotificationHelper
import com.posturecoach.testing.FakeActivityRepository
import com.posturecoach.testing.FakeSettingsRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SittingCheckWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `worker returns success and skips notification when notifications disabled`() = runTest {
        val settings = FakeSettingsRepository(notificationsEnabled = false)
        val activity = FakeActivityRepository().apply { currentStillMs = Long.MAX_VALUE }
        val notif = mockk<NotificationHelper>(relaxed = true)

        val result = buildWorker(activity, settings, notif).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 0) { notif.showSittingNudge(any()) }
    }

    @Test
    fun `worker does not notify when below threshold`() = runTest {
        val settings = FakeSettingsRepository(sittingThresholdMin = 45)
        val activity = FakeActivityRepository().apply {
            currentStillMs = 30L * 60 * 1000 // 30 minutes, below 45
        }
        val notif = mockk<NotificationHelper>(relaxed = true)

        buildWorker(activity, settings, notif).doWork()

        verify(exactly = 0) { notif.showSittingNudge(any()) }
    }

    @Test
    fun `worker respects cooldown and skips notification`() = runTest {
        val settings = FakeSettingsRepository(sittingThresholdMin = 45).apply {
            lastSittingNotifMs = System.currentTimeMillis() // just now
        }
        val activity = FakeActivityRepository().apply {
            currentStillMs = 60L * 60 * 1000 // 60 minutes, above threshold
        }
        val notif = mockk<NotificationHelper>(relaxed = true)

        buildWorker(activity, settings, notif).doWork()

        verify(exactly = 0) { notif.showSittingNudge(any()) }
    }

    @Test
    fun `worker posts notification when above threshold and cooldown elapsed`() = runTest {
        val settings = FakeSettingsRepository(sittingThresholdMin = 45).apply {
            lastSittingNotifMs = 0L // far in the past
        }
        val activity = FakeActivityRepository().apply {
            currentStillMs = 60L * 60 * 1000
        }
        val notif = mockk<NotificationHelper>(relaxed = true)
        justRun { notif.showSittingNudge(any()) }

        val before = System.currentTimeMillis()
        val result = buildWorker(activity, settings, notif).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 1) { notif.showSittingNudge(60) }
        assertThat(settings.lastSittingNotifMs).isAtLeast(before)
    }

    private fun buildWorker(
        activity: FakeActivityRepository,
        settings: FakeSettingsRepository,
        notif: NotificationHelper,
    ): SittingCheckWorker = TestListenableWorkerBuilder<SittingCheckWorker>(context)
        .setWorkerFactory(SittingCheckWorkerFactory(activity, settings, notif))
        .build()
}

private class SittingCheckWorkerFactory(
    private val activity: FakeActivityRepository,
    private val settings: FakeSettingsRepository,
    private val notif: NotificationHelper,
) : androidx.work.WorkerFactory() {
    override fun createWorker(
        appContext: android.content.Context,
        workerClassName: String,
        workerParameters: androidx.work.WorkerParameters,
    ): androidx.work.ListenableWorker = SittingCheckWorker(
        context = appContext,
        params = workerParameters,
        activityRepository = activity,
        settingsRepository = settings,
        notificationHelper = notif,
    )
}
