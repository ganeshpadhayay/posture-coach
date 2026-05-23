package com.posturecoach.ui.scan

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.posturecoach.HiltComponentActivity
import com.posturecoach.R
import com.posturecoach.domain.pose.AnalyzedPose
import com.posturecoach.domain.pose.PoseAnalyzer
import com.posturecoach.domain.usecase.AnalyzePostureUseCase
import com.posturecoach.testing.FakeScanRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ScanScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    /**
     * In an instrumentation test the CAMERA permission is not auto-granted by default,
     * so the permission rationale branch should render. We do NOT exercise the live
     * camera; only the fallback state is asserted.
     */
    @Test
    fun permissionNotGrantedShowsRationale() {
        val analyzer = object : PoseAnalyzer {
            override suspend fun analyzeFile(path: String): AnalyzedPose? = null
            override fun close() = Unit
        }
        val useCase = AnalyzePostureUseCase(analyzer, FakeScanRepository())
        val vm = ScanViewModel(useCase)

        composeRule.setContent {
            ScanScreen(onCaptured = {}, onCancel = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.grant_camera))
            .assertIsDisplayed()
    }
}
