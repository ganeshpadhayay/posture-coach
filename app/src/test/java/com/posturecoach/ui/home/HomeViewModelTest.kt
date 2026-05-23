package com.posturecoach.ui.home

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.FakeActivityRepository
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.testing.ScanFactory
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    private val scanRepo = FakeScanRepository()
    private val activityRepo = FakeActivityRepository()

    @Test
    fun `latestScan reflects what ScanRepository emits`() = runTest {
        val vm = HomeViewModel(scanRepo, activityRepo)

        vm.latestScan.test {
            assertThat(awaitItem()).isNull()
            val scan = ScanFactory.sample(id = "fresh", timestampMs = 999L)
            scanRepo.save(scan)
            assertThat(awaitItem()?.id).isEqualTo("fresh")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `init populates currentStillMs from activity repository`() = runTest {
        activityRepo.currentStillMs = 7 * 60_000L
        activityRepo.stillTodayMs = 42 * 60_000L

        val vm = HomeViewModel(scanRepo, activityRepo)
        advanceTimeBy(1)

        assertThat(vm.currentStillMs.value).isEqualTo(7 * 60_000L)
        assertThat(vm.sittingTodayMs.value).isEqualTo(42 * 60_000L)
    }

    @Test
    fun `currentStillMs re-polls after 30s refresh window`() = runTest {
        activityRepo.currentStillMs = 60_000L
        val vm = HomeViewModel(scanRepo, activityRepo)
        advanceTimeBy(1)
        assertThat(vm.currentStillMs.value).isEqualTo(60_000L)

        activityRepo.currentStillMs = 120_000L
        // Refresh interval in HomeViewModel is 30 seconds.
        advanceTimeBy(31_000L)

        assertThat(vm.currentStillMs.value).isEqualTo(120_000L)
    }
}
