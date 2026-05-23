package com.posturecoach.ui.results

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.testing.ScanFactory
import com.posturecoach.ui.nav.Routes
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test

class ResultsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads scan from repository by id from SavedStateHandle`() = runTest {
        val scan = ScanFactory.sample(id = "abc")
        val repo = FakeScanRepository().apply { seed(scan) }
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "abc"))

        val vm = ResultsViewModel(handle, repo)
        advanceUntilIdle()

        assertThat(vm.scanId).isEqualTo("abc")
        assertThat(vm.scan.value?.id).isEqualTo("abc")
    }

    @Test
    fun `scan stays null when id is unknown`() = runTest {
        val repo = FakeScanRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "missing"))

        val vm = ResultsViewModel(handle, repo)
        advanceUntilIdle()

        assertThat(vm.scan.value).isNull()
    }

    @Test
    fun `missing scanId argument throws IllegalStateException`() {
        val repo = FakeScanRepository()
        val handle = SavedStateHandle()
        assertThrows(IllegalStateException::class.java) {
            ResultsViewModel(handle, repo)
        }
    }
}
