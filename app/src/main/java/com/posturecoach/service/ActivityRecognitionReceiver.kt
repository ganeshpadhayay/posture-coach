package com.posturecoach.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.ActivityTransition
import com.google.android.gms.location.ActivityTransitionResult
import com.google.android.gms.location.DetectedActivity
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.repository.ActivityRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ActivityRecognitionReceiver : BroadcastReceiver() {

    @Inject lateinit var activityRepository: ActivityRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (!ActivityTransitionResult.hasResult(intent)) return
        val result = ActivityTransitionResult.extractResult(intent) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                result.transitionEvents.forEach { event ->
                    if (event.transitionType != ActivityTransition.ACTIVITY_TRANSITION_ENTER) return@forEach
                    val mapped = when (event.activityType) {
                        DetectedActivity.STILL -> ActivityType.STILL
                        DetectedActivity.WALKING,
                        DetectedActivity.RUNNING,
                        DetectedActivity.ON_FOOT,
                        DetectedActivity.ON_BICYCLE -> ActivityType.MOVING
                        else -> null
                    }
                    if (mapped != null) {
                        activityRepository.beginTransition(mapped)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
