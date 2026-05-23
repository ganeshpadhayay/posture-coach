package com.posturecoach.ui.exercises

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.testing.FakeExerciseRepository
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.MainDispatcherRule
import com.posturecoach.testing.ScanFactory
import com.posturecoach.ui.nav.Routes
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class ExerciseListViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun `loads exercises matching scan's flagged issues`() = runTest {
        val scan = ScanFactory.sample(
            id = "scan-1",
            issues = listOf(PostureIssue.FORWARD_HEAD),
        )
        val scanRepo = FakeScanRepository().apply { seed(scan) }
        val exerciseRepo = FakeExerciseRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "scan-1"))

        val vm = ExerciseListViewModel(handle, scanRepo, exerciseRepo)
        advanceUntilIdle()

        val ids = vm.exercises.value.map { it.id }
        // FakeExerciseRepository ships 2 forward-head exercises by default.
        assertThat(ids).containsExactly("chin_tucks", "neck_stretch")
    }

    @Test
    fun `unknown scanId loads catalog-wide list since issues default to empty`() = runTest {
        val scanRepo = FakeScanRepository() // no scan seeded
        val exerciseRepo = FakeExerciseRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "missing"))

        val vm = ExerciseListViewModel(handle, scanRepo, exerciseRepo)
        advanceUntilIdle()

        assertThat(vm.exercises.value.map { it.id }).containsExactly(
            "chin_tucks", "neck_stretch", "wall_angels", "cat_cow",
        )
    }

    @Test
    fun `empty issues on scan still returns all exercises`() = runTest {
        val scan = ScanFactory.sample(id = "ok", issues = emptyList())
        val scanRepo = FakeScanRepository().apply { seed(scan) }
        val exerciseRepo = FakeExerciseRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "ok"))

        val vm = ExerciseListViewModel(handle, scanRepo, exerciseRepo)
        advanceUntilIdle()

        assertThat(vm.exercises.value).hasSize(4)
    }
}
