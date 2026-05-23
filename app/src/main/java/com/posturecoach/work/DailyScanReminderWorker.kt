package com.posturecoach.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.posturecoach.data.repository.ScanRepository
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.domain.model.NudgeFrequency
import com.posturecoach.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class DailyScanReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val scanRepository: ScanRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.notificationsEnabled.first()
        if (!enabled) return Result.success()
        val freq = settingsRepository.frequency.first()
        if (freq == NudgeFrequency.LOW) return Result.success()

        val scansToday = scanRepository.countToday()
        if (scansToday == 0) {
            val now = System.currentTimeMillis()
            val last = settingsRepository.lastScanReminderMs()
            if (now - last >= COOLDOWN_MS) {
                notificationHelper.showScanReminder()
                settingsRepository.setLastScanReminderMs(now)
            }
        }
        return Result.success()
    }

    companion object {
        const val NAME = "daily_scan_reminder"
        private const val COOLDOWN_MS = 6L * 60 * 60 * 1000
    }
}
