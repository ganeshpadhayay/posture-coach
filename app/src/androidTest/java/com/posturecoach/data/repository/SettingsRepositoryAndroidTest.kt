package com.posturecoach.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.domain.model.NudgeFrequency
import io.mockk.mockk
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryAndroidTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val firestore = mockk<FirestoreSync>(relaxed = true)
    private lateinit var prefsFile: File

    @Before
    fun setup() {
        prefsFile = File(context.cacheDir, "settings_android_test.preferences_pb")
        if (prefsFile.exists()) prefsFile.delete()
    }

    @After
    fun cleanup() {
        prefsFile.delete()
    }

    @Test
    fun settingsSurviveDataStoreRecreation() = runTest {
        val firstStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val firstRepo = SettingsRepository(firstStore, firestore)
        firstRepo.setFrequency(NudgeFrequency.HIGH)
        firstRepo.setSittingThresholdMin(60)
        firstRepo.setOnboardingComplete(true)

        // Simulate process death: spin up a new repo on the same file.
        val secondStore = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val secondRepo = SettingsRepository(secondStore, firestore)

        assertThat(secondRepo.frequency.first()).isEqualTo(NudgeFrequency.HIGH)
        assertThat(secondRepo.sittingThresholdMin.first()).isEqualTo(60)
        assertThat(secondRepo.onboardingComplete.first()).isTrue()
    }

    @Test
    fun thresholdClampingPersistsOnDisk() = runTest {
        val store = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val repo = SettingsRepository(store, firestore)

        repo.setSittingThresholdMin(SettingsRepository.MAX_THRESHOLD + 100)

        val store2 = PreferenceDataStoreFactory.create(produceFile = { prefsFile })
        val repo2 = SettingsRepository(store2, firestore)
        assertThat(repo2.sittingThresholdMin.first()).isEqualTo(SettingsRepository.MAX_THRESHOLD)
    }
}
