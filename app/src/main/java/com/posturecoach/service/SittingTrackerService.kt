package com.posturecoach.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.posturecoach.R
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.repository.ActivityRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground accelerometer-based sitting tracker. Only used as a fallback when the
 * Activity Recognition Transition API is not available. Cheap rule: classify every
 * 60-second window by stddev of |accel - g|. Stddev < threshold -> STILL.
 */
@AndroidEntryPoint
class SittingTrackerService : Service(), SensorEventListener {

    @Inject lateinit var activityRepository: ActivityRepository

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private val samples = ArrayDeque<Float>()
    private var windowStartMs: Long = 0
    private var currentType: String? = null
    private var classifyJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
        windowStartMs = System.currentTimeMillis()
        return START_STICKY
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]
        val magnitude = sqrt(ax * ax + ay * ay + az * az)
        samples += abs(magnitude - SensorManager.GRAVITY_EARTH)
        if (samples.size > SAMPLE_CAP) samples.removeFirst()
        if (System.currentTimeMillis() - windowStartMs >= WINDOW_MS) {
            classify()
            windowStartMs = System.currentTimeMillis()
        }
    }

    private fun classify() {
        val snapshot = samples.toList()
        if (snapshot.isEmpty()) return
        val mean = snapshot.average().toFloat()
        val variance = snapshot.fold(0f) { acc, v -> acc + (v - mean) * (v - mean) } / snapshot.size
        val stddev = sqrt(variance)
        val classified = if (stddev < STILL_STDDEV_THRESHOLD) ActivityType.STILL else ActivityType.MOVING
        if (classified != currentType) {
            currentType = classified
            classifyJob?.cancel()
            classifyJob = scope.launch { activityRepository.beginTransition(classified) }
        }
        samples.clear()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        ensureChannel()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.sitting_today))
            .setOngoing(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build().also { _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                }
            }
    }

    private fun ensureChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.app_name),
            NotificationManager.IMPORTANCE_MIN,
        )
        manager.createNotificationChannel(channel)
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "posture_tracking"
        private const val WINDOW_MS = 60_000L
        private const val SAMPLE_CAP = 2000
        private const val STILL_STDDEV_THRESHOLD = 0.35f
    }
}
