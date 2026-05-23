package com.posturecoach.data.firebase

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.PostureScanEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSync @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) {

    private val uid: String? get() = auth.currentUser?.uid

    suspend fun upsertScan(entity: PostureScanEntity) = runSafe {
        val user = uid ?: return@runSafe
        firestore.collection("users").document(user)
            .collection("scans").document(entity.id)
            .set(
                mapOf(
                    "id" to entity.id,
                    "timestampMs" to entity.timestampMs,
                    "imageWidth" to entity.imageWidth,
                    "imageHeight" to entity.imageHeight,
                    "issues" to entity.issuesCsv.split(",").filter { it.isNotBlank() },
                    "anglesJson" to entity.anglesJson,
                ),
            ).await()
    }

    suspend fun upsertActivity(entity: ActivityLogEntity) = runSafe {
        val user = uid ?: return@runSafe
        firestore.collection("users").document(user)
            .collection("activity").document(entity.id.toString())
            .set(
                mapOf(
                    "startTs" to entity.startTs,
                    "endTs" to entity.endTs,
                    "type" to entity.type,
                ),
            ).await()
    }

    suspend fun upsertSettings(notificationsEnabled: Boolean, frequency: String, thresholdMin: Int) = runSafe {
        val user = uid ?: return@runSafe
        firestore.collection("users").document(user).set(
            mapOf(
                "notificationsEnabled" to notificationsEnabled,
                "frequency" to frequency,
                "sittingThresholdMin" to thresholdMin,
            ),
        ).await()
    }

    private inline fun runSafe(block: () -> Unit) {
        try {
            block()
        } catch (t: Throwable) {
            Log.w(TAG, "Firestore sync failed: ${t.message}")
        }
    }

    companion object {
        private const val TAG = "FirestoreSync"
    }
}
