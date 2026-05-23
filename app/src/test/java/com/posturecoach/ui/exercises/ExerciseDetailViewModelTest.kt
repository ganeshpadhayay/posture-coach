package com.posturecoach.ui.exercises

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.posturecoach.testing.FakeExerciseRepository
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.ui.nav.Routes
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExerciseDetailViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads exercise by id from SavedStateHandle`() = runTest {
        val handle = SavedStateHandle(mapOf(Routes.ARG_EXERCISE_ID to "chin_tucks"))
        val vm = ExerciseDetailViewModel(handle, FakeExerciseRepository())

        advanceUntilIdle()

        assertThat(vm.exercise.value?.id).isEqualTo("chin_tucks")
        assertThat(vm.exercise.value?.name).isEqualTo("Chin tucks")
    }

    @Test
    fun `unknown exercise id stays null`() = runTest {
        val handle = SavedStateHandle(mapOf(Routes.ARG_EXERCISE_ID to "nope"))
        val vm = ExerciseDetailViewModel(handle, FakeExerciseRepository())

        advanceUntilIdle()

        assertThat(vm.exercise.value).isNull()
    }
}
