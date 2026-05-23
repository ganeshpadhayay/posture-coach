package com.posturecoach.work

import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.NudgeFrequency
import com.posturecoach.notification.NotificationHelper
import com.posturecoach.testing.FakeScanRepository
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
class DailyScanReminderWorkerTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Test
    fun `LOW frequency short-circuits without notifying`() = runTest {
        val settings = FakeSettingsRepository(frequency = NudgeFrequency.LOW)
        val scanRepo = FakeScanRepository().apply { countTodayResult = 0 }
        val notif = mockk<NotificationHelper>(relaxed = true)

        val result = buildWorker(scanRepo, settings, notif).doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 0) { notif.showScanReminder() }
    }

    @Test
    fun `notifications disabled short-circuits`() = runTest {
        val settings = FakeSettingsRepository(notificationsEnabled = false)
        val scanRepo = FakeScanRepository().apply { countTodayResult = 0 }
        val notif = mockk<NotificationHelper>(relaxed = true)

        buildWorker(scanRepo, settings, notif).doWork()

        verify(exactly = 0) { notif.showScanReminder() }
    }

    @Test
    fun `skips when user already scanned today`() = runTest {
        val settings = FakeSettingsRepository(frequency = NudgeFrequency.MEDIUM)
        val scanRepo = FakeScanRepository().apply { countTodayResult = 1 }
        val notif = mockk<NotificationHelper>(relaxed = true)

        buildWorker(scanRepo, settings, notif).doWork()

        verify(exactly = 0) { notif.showScanReminder() }
    }

    @Test
    fun `respects cooldown window since last reminder`() = runTest {
        val settings = FakeSettingsRepository(frequency = NudgeFrequency.MEDIUM).apply {
            lastScanReminderMs = System.currentTimeMillis()
        }
        val scanRepo = FakeScanRepository().apply { countTodayResult = 0 }
        val notif = mockk<NotificationHelper>(relaxed = true)

        buildWorker(scanRepo, settings, notif).doWork()

        verify(exactly = 0) { notif.showScanReminder() }
    }

    @Test
    fun `posts reminder when all conditions met`() = runTest {
        val settings = FakeSettingsRepository(frequency = NudgeFrequency.HIGH).apply {
            lastScanReminderMs = 0L
        }
        val scanRepo = FakeScanRepository().apply { countTodayResult = 0 }
        val notif = mockk<NotificationHelper>(relaxed = true)
        justRun { notif.showScanReminder() }

        val before = System.currentTimeMillis()
        buildWorker(scanRepo, settings, notif).doWork()

        verify(exactly = 1) { notif.showScanReminder() }
        assertThat(settings.lastScanReminderMs).isAtLeast(before)
    }

    private fun buildWorker(
        scanRepo: FakeScanRepository,
        settings: FakeSettingsRepository,
        notif: NotificationHelper,
    ): DailyScanReminderWorker = TestListenableWorkerBuilder<DailyScanReminderWorker>(context)
        .setWorkerFactory(object : WorkerFactory() {
            override fun createWorker(
                appContext: android.content.Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ) = DailyScanReminderWorker(appContext, workerParameters, scanRepo, settings, notif)
        })
        .build()
}
