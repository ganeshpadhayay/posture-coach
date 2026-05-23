package com.posturecoach.testing

import com.posturecoach.data.firebase.FirestoreSync
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.domain.model.NudgeFrequency
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    onboardingComplete: Boolean = false,
    notificationsEnabled: Boolean = true,
    frequency: NudgeFrequency = NudgeFrequency.MEDIUM,
    sittingThresholdMin: Int = DEFAULT_THRESHOLD_MIN,
) : SettingsRepository(
    dataStore = mockk(relaxed = true),
    firestore = mockk<FirestoreSync>(relaxed = true),
) {
    private val _onboardingComplete = MutableStateFlow(onboardingComplete)
    override val onboardingComplete: Flow<Boolean> = _onboardingComplete.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(notificationsEnabled)
    override val notificationsEnabled: Flow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _frequency = MutableStateFlow(frequency)
    override val frequency: Flow<NudgeFrequency> = _frequency.asStateFlow()

    private val _threshold = MutableStateFlow(sittingThresholdMin)
    override val sittingThresholdMin: Flow<Int> = _threshold.asStateFlow()

    var lastSittingNotifMs: Long = 0L
    var lastScanReminderMs: Long = 0L

    override suspend fun setOnboardingComplete(value: Boolean) {
        _onboardingComplete.value = value
    }

    override suspend fun setNotificationsEnabled(value: Boolean) {
        _notificationsEnabled.value = value
    }

    override suspend fun setFrequency(value: NudgeFrequency) {
        _frequency.value = value
    }

    override suspend fun setSittingThresholdMin(value: Int) {
        _threshold.value = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
    }

    override suspend fun lastSittingNotifMs(): Long = lastSittingNotifMs

    override suspend fun setLastSittingNotifMs(value: Long) {
        lastSittingNotifMs = value
    }

    override suspend fun lastScanReminderMs(): Long = lastScanReminderMs

    override suspend fun setLastScanReminderMs(value: Long) {
        lastScanReminderMs = value
    }
}
