package com.posturecoach.ui.scan

import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.usecase.AnalyzePostureUseCase
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.testing.ScanFactory
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ScanViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `analyze emits Analyzing then Success when use case returns Success`() = runTest {
        val scan = ScanFactory.sample(id = "ok")
        val useCase = mockk<AnalyzePostureUseCase>()
        coEvery { useCase("/path") } returns AnalyzePostureUseCase.Result.Success(scan)
        val vm = ScanViewModel(useCase)

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Idle)
        vm.analyze("/path")
        // launch{} is queued; state should already be Analyzing synchronously.
        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Analyzing)
        advanceUntilIdle()

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Success("ok"))
    }

    @Test
    fun `analyze emits Error with NO_POSE on NoPoseDetected`() = runTest {
        val useCase = mockk<AnalyzePostureUseCase>()
        coEvery { useCase(any()) } returns AnalyzePostureUseCase.Result.NoPoseDetected
        val vm = ScanViewModel(useCase)

        vm.analyze("/path")
        advanceUntilIdle()

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Error(ScanViewModel.NO_POSE))
    }

    @Test
    fun `analyze emits Error with message from underlying throwable`() = runTest {
        val useCase = mockk<AnalyzePostureUseCase>()
        coEvery { useCase(any()) } returns AnalyzePostureUseCase.Result.Error(RuntimeException("boom"))
        val vm = ScanViewModel(useCase)

        vm.analyze("/path")
        advanceUntilIdle()

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Error("boom"))
    }

    @Test
    fun `analyze emits generic error when throwable has no message`() = runTest {
        val useCase = mockk<AnalyzePostureUseCase>()
        coEvery { useCase(any()) } returns AnalyzePostureUseCase.Result.Error(RuntimeException())
        val vm = ScanViewModel(useCase)

        vm.analyze("/path")
        advanceUntilIdle()

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Error(ScanViewModel.GENERIC_ERROR))
    }

    @Test
    fun `analyze is ignored when already analyzing`() = runTest {
        val useCase = mockk<AnalyzePostureUseCase>()
        // Hangs forever on the first invocation.
        coEvery { useCase("/first") } coAnswers {
            kotlinx.coroutines.delay(Long.MAX_VALUE)
            AnalyzePostureUseCase.Result.NoPoseDetected
        }
        coEvery { useCase("/second") } returns AnalyzePostureUseCase.Result.NoPoseDetected
        val vm = ScanViewModel(useCase)

        vm.analyze("/first")
        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Analyzing)
        vm.analyze("/second")
        // Still Analyzing because the second call should be ignored.
        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Analyzing)
    }

    @Test
    fun `reset returns to Idle`() = runTest {
        val useCase = mockk<AnalyzePostureUseCase>()
        coEvery { useCase(any()) } returns AnalyzePostureUseCase.Result.NoPoseDetected
        val vm = ScanViewModel(useCase)
        vm.analyze("/path")
        advanceUntilIdle()

        vm.reset()

        assertThat(vm.state.value).isEqualTo(ScanViewModel.State.Idle)
    }
}
