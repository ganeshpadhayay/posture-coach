package com.posturecoach.ui.exercises

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.posturecoach.HiltComponentActivity
import com.posturecoach.domain.model.Exercise
import com.posturecoach.testing.FakeExerciseRepository
import com.posturecoach.ui.nav.Routes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ExerciseDetailScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun instructionsRenderInOrderWithNumberedSteps() {
        val custom = mutableListOf<Exercise>(
            Exercise(
                id = "test_ex",
                name = "Test Exercise",
                issue = "forward_head",
                instructions = listOf("Step one.", "Step two.", "Step three."),
                durationSec = 30,
                gifAsset = "exercises/chin_tucks.gif",
            ),
        )
        val repo = FakeExerciseRepository(custom)
        val handle = SavedStateHandle(mapOf(Routes.ARG_EXERCISE_ID to "test_ex"))
        val vm = ExerciseDetailViewModel(handle, repo)

        composeRule.setContent {
            ExerciseDetailScreen(onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { vm.exercise.value != null }

        composeRule.onNodeWithText("Step one.").assertIsDisplayed()
        composeRule.onNodeWithText("Step two.").assertIsDisplayed()
        composeRule.onNodeWithText("Step three.").assertIsDisplayed()
        composeRule.onNodeWithText("1").assertIsDisplayed()
        composeRule.onNodeWithText("2").assertIsDisplayed()
        composeRule.onNodeWithText("3").assertIsDisplayed()
    }

    @Test
    fun titleShowsExerciseName() {
        val handle = SavedStateHandle(mapOf(Routes.ARG_EXERCISE_ID to "chin_tucks"))
        val vm = ExerciseDetailViewModel(handle, FakeExerciseRepository())

        composeRule.setContent {
            ExerciseDetailScreen(onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { vm.exercise.value != null }

        composeRule.onNodeWithText("Chin tucks").assertIsDisplayed()
    }
}
