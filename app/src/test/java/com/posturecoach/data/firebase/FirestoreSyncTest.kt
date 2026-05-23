package com.posturecoach.data.firebase

import com.google.android.gms.tasks.Tasks
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.posturecoach.data.db.entities.ActivityLogEntity
import com.posturecoach.data.db.entities.ActivityType
import com.posturecoach.data.db.entities.PostureScanEntity
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class FirestoreSyncTest {

    private val auth = mockk<FirebaseAuth>(relaxed = true)
    private val firestore = mockk<FirebaseFirestore>(relaxed = true)
    private val sync = FirestoreSync(auth, firestore)

    private fun stubUser(uid: String?) {
        if (uid == null) {
            every { auth.currentUser } returns null
        } else {
            val user = mockk<FirebaseUser>(relaxed = true)
            every { user.uid } returns uid
            every { auth.currentUser } returns user
        }
    }

    @Test
    fun `upsertScan no-ops when no signed-in user`() = runTest {
        stubUser(null)
        sync.upsertScan(samplePostureEntity())
        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun `upsertScan writes to expected path under signed-in user`() = runTest {
        stubUser("u1")
        val usersCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val scansCollection = mockk<CollectionReference>(relaxed = true)
        val scanDoc = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("u1") } returns userDoc
        every { userDoc.collection("scans") } returns scansCollection
        every { scansCollection.document("scan-7") } returns scanDoc
        val payload = slot<Map<String, Any?>>()
        every { scanDoc.set(capture(payload)) } returns Tasks.forResult(null)

        sync.upsertScan(samplePostureEntity(id = "scan-7"))

        verify(exactly = 1) { scanDoc.set(any<Map<String, Any?>>()) }
        assertThat(payload.captured["id"]).isEqualTo("scan-7")
        assertThat(payload.captured["timestampMs"]).isEqualTo(1_700_000_000_000L)
    }

    @Test
    fun `upsertActivity writes under activity collection`() = runTest {
        stubUser("u2")
        val usersCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        val activityCollection = mockk<CollectionReference>(relaxed = true)
        val activityDoc = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("u2") } returns userDoc
        every { userDoc.collection("activity") } returns activityCollection
        every { activityCollection.document("42") } returns activityDoc
        every { activityDoc.set(any()) } returns Tasks.forResult(null)

        sync.upsertActivity(ActivityLogEntity(id = 42, startTs = 100, endTs = 200, type = ActivityType.STILL))

        verify { activityDoc.set(any()) }
    }

    @Test
    fun `upsertSettings writes to user document`() = runTest {
        stubUser("u3")
        val usersCollection = mockk<CollectionReference>(relaxed = true)
        val userDoc = mockk<DocumentReference>(relaxed = true)
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("u3") } returns userDoc
        val payload = slot<Map<String, Any?>>()
        every { userDoc.set(capture(payload)) } returns Tasks.forResult(null)

        sync.upsertSettings(notificationsEnabled = true, frequency = "LOW", thresholdMin = 30)

        assertThat(payload.captured).containsEntry("notificationsEnabled", true)
        assertThat(payload.captured).containsEntry("frequency", "LOW")
        assertThat(payload.captured).containsEntry("sittingThresholdMin", 30)
    }

    @Test
    fun `failures from Firestore are swallowed`() = runTest {
        stubUser("u4")
        every { firestore.collection(any()) } throws RuntimeException("network down")

        // Should not throw.
        sync.upsertScan(samplePostureEntity())
        sync.upsertSettings(true, "MEDIUM", 45)
    }

    private fun samplePostureEntity(id: String = "scan") = PostureScanEntity(
        id = id,
        timestampMs = 1_700_000_000_000L,
        imagePath = "/tmp/x.jpg",
        imageWidth = 100,
        imageHeight = 100,
        issuesCsv = "forward_head",
        anglesJson = "{}",
        landmarksJson = "[]",
    )
}
