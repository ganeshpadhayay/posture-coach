package com.posturecoach

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.posturecoach.notification.NotificationHelper
import com.posturecoach.work.WorkScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class PostureCoachApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var workScheduler: WorkScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        signInAnonymouslyIfNeeded()
        notificationHelper.ensureChannels()
        workScheduler.scheduleAll()
    }

    private fun signInAnonymouslyIfNeeded() {
        val auth = runCatching { FirebaseAuth.getInstance() }.getOrNull() ?: return
        if (auth.currentUser == null) {
            auth.signInAnonymously()
        }
    }
}
