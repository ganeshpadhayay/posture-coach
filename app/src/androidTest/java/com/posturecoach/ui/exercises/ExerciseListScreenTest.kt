package com.posturecoach.ui.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.posturecoach.HiltComponentActivity
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.testing.FakeExerciseRepository
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.ScanFactory
import com.posturecoach.ui.nav.Routes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ExerciseListScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun listShowsExercisesFromViewModel() {
        val scan = ScanFactory.sample(id = "scan", issues = listOf(PostureIssue.FORWARD_HEAD))
        val scanRepo = FakeScanRepository().apply { seed(scan) }
        val exerciseRepo = FakeExerciseRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "scan"))
        val vm = ExerciseListViewModel(handle, scanRepo, exerciseRepo)

        composeRule.setContent {
            ExerciseListScreen(onExerciseClick = {}, onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { vm.exercises.value.isNotEmpty() }

        composeRule.onNodeWithText("Chin tucks").assertIsDisplayed()
        composeRule.onNodeWithText("Neck side stretch").assertIsDisplayed()
    }

    @Test
    fun rowClickInvokesLambdaWithCorrectId() {
        val scan = ScanFactory.sample(id = "scan", issues = listOf(PostureIssue.FORWARD_HEAD))
        val scanRepo = FakeScanRepository().apply { seed(scan) }
        val exerciseRepo = FakeExerciseRepository()
        val handle = SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to "scan"))
        val vm = ExerciseListViewModel(handle, scanRepo, exerciseRepo)

        var clicked: String? = null
        composeRule.setContent {
            ExerciseListScreen(onExerciseClick = { clicked = it }, onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { vm.exercises.value.isNotEmpty() }

        composeRule.onNodeWithText("Chin tucks").performClick()
        composeRule.waitForIdle()
        assertThat(clicked).isEqualTo("chin_tucks")
    }
}
