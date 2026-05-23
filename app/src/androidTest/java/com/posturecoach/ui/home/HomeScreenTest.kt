package com.posturecoach.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.posturecoach.HiltComponentActivity
import com.posturecoach.R
import com.posturecoach.testing.FakeActivityRepository
import com.posturecoach.testing.FakeScanRepository
import com.posturecoach.testing.ScanFactory
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class HomeScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun ctaIsDisplayedAndClickInvokesCallback() {
        var checked = 0
        val vm = HomeViewModel(FakeScanRepository(), FakeActivityRepository())
        composeRule.setContent {
            HomeScreen(
                onCheckPosture = { checked++ },
                onSettings = {},
                onOpenScan = {},
                viewModel = vm,
            )
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.check_posture))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        assertThat(checked).isEqualTo(1)
    }

    @Test
    fun emptyStateShownWhenNoScans() {
        val vm = HomeViewModel(FakeScanRepository(), FakeActivityRepository())
        composeRule.setContent {
            HomeScreen(onCheckPosture = {}, onSettings = {}, onOpenScan = {}, viewModel = vm)
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.no_scans_yet))
            .assertIsDisplayed()
    }

    @Test
    fun warnStateShownWhenStillExceedsThreshold() {
        val activity = FakeActivityRepository().apply { currentStillMs = 46L * 60_000 }
        val vm = HomeViewModel(FakeScanRepository(), activity)
        composeRule.setContent {
            HomeScreen(onCheckPosture = {}, onSettings = {}, onOpenScan = {}, viewModel = vm)
        }
        // Wait for VM init poll to update state.
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            vm.currentStillMs.value >= 45L * 60_000
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.time_to_move))
            .assertIsDisplayed()
    }

    @Test
    fun settingsActionClickInvokesCallback() {
        var settings = 0
        val vm = HomeViewModel(FakeScanRepository(), FakeActivityRepository())
        composeRule.setContent {
            HomeScreen(
                onCheckPosture = {},
                onSettings = { settings++ },
                onOpenScan = {},
                viewModel = vm,
            )
        }
        composeRule.onNodeWithContentDescriptionMatch(composeRule.activity.getString(R.string.settings))
            .performClick()
        composeRule.waitForIdle()
        assertThat(settings).isEqualTo(1)
    }

    @Test
    fun latestScanCardClickInvokesOpenWithId() {
        var opened: String? = null
        val scan = ScanFactory.sample(id = "scan-xyz", issues = emptyList())
        val scanRepo = FakeScanRepository().apply { seed(scan) }
        val vm = HomeViewModel(scanRepo, FakeActivityRepository())
        composeRule.setContent {
            HomeScreen(
                onCheckPosture = {},
                onSettings = {},
                onOpenScan = { opened = it },
                viewModel = vm,
            )
        }
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) { vm.latestScan.value != null }

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.looks_good))
            .assertIsDisplayed()
            .performClick()
        composeRule.waitForIdle()
        assertThat(opened).isEqualTo("scan-xyz")
    }
}

private fun AndroidComposeTestRule<*, *>.onNodeWithContentDescriptionMatch(
    text: String,
) = onNode(hasContentDescription(text))
