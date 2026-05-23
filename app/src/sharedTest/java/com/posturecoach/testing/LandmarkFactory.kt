package com.posturecoach.testing

import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.pose.PoseIndex

/**
 * Builders for the 33-keypoint MediaPipe pose used by [com.posturecoach.domain.pose.PostureRules].
 * Each builder returns a normalized list (x,y in [0,1]) shaped like a typical side-profile
 * person, with whatever deviation the test needs.
 *
 * Coordinates are tuned to keep all rules below threshold by default; tweak in the
 * builder lambda to create targeted bad postures.
 */
object LandmarkFactory {

    /**
     * Reasonably aligned side profile: ear over shoulder over hip, level shoulders.
     * `evaluateForwardHead` ratio ≈ 0.0, shoulder tilt ≈ 0°, spine tilt ≈ 0°.
     */
    fun aligned(visibility: Float = 1f): MutableList<PoseLandmark> {
        val list = MutableList(33) { index ->
            PoseLandmark(
                name = PoseIndex.NAMES[index] ?: "lm_$index",
                x = 0.5f,
                y = 0.5f,
                visibility = visibility,
            )
        }
        list[PoseIndex.NOSE] = lm("nose", 0.5f, 0.15f, visibility)
        list[PoseIndex.LEFT_EAR] = lm("leftEar", 0.5f, 0.18f, visibility)
        list[PoseIndex.RIGHT_EAR] = lm("rightEar", 0.5f, 0.18f, visibility)
        list[PoseIndex.LEFT_SHOULDER] = lm("leftShoulder", 0.5f, 0.32f, visibility)
        list[PoseIndex.RIGHT_SHOULDER] = lm("rightShoulder", 0.5f, 0.32f, visibility)
        list[PoseIndex.LEFT_HIP] = lm("leftHip", 0.5f, 0.60f, visibility)
        list[PoseIndex.RIGHT_HIP] = lm("rightHip", 0.5f, 0.60f, visibility)
        list[PoseIndex.LEFT_KNEE] = lm("leftKnee", 0.5f, 0.78f, visibility)
        list[PoseIndex.RIGHT_KNEE] = lm("rightKnee", 0.5f, 0.78f, visibility)
        list[PoseIndex.LEFT_ANKLE] = lm("leftAnkle", 0.5f, 0.95f, visibility)
        list[PoseIndex.RIGHT_ANKLE] = lm("rightAnkle", 0.5f, 0.95f, visibility)
        return list
    }

    /**
     * Side profile with the ear translated forward of the shoulder by [ratio] * torsoLength.
     * `ratio > FORWARD_HEAD_RATIO_THRESHOLD (0.18)` should trigger FORWARD_HEAD.
     */
    fun forwardHead(ratio: Float = 0.25f, visibility: Float = 1f): MutableList<PoseLandmark> {
        val list = aligned(visibility)
        val shoulderY = list[PoseIndex.LEFT_SHOULDER].y
        val hipY = list[PoseIndex.LEFT_HIP].y
        val torso = hipY - shoulderY
        val xOffset = ratio * torso
        list[PoseIndex.LEFT_EAR] = list[PoseIndex.LEFT_EAR].copy(x = 0.5f + xOffset)
        list[PoseIndex.RIGHT_EAR] = list[PoseIndex.RIGHT_EAR].copy(x = 0.5f + xOffset)
        return list
    }

    /**
     * Spine tilted forward by [degrees] from vertical (positive = leaning forward toward
     * face direction). `abs(degrees) > SLOUCH_DEG_THRESHOLD (15)` should trigger SLOUCHING.
     */
    fun slouch(degrees: Float = 20f, visibility: Float = 1f): MutableList<PoseLandmark> {
        val list = aligned(visibility)
        val shoulderY = list[PoseIndex.LEFT_SHOULDER].y
        val hipY = list[PoseIndex.LEFT_HIP].y
        val torso = hipY - shoulderY
        val rad = Math.toRadians(degrees.toDouble())
        val dx = (Math.tan(rad) * torso).toFloat()
        list[PoseIndex.LEFT_SHOULDER] = list[PoseIndex.LEFT_SHOULDER].copy(x = 0.5f + dx)
        list[PoseIndex.RIGHT_SHOULDER] = list[PoseIndex.RIGHT_SHOULDER].copy(x = 0.5f + dx)
        return list
    }

    /**
     * Shoulder line tilted from horizontal by [degrees]. Positive = right shoulder lower
     * than left. `abs(degrees) > ROUNDED_SHOULDER_DEG_THRESHOLD (10)` triggers ROUNDED_SHOULDERS.
     */
    fun roundedShoulders(degrees: Float = 15f, visibility: Float = 1f): MutableList<PoseLandmark> {
        val list = aligned(visibility)
        val rad = Math.toRadians(degrees.toDouble())
        val dy = (Math.tan(rad) * 0.2f).toFloat()
        list[PoseIndex.LEFT_SHOULDER] = list[PoseIndex.LEFT_SHOULDER].copy(x = 0.4f, y = 0.32f)
        list[PoseIndex.RIGHT_SHOULDER] = list[PoseIndex.RIGHT_SHOULDER].copy(x = 0.6f, y = 0.32f + dy)
        return list
    }

    private fun lm(name: String, x: Float, y: Float, visibility: Float) =
        PoseLandmark(name = name, x = x, y = y, visibility = visibility)
}
