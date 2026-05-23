package com.posturecoach.notification

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class NotificationHelperTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val helper = NotificationHelper(context)

    @Test
    fun ensureChannelsCreatesNudgeChannel() {
        helper.ensureChannels()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(NotificationHelper.CHANNEL_NUDGES)
        assertThat(channel).isNotNull()
        assertThat(channel!!.importance).isEqualTo(NotificationManager.IMPORTANCE_DEFAULT)
    }

    @Test
    fun ensureChannelsIsIdempotent() {
        helper.ensureChannels()
        helper.ensureChannels()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // Only the expected channel should exist, not duplicates of it.
        val nudgeChannels = manager.notificationChannels.filter {
            it.id == NotificationHelper.CHANNEL_NUDGES
        }
        assertThat(nudgeChannels).hasSize(1)
    }
}
