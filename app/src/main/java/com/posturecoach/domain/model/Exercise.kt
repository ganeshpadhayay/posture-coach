package com.posturecoach.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val issue: String,
    val instructions: List<String>,
    val durationSec: Int,
    val gifAsset: String,
)

@Serializable
data class ExerciseCatalog(
    val exercises: List<Exercise>,
)
