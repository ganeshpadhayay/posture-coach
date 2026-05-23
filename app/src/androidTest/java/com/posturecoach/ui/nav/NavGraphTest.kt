package com.posturecoach.ui.nav

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.testing.TestNavHostController
import com.google.common.truth.Truth.assertThat
import com.posturecoach.HiltComponentActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Exercises the navigation graph contract: routes, arguments, and `popUpTo` policy.
 * Uses placeholder composables so we don't depend on production screens here.
 */
@HiltAndroidTest
class NavGraphTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<HiltComponentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
    }

    private fun startGraph(start: String = Routes.HOME) {
        composeRule.setContent {
            navController = TestNavHostController(androidx.compose.ui.platform.LocalContext.current)
            navController.navigatorProvider.addNavigator(ComposeNavigator())
            FakeNavGraph(navController = navController, startDestination = start)
        }
    }

    @Test
    fun homeStartDestinationIsHome() {
        startGraph(start = Routes.HOME)
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(Routes.HOME)
    }

    @Test
    fun onboardingStartDestinationIsOnboarding() {
        startGraph(start = Routes.ONBOARDING)
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(Routes.ONBOARDING)
    }

    @Test
    fun homeToScanToResultsNavigatesCorrectly() {
        startGraph()
        composeRule.runOnIdle { navController.navigate(Routes.SCAN) }
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(Routes.SCAN)

        composeRule.runOnIdle {
            navController.navigate(Routes.results("scan-42")) {
                popUpTo(Routes.SCAN) { inclusive = true }
            }
        }
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(Routes.RESULTS)
        // Argument propagation.
        val args = navController.currentBackStackEntry?.arguments
        assertThat(args?.getString(Routes.ARG_SCAN_ID)).isEqualTo("scan-42")
    }

    @Test
    fun popUpToFromResultsRemovesScanFromBackStack() {
        startGraph()
        composeRule.runOnIdle {
            navController.navigate(Routes.SCAN)
        }
        composeRule.runOnIdle {
            navController.navigate(Routes.results("scan-1")) {
                popUpTo(Routes.SCAN) { inclusive = true }
            }
        }
        composeRule.waitForIdle()

        // Pop the current results destination — we should land on HOME, not SCAN.
        composeRule.runOnIdle { navController.popBackStack() }
        composeRule.waitForIdle()
        assertThat(navController.currentDestination?.route).isEqualTo(Routes.HOME)
    }

    @Test
    fun resultsToExercisesNavigatesWithScanIdArgument() {
        startGraph()
        composeRule.runOnIdle { navController.navigate(Routes.results("s1")) }
        composeRule.runOnIdle { navController.navigate(Routes.exercises("s1")) }
        composeRule.waitForIdle()

        assertThat(navController.currentDestination?.route).isEqualTo(Routes.EXERCISES)
        val args = navController.currentBackStackEntry?.arguments
        assertThat(args?.getString(Routes.ARG_SCAN_ID)).isEqualTo("s1")
    }
}

@Composable
private fun FakeNavGraph(navController: NavHostController, startDestination: String) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) { Text("onboarding") }
        composable(Routes.HOME) { Text("home") }
        composable(Routes.SCAN) { Text("scan") }
        composable(
            route = Routes.RESULTS,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.StringType }),
        ) { Text("results") }
        composable(
            route = Routes.EXERCISES,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.StringType }),
        ) { Text("exercises") }
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_EXERCISE_ID) { type = NavType.StringType }),
        ) { Text("detail") }
        composable(Routes.SETTINGS) { Text("settings") }
    }
}
