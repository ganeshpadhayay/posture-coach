package com.posturecoach.work

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.google.common.truth.Truth.assertThat
import com.posturecoach.notification.NotificationHelper
import com.posturecoach.testing.FakeActivityRepository
import com.posturecoach.testing.FakeSettingsRepository
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SittingCheckWorkerAndroidTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Before
    fun setup() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setMinimumLoggingLevel(android.util.Log.DEBUG).build(),
        )
    }

    @Test
    fun aboveThresholdPostsNudgeAndReturnsSuccess() = runTest {
        val settings = FakeSettingsRepository(sittingThresholdMin = 45).apply {
            lastSittingNotifMs = 0L
        }
        val activity = FakeActivityRepository().apply {
            currentStillMs = 90L * 60 * 1000 // 90 minutes
        }
        val notif = mockk<NotificationHelper>(relaxed = true)
        justRun { notif.showSittingNudge(any()) }

        val worker = TestListenableWorkerBuilder<SittingCheckWorker>(context)
            .setWorkerFactory(workerFactory(activity, settings, notif))
            .build()

        val result = worker.doWork()

        assertThat(result).isEqualTo(ListenableWorker.Result.success())
        verify(exactly = 1) { notif.showSittingNudge(90) }
        assertThat(settings.lastSittingNotifMs).isGreaterThan(0L)
    }

    @Test
    fun belowThresholdSkipsNotification() = runTest {
        val settings = FakeSettingsRepository(sittingThresholdMin = 45)
        val activity = FakeActivityRepository().apply {
            currentStillMs = 10L * 60 * 1000
        }
        val notif = mockk<NotificationHelper>(relaxed = true)

        val worker = TestListenableWorkerBuilder<SittingCheckWorker>(context)
            .setWorkerFactory(workerFactory(activity, settings, notif))
            .build()

        worker.doWork()

        verify(exactly = 0) { notif.showSittingNudge(any()) }
    }

    private fun workerFactory(
        activity: FakeActivityRepository,
        settings: FakeSettingsRepository,
        notif: NotificationHelper,
    ): WorkerFactory = object : WorkerFactory() {
        override fun createWorker(
            appContext: Context,
            workerClassName: String,
            workerParameters: WorkerParameters,
        ) = SittingCheckWorker(appContext, workerParameters, activity, settings, notif)
    }
}
