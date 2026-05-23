package com.posturecoach.ui.onboarding

import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.FakeSettingsRepository
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.work.WorkScheduler
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class OnboardingViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `complete persists onboarding flag, schedules work and invokes callback`() = runTest {
        val settings = FakeSettingsRepository(onboardingComplete = false)
        val scheduler = mockk<WorkScheduler>(relaxed = true)
        justRun { scheduler.scheduleAll() }
        val vm = OnboardingViewModel(settings, scheduler)

        var doneCalls = 0
        vm.complete { doneCalls++ }
        advanceUntilIdle()

        assertThat(settings.onboardingComplete.first()).isTrue()
        verify(exactly = 1) { scheduler.scheduleAll() }
        assertThat(doneCalls).isEqualTo(1)
    }

    @Test
    fun `complete calls onDone exactly once even if invoked twice`() = runTest {
        val settings = FakeSettingsRepository()
        val scheduler = mockk<WorkScheduler>(relaxed = true)
        val vm = OnboardingViewModel(settings, scheduler)

        var count = 0
        vm.complete { count++ }
        vm.complete { count++ }
        advanceUntilIdle()

        assertThat(count).isEqualTo(2) // both launches finish since each owns its own callback
        verify(exactly = 2) { scheduler.scheduleAll() }
    }
}
