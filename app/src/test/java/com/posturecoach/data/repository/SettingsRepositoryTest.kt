package com.posturecoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.domain.model.NudgeFrequency
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SettingsRepositoryTest {

    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefsFile: File
    private val firestore = mockk<FirestoreSync>(relaxed = true)
    private lateinit var repo: SettingsRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        prefsFile = File(context.cacheDir, "settings_test.preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        repo = SettingsRepository(dataStore, firestore)
    }

    @After
    fun cleanup() {
        prefsFile.delete()
    }

    @Test
    fun `defaults are returned when nothing has been persisted yet`() = runTest {
        assertThat(repo.onboardingComplete.first()).isFalse()
        assertThat(repo.notificationsEnabled.first()).isTrue()
        assertThat(repo.frequency.first()).isEqualTo(NudgeFrequency.MEDIUM)
        assertThat(repo.sittingThresholdMin.first()).isEqualTo(SettingsRepository.DEFAULT_THRESHOLD_MIN)
        assertThat(repo.lastSittingNotifMs()).isEqualTo(0L)
        assertThat(repo.lastScanReminderMs()).isEqualTo(0L)
    }

    @Test
    fun `setOnboardingComplete persists and does not touch firestore`() = runTest {
        repo.setOnboardingComplete(true)
        assertThat(repo.onboardingComplete.first()).isTrue()
        coVerify(exactly = 0) { firestore.upsertSettings(any(), any(), any()) }
    }

    @Test
    fun `setNotificationsEnabled persists and pushes to firestore`() = runTest {
        repo.setNotificationsEnabled(false)
        assertThat(repo.notificationsEnabled.first()).isFalse()
        coVerify(exactly = 1) { firestore.upsertSettings(notificationsEnabled = false, any(), any()) }
    }

    @Test
    fun `setFrequency persists and pushes to firestore`() = runTest {
        repo.setFrequency(NudgeFrequency.HIGH)
        assertThat(repo.frequency.first()).isEqualTo(NudgeFrequency.HIGH)
        coVerify(exactly = 1) { firestore.upsertSettings(any(), frequency = "HIGH", any()) }
    }

    @Test
    fun `setSittingThresholdMin clamps below MIN`() = runTest {
        repo.setSittingThresholdMin(SettingsRepository.MIN_THRESHOLD - 5)
        assertThat(repo.sittingThresholdMin.first()).isEqualTo(SettingsRepository.MIN_THRESHOLD)
    }

    @Test
    fun `setSittingThresholdMin clamps above MAX`() = runTest {
        repo.setSittingThresholdMin(SettingsRepository.MAX_THRESHOLD + 99)
        assertThat(repo.sittingThresholdMin.first()).isEqualTo(SettingsRepository.MAX_THRESHOLD)
    }

    @Test
    fun `setSittingThresholdMin accepts value within range`() = runTest {
        repo.setSittingThresholdMin(60)
        assertThat(repo.sittingThresholdMin.first()).isEqualTo(60)
    }

    @Test
    fun `last sitting notif timestamp round trips`() = runTest {
        repo.setLastSittingNotifMs(123456789L)
        assertThat(repo.lastSittingNotifMs()).isEqualTo(123456789L)
        coVerify(exactly = 0) { firestore.upsertSettings(any(), any(), any()) }
    }

    @Test
    fun `last scan reminder timestamp round trips`() = runTest {
        repo.setLastScanReminderMs(987654321L)
        assertThat(repo.lastScanReminderMs()).isEqualTo(987654321L)
    }
}
