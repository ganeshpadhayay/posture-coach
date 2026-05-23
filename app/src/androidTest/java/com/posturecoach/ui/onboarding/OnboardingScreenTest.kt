package com.posturecoach.ui.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.posturecoach.HiltComponentActivity
import com.posturecoach.R
import com.posturecoach.testing.FakeSettingsRepository
import com.posturecoach.work.WorkScheduler
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.justRun
import io.mockk.mockk
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class OnboardingScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun buildViewModel(): OnboardingViewModel {
        val scheduler = mockk<WorkScheduler>(relaxed = true).also { justRun { it.scheduleAll() } }
        return OnboardingViewModel(FakeSettingsRepository(), scheduler)
    }

    @Test
    fun firstPageShowsWelcomeAndNextButton() {
        composeRule.setContent {
            OnboardingScreen(onFinished = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_welcome_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).assertIsDisplayed()
    }

    @Test
    fun nextNavigatesThroughPagesUntilGetStartedAppears() {
        composeRule.setContent {
            OnboardingScreen(onFinished = {}, viewModel = buildViewModel())
        }
        // page 1 → page 2
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.onboarding_how_title))
            .assertIsDisplayed()

        // page 2 → page 3 (permissions). On the last page the button says "Get started".
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.get_started))
            .assertIsDisplayed()
    }

    @Test
    fun permissionRowsRenderOnLastPage() {
        composeRule.setContent {
            OnboardingScreen(onFinished = {}, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.grant_camera))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.grant_activity))
            .assertIsDisplayed()
    }

    @Test
    fun getStartedButtonTriggersOnFinishedCallback() {
        var finished = 0
        composeRule.setContent {
            OnboardingScreen(onFinished = { finished++ }, viewModel = buildViewModel())
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.next)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.get_started)).performClick()
        composeRule.waitForIdle()

        assert(finished == 1) { "Expected onFinished called once, got $finished" }
    }
}
