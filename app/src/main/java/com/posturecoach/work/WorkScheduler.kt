package com.posturecoach.work

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.posturecoach.service.ActivityRecognitionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class WorkScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val activityRecognitionManager: ActivityRecognitionManager,
) {

    open fun scheduleAll() {
        scheduleSittingCheck()
        scheduleDailyScanReminder()
        activityRecognitionManager.start()
    }

    internal fun scheduleSittingCheck() {
        val request = PeriodicWorkRequestBuilder<SittingCheckWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            SittingCheckWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    internal fun scheduleDailyScanReminder() {
        val request = PeriodicWorkRequestBuilder<DailyScanReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelayUntilEvening(), TimeUnit.MILLISECONDS)
            .setConstraints(Constraints.NONE)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DailyScanReminderWorker.NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    internal fun initialDelayUntilEvening(nowMs: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMs }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, 18)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now.timeInMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
