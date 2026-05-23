package com.posturecoach.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.posturecoach.data.repository.ActivityRepository
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class SittingCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val activityRepository: ActivityRepository,
    private val settingsRepository: SettingsRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = settingsRepository.notificationsEnabled.first()
        if (!enabled) return Result.success()

        val thresholdMin = settingsRepository.sittingThresholdMin.first()
        val stillMs = activityRepository.currentStillDurationMs()
        val stillMin = (stillMs / 1000 / 60).toInt()

        if (stillMin >= thresholdMin) {
            val now = System.currentTimeMillis()
            val last = settingsRepository.lastSittingNotifMs()
            if (now - last >= COOLDOWN_MS) {
                notificationHelper.showSittingNudge(stillMin)
                settingsRepository.setLastSittingNotifMs(now)
            }
        }
        return Result.success()
    }

    companion object {
        const val NAME = "sitting_check"
        private const val COOLDOWN_MS = 30L * 60 * 1000
    }
}
