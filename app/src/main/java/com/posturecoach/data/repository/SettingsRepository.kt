package com.posturecoach.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.domain.model.NudgeFrequency
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

@Singleton
open class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val firestore: FirestoreSync,
) {

    private object Keys {
        val onboardingComplete = booleanPreferencesKey("onboarding_complete")
        val notificationsEnabled = booleanPreferencesKey("notifications_enabled")
        val frequency = stringPreferencesKey("nudge_frequency")
        val sittingThresholdMin = intPreferencesKey("sitting_threshold_min")
        val lastSittingNotifMs = longPreferencesKey("last_sitting_notif_ms")
        val lastScanReminderMs = longPreferencesKey("last_scan_reminder_ms")
    }

    open val onboardingComplete: Flow<Boolean> = dataStore.data.map {
        it[Keys.onboardingComplete] ?: false
    }

    open val notificationsEnabled: Flow<Boolean> = dataStore.data.map {
        it[Keys.notificationsEnabled] ?: true
    }

    open val frequency: Flow<NudgeFrequency> = dataStore.data.map {
        NudgeFrequency.fromName(it[Keys.frequency])
    }

    open val sittingThresholdMin: Flow<Int> = dataStore.data.map {
        it[Keys.sittingThresholdMin] ?: DEFAULT_THRESHOLD_MIN
    }

    open suspend fun setOnboardingComplete(value: Boolean) {
        dataStore.edit { it[Keys.onboardingComplete] = value }
    }

    open suspend fun setNotificationsEnabled(value: Boolean) {
        dataStore.edit { it[Keys.notificationsEnabled] = value }
        pushToFirestore()
    }

    open suspend fun setFrequency(value: NudgeFrequency) {
        dataStore.edit { it[Keys.frequency] = value.name }
        pushToFirestore()
    }

    open suspend fun setSittingThresholdMin(value: Int) {
        dataStore.edit { it[Keys.sittingThresholdMin] = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD) }
        pushToFirestore()
    }

    open suspend fun lastSittingNotifMs(): Long =
        dataStore.data.map { it[Keys.lastSittingNotifMs] ?: 0L }.first()

    open suspend fun setLastSittingNotifMs(value: Long) {
        dataStore.edit { it[Keys.lastSittingNotifMs] = value }
    }

    open suspend fun lastScanReminderMs(): Long =
        dataStore.data.map { it[Keys.lastScanReminderMs] ?: 0L }.first()

    open suspend fun setLastScanReminderMs(value: Long) {
        dataStore.edit { it[Keys.lastScanReminderMs] = value }
    }

    private suspend fun pushToFirestore() {
        firestore.upsertSettings(
            notificationsEnabled = notificationsEnabled.first(),
            frequency = frequency.first().name,
            thresholdMin = sittingThresholdMin.first(),
        )
    }

    companion object {
        const val DEFAULT_THRESHOLD_MIN = 45
        const val MIN_THRESHOLD = 15
        const val MAX_THRESHOLD = 120
    }
}
