package com.posturecoach.testing

import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.domain.model.PostureScan

object ScanFactory {

    fun sample(
        id: String = "scan-1",
        timestampMs: Long = 1_700_000_000_000L,
        imagePath: String = "/tmp/scan.jpg",
        imageWidth: Int = 1080,
        imageHeight: Int = 1920,
        issues: List<PostureIssue> = listOf(PostureIssue.FORWARD_HEAD),
        angles: Map<String, Float> = mapOf("forwardHeadRatio" to 0.25f),
        landmarks: List<PoseLandmark> = LandmarkFactory.aligned(),
    ): PostureScan = PostureScan(
        id = id,
        timestampMs = timestampMs,
        imagePath = imagePath,
        imageWidth = imageWidth,
        imageHeight = imageHeight,
        issues = issues,
        angles = angles,
        landmarks = landmarks,
    )
}
