package com.posturecoach.ui.results

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import com.posturecoach.HiltComponentActivity
import com.posturecoach.R
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.ScanFactory
import com.posturecoach.ui.nav.Routes
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ResultsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun viewModelFor(scanId: String, repo: FakeScanRepository): ResultsViewModel =
        ResultsViewModel(SavedStateHandle(mapOf(Routes.ARG_SCAN_ID to scanId)), repo)

    @Test
    fun emptyIssuesHidesViewExercisesShowsRetake() {
        val scan = ScanFactory.sample(id = "clean", issues = emptyList())
        val repo = FakeScanRepository().apply { seed(scan) }
        composeRule.setContent {
            ResultsScreen(
                onRetake = {},
                onViewExercises = {},
                onBack = {},
                viewModel = viewModelFor("clean", repo),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retake))
            .assertIsDisplayed()
        composeRule.onAllNodesWithText(composeRule.activity.getString(R.string.view_exercises))
            .assertCountEquals(0)
    }

    @Test
    fun flaggedIssuesShowViewExercisesAndCallbackPassesScanId() {
        val scan = ScanFactory.sample(id = "flagged", issues = listOf(PostureIssue.FORWARD_HEAD))
        val repo = FakeScanRepository().apply { seed(scan) }
        var openedId: String? = null
        composeRule.setContent {
            ResultsScreen(
                onRetake = {},
                onViewExercises = { openedId = it },
                onBack = {},
                viewModel = viewModelFor("flagged", repo),
            )
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.view_exercises))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        assertThat(openedId).isEqualTo("flagged")
    }

    @Test
    fun retakeButtonClickInvokesCallback() {
        val scan = ScanFactory.sample(id = "x", issues = emptyList())
        val repo = FakeScanRepository().apply { seed(scan) }
        var retakeCount = 0
        composeRule.setContent {
            ResultsScreen(
                onRetake = { retakeCount++ },
                onViewExercises = {},
                onBack = {},
                viewModel = viewModelFor("x", repo),
            )
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.retake)).performClick()
        composeRule.waitForIdle()
        assertThat(retakeCount).isEqualTo(1)
    }
}
