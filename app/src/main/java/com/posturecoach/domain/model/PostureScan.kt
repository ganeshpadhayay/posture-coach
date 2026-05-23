package com.posturecoach.domain.model

data class PostureScan(
    val id: String,
    val timestampMs: Long,
    val imagePath: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val issues: List<PostureIssue>,
    val angles: Map<String, Float>,
    val landmarks: List<PoseLandmark>,
)

data class PoseLandmark(
    val name: String,
    val x: Float,
    val y: Float,
    val visibility: Float,
)
