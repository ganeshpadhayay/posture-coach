package com.posturecoach.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.posturecoach.HiltComponentActivity
import com.posturecoach.R
import com.posturecoach.domain.model.NudgeFrequency
import com.posturecoach.testing.FakeSettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun toggleSwitchUpdatesViewModel() {
        val repo = FakeSettingsRepository(notificationsEnabled = true)
        val vm = SettingsViewModel(repo)
        composeRule.setContent {
            SettingsScreen(onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notifications_enabled))
            .assertIsDisplayed()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.notifications_enabled))
            .performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 2_000) { !vm.notificationsEnabled.value }
        assertThat(vm.notificationsEnabled.value).isFalse()
    }

    @Test
    fun chipSelectionUpdatesFrequency() {
        val repo = FakeSettingsRepository(frequency = NudgeFrequency.MEDIUM)
        val vm = SettingsViewModel(repo)
        composeRule.setContent {
            SettingsScreen(onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText(composeRule.activity.getString(R.string.frequency_high))
            .performClick()
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 2_000) { vm.frequency.value == NudgeFrequency.HIGH }
        assertThat(vm.frequency.value).isEqualTo(NudgeFrequency.HIGH)
    }

    @Test
    fun thresholdValueLabelMatchesViewModel() {
        val repo = FakeSettingsRepository(sittingThresholdMin = 60)
        val vm = SettingsViewModel(repo)
        composeRule.setContent {
            SettingsScreen(onBack = {}, viewModel = vm)
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithText(
            composeRule.activity.getString(R.string.minutes_value, 60),
        ).assertIsDisplayed()
    }
}
