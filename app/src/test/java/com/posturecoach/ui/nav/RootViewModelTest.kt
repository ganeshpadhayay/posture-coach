package com.posturecoach.ui.nav

import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.FakeSettingsRepository
import com.posturecoach.testing.MainDispatcherRule
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class RootViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `initial value before subscription is null`() = runTest {
        val settings = FakeSettingsRepository(onboardingComplete = true)
        val vm = RootViewModel(settings)
        assertThat(vm.onboardingComplete.value).isNull()
    }

    @Test
    fun `onboardingComplete StateFlow emits true when settings says complete`() = runTest {
        val settings = FakeSettingsRepository(onboardingComplete = true)
        val vm = RootViewModel(settings)
        advanceUntilIdle()
        assertThat(vm.onboardingComplete.value).isTrue()
    }

    @Test
    fun `onboardingComplete StateFlow emits false when settings says incomplete`() = runTest {
        val settings = FakeSettingsRepository(onboardingComplete = false)
        val vm = RootViewModel(settings)
        advanceUntilIdle()
        assertThat(vm.onboardingComplete.value).isFalse()
    }
}
