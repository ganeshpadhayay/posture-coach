package com.posturecoach.data.firebase

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * FCM service is wired so the app can later push server-side reminders
 * (e.g. weekly check-ins, A/B-tested copy). MVP behavior is log-only.
 */
class AppFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        Log.d(TAG, "FCM received: ${message.data}")
    }

    override fun onNewToken(token: String) {
        Log.d(TAG, "FCM token: $token")
    }

    companion object { private const val TAG = "PostureFCM" }
}
