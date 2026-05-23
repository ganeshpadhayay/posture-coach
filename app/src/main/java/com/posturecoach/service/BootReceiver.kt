package com.posturecoach.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.posturecoach.work.WorkScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var workScheduler: WorkScheduler
    @Inject lateinit var activityRecognitionManager: ActivityRecognitionManager

    override fun onReceive(context: Context, intent: Intent) {
        workScheduler.scheduleAll()
        activityRecognitionManager.start()
    }
}
