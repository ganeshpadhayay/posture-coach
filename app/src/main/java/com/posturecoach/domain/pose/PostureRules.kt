package com.posturecoach.domain.pose

import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.model.PostureIssue
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

data class PostureAnalysisResult(
    val issues: List<PostureIssue>,
    val angles: Map<String, Float>,
)

/**
 * Rule-based posture analyzer working from normalized MediaPipe landmarks (x,y in [0,1]).
 * Designed for a side-profile photo, but defensively handles fronts/3-quarters by
 * selecting whichever side has higher visibility.
 *
 * All thresholds are conservative and tunable. Copy in `PostureIssue` is intentionally soft.
 */
object PostureRules {

    /** Forward-head: |ear.x - shoulder.x| normalized by torso length. */
    const val FORWARD_HEAD_RATIO_THRESHOLD = 0.18f

    /** Rounded shoulders: shoulder-line tilt from horizontal in degrees. */
    const val ROUNDED_SHOULDER_DEG_THRESHOLD = 10f

    /** Slouching: spine deviation from vertical in degrees. */
    const val SLOUCH_DEG_THRESHOLD = 15f

    /** Minimum per-keypoint visibility to trust a measurement. */
    const val MIN_VISIBILITY = 0.4f

    const val ANGLE_FORWARD_HEAD_RATIO = "forwardHeadRatio"
    const val ANGLE_SHOULDER_TILT_DEG = "shoulderTiltDeg"
    const val ANGLE_SPINE_TILT_DEG = "spineTiltDeg"

    fun analyze(landmarks: List<PoseLandmark>): PostureAnalysisResult {
        if (landmarks.size < 33) return PostureAnalysisResult(emptyList(), emptyMap())

        val issues = mutableListOf<PostureIssue>()
        val angles = mutableMapOf<String, Float>()

        evaluateForwardHead(landmarks)?.let { (ratio, flagged) ->
            angles[ANGLE_FORWARD_HEAD_RATIO] = ratio
            if (flagged) issues += PostureIssue.FORWARD_HEAD
        }
        evaluateShoulderTilt(landmarks)?.let { (deg, flagged) ->
            angles[ANGLE_SHOULDER_TILT_DEG] = deg
            if (flagged) issues += PostureIssue.ROUNDED_SHOULDERS
        }
        evaluateSpineTilt(landmarks)?.let { (deg, flagged) ->
            angles[ANGLE_SPINE_TILT_DEG] = deg
            if (flagged) issues += PostureIssue.SLOUCHING
        }

        return PostureAnalysisResult(issues.distinct(), angles)
    }

    private fun evaluateForwardHead(lm: List<PoseLandmark>): Pair<Float, Boolean>? {
        val leftEar = lm.getOrNull(PoseIndex.LEFT_EAR)
        val rightEar = lm.getOrNull(PoseIndex.RIGHT_EAR)
        val leftShoulder = lm.getOrNull(PoseIndex.LEFT_SHOULDER)
        val rightShoulder = lm.getOrNull(PoseIndex.RIGHT_SHOULDER)
        val leftHip = lm.getOrNull(PoseIndex.LEFT_HIP)
        val rightHip = lm.getOrNull(PoseIndex.RIGHT_HIP)
        if (leftEar == null || rightEar == null || leftShoulder == null || rightShoulder == null ||
            leftHip == null || rightHip == null
        ) return null

        val (ear, shoulder, hip) = pickSide(leftEar, rightEar, leftShoulder, rightShoulder, leftHip, rightHip)
            ?: return null

        val torsoLen = distance(shoulder, hip).coerceAtLeast(1e-3f)
        val horizontalOffset = abs(ear.x - shoulder.x)
        val ratio = horizontalOffset / torsoLen
        return ratio to (ratio > FORWARD_HEAD_RATIO_THRESHOLD)
    }

    private fun evaluateShoulderTilt(lm: List<PoseLandmark>): Pair<Float, Boolean>? {
        val left = lm.getOrNull(PoseIndex.LEFT_SHOULDER) ?: return null
        val right = lm.getOrNull(PoseIndex.RIGHT_SHOULDER) ?: return null
        if (left.visibility < MIN_VISIBILITY || right.visibility < MIN_VISIBILITY) return null
        val tilt = angleFromHorizontalDeg(left, right)
        return tilt to (abs(tilt) > ROUNDED_SHOULDER_DEG_THRESHOLD)
    }

    private fun evaluateSpineTilt(lm: List<PoseLandmark>): Pair<Float, Boolean>? {
        val leftShoulder = lm.getOrNull(PoseIndex.LEFT_SHOULDER) ?: return null
        val rightShoulder = lm.getOrNull(PoseIndex.RIGHT_SHOULDER) ?: return null
        val leftHip = lm.getOrNull(PoseIndex.LEFT_HIP) ?: return null
        val rightHip = lm.getOrNull(PoseIndex.RIGHT_HIP) ?: return null

        val midShoulder = midpoint(leftShoulder, rightShoulder)
        val midHip = midpoint(leftHip, rightHip)
        val tilt = angleFromVerticalDeg(midShoulder, midHip)
        return tilt to (abs(tilt) > SLOUCH_DEG_THRESHOLD)
    }

    private fun pickSide(
        leftEar: PoseLandmark,
        rightEar: PoseLandmark,
        leftShoulder: PoseLandmark,
        rightShoulder: PoseLandmark,
        leftHip: PoseLandmark,
        rightHip: PoseLandmark,
    ): Triple<PoseLandmark, PoseLandmark, PoseLandmark>? {
        val leftVis = minOf(leftEar.visibility, leftShoulder.visibility, leftHip.visibility)
        val rightVis = minOf(rightEar.visibility, rightShoulder.visibility, rightHip.visibility)
        return when {
            leftVis >= rightVis && leftVis >= MIN_VISIBILITY ->
                Triple(leftEar, leftShoulder, leftHip)
            rightVis >= MIN_VISIBILITY ->
                Triple(rightEar, rightShoulder, rightHip)
            else -> null
        }
    }

    private fun midpoint(a: PoseLandmark, b: PoseLandmark): PoseLandmark =
        PoseLandmark("mid", (a.x + b.x) / 2f, (a.y + b.y) / 2f, minOf(a.visibility, b.visibility))

    private fun distance(a: PoseLandmark, b: PoseLandmark): Float {
        val dx = a.x - b.x
        val dy = a.y - b.y
        return sqrt(dx * dx + dy * dy)
    }

    private fun angleFromHorizontalDeg(a: PoseLandmark, b: PoseLandmark): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
    }

    private fun angleFromVerticalDeg(top: PoseLandmark, bottom: PoseLandmark): Float {
        val dx = top.x - bottom.x
        val dy = top.y - bottom.y
        val rad = atan2(dx.toDouble(), -dy.toDouble())
        return Math.toDegrees(rad).toFloat()
    }
}
