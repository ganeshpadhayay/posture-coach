package com.posturecoach.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.posturecoach.data.repository.SettingsRepository
import com.posturecoach.ui.exercises.ExerciseDetailScreen
import com.posturecoach.ui.exercises.ExerciseListScreen
import com.posturecoach.ui.home.HomeScreen
import com.posturecoach.ui.onboarding.OnboardingScreen
import com.posturecoach.ui.results.ResultsScreen
import com.posturecoach.ui.scan.ScanScreen
import com.posturecoach.ui.settings.SettingsScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

@HiltViewModel
class RootViewModel @Inject constructor(
    settings: SettingsRepository,
) : ViewModel() {
    val onboardingComplete = settings.onboardingComplete.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = null,
    )
}

@Composable
fun PostureCoachNavGraph(viewModel: RootViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()

    val start = when (onboardingComplete) {
        null -> null
        true -> Routes.HOME
        false -> Routes.ONBOARDING
    }
    if (start == null) return

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onCheckPosture = { navController.navigate(Routes.SCAN) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenScan = { scanId -> navController.navigate(Routes.results(scanId)) },
            )
        }
        composable(Routes.SCAN) {
            ScanScreen(
                onCaptured = { scanId ->
                    navController.navigate(Routes.results(scanId)) {
                        popUpTo(Routes.SCAN) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.RESULTS,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.StringType }),
        ) {
            ResultsScreen(
                onRetake = {
                    navController.navigate(Routes.SCAN) {
                        popUpTo(Routes.HOME)
                    }
                },
                onViewExercises = { scanId -> navController.navigate(Routes.exercises(scanId)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EXERCISES,
            arguments = listOf(navArgument(Routes.ARG_SCAN_ID) { type = NavType.StringType }),
        ) {
            ExerciseListScreen(
                onExerciseClick = { id -> navController.navigate(Routes.exerciseDetail(id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.EXERCISE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_EXERCISE_ID) { type = NavType.StringType }),
        ) {
            ExerciseDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
