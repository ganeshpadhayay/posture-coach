package com.posturecoach.ui.nav

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val SCAN = "scan"
    const val RESULTS = "results/{scanId}"
    const val EXERCISES = "exercises/{scanId}"
    const val EXERCISE_DETAIL = "exercise/{exerciseId}"
    const val SETTINGS = "settings"

    const val ARG_SCAN_ID = "scanId"
    const val ARG_EXERCISE_ID = "exerciseId"

    fun results(scanId: String) = "results/$scanId"
    fun exercises(scanId: String) = "exercises/$scanId"
    fun exerciseDetail(exerciseId: String) = "exercise/$exerciseId"
}
