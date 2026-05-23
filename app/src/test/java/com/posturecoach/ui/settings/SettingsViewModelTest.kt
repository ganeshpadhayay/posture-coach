package com.posturecoach.ui.settings

import com.google.common.truth.Truth.assertThat
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.domain.model.NudgeFrequency
import com.posturecoach.testing.FakeSettingsRepository
import com.posturecoach.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `flows expose repository values eagerly`() = runTest {
        val repo = FakeSettingsRepository(
            notificationsEnabled = false,
            frequency = NudgeFrequency.HIGH,
            sittingThresholdMin = 60,
        )
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        assertThat(vm.notificationsEnabled.value).isFalse()
        assertThat(vm.frequency.value).isEqualTo(NudgeFrequency.HIGH)
        assertThat(vm.thresholdMin.value).isEqualTo(60)
    }

    @Test
    fun `setNotificationsEnabled writes through to repository`() = runTest {
        val repo = FakeSettingsRepository(notificationsEnabled = true)
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setNotificationsEnabled(false)
        advanceUntilIdle()

        assertThat(vm.notificationsEnabled.value).isFalse()
    }

    @Test
    fun `setFrequency writes through to repository`() = runTest {
        val repo = FakeSettingsRepository(frequency = NudgeFrequency.MEDIUM)
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setFrequency(NudgeFrequency.LOW)
        advanceUntilIdle()

        assertThat(vm.frequency.value).isEqualTo(NudgeFrequency.LOW)
    }

    @Test
    fun `setThresholdMin clamps below MIN to MIN`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setThresholdMin(SettingsRepository.MIN_THRESHOLD - 5)
        advanceUntilIdle()

        assertThat(vm.thresholdMin.value).isEqualTo(SettingsRepository.MIN_THRESHOLD)
    }

    @Test
    fun `setThresholdMin clamps above MAX to MAX`() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        advanceUntilIdle()

        vm.setThresholdMin(SettingsRepository.MAX_THRESHOLD + 50)
        advanceUntilIdle()

        assertThat(vm.thresholdMin.value).isEqualTo(SettingsRepository.MAX_THRESHOLD)
    }
}
