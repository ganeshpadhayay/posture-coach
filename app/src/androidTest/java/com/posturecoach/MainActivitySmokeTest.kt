package com.posturecoach

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import com.posturecoach.data.repository.SettingsRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class MainActivitySmokeTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createEmptyComposeRule()

    @Inject
    lateinit var settings: SettingsRepository

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun activityLaunchesWithoutCrash() {
        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
        }
    }

    @Test
    fun postOnboardingHomeRendersAndSettingsAreReachable() {
        runBlocking { settings.setOnboardingComplete(true) }

        ActivityScenario.launch(MainActivity::class.java).use {
            composeRule.waitForIdle()
            // Wait for the Home title from strings.xml to be displayed.
            composeRule.waitUntil(timeoutMillis = 10_000) {
                composeRule.onAllNodesWithText("Today").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Today").assertIsDisplayed()

            // Tap the settings icon (content description from R.string.settings).
            composeRule.onNode(hasContentDescription("Settings")).performClick()
            composeRule.waitForIdle()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithText("Settings").fetchSemanticsNodes().isNotEmpty()
            }
            composeRule.onNodeWithText("Settings").assertIsDisplayed()
        }
    }
}
