package com.posturecoach.domain.pose

import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.testing.LandmarkFactory
import org.junit.Test
import kotlin.math.abs

class PostureRulesTest {

    @Test
    fun `less than 33 landmarks returns no issues`() {
        val result = PostureRules.analyze(emptyList())
        assertThat(result.issues).isEmpty()
        assertThat(result.angles).isEmpty()

        val partial = LandmarkFactory.aligned().take(20)
        val partialResult = PostureRules.analyze(partial)
        assertThat(partialResult.issues).isEmpty()
        assertThat(partialResult.angles).isEmpty()
    }

    @Test
    fun `aligned posture flags nothing`() {
        val result = PostureRules.analyze(LandmarkFactory.aligned())
        assertThat(result.issues).isEmpty()
        // Angles are still computed even when nothing fires.
        assertThat(result.angles).containsKey(PostureRules.ANGLE_FORWARD_HEAD_RATIO)
        assertThat(result.angles).containsKey(PostureRules.ANGLE_SPINE_TILT_DEG)
        assertThat(result.angles[PostureRules.ANGLE_FORWARD_HEAD_RATIO]!!).isWithin(1e-3f).of(0f)
    }

    // ───── Forward head: boundary table ─────

    @Test
    fun `forward head boundary just below threshold does not flag`() {
        val just = PostureRules.FORWARD_HEAD_RATIO_THRESHOLD - 0.01f
        val landmarks = LandmarkFactory.forwardHead(ratio = just)
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).doesNotContain(PostureIssue.FORWARD_HEAD)
        val ratio = result.angles[PostureRules.ANGLE_FORWARD_HEAD_RATIO]!!
        assertThat(ratio).isLessThan(PostureRules.FORWARD_HEAD_RATIO_THRESHOLD)
    }

    @Test
    fun `forward head boundary just above threshold flags`() {
        val just = PostureRules.FORWARD_HEAD_RATIO_THRESHOLD + 0.05f
        val landmarks = LandmarkFactory.forwardHead(ratio = just)
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).contains(PostureIssue.FORWARD_HEAD)
        val ratio = result.angles[PostureRules.ANGLE_FORWARD_HEAD_RATIO]!!
        assertThat(ratio).isGreaterThan(PostureRules.FORWARD_HEAD_RATIO_THRESHOLD)
    }

    // ───── Slouching: boundary + direction ─────

    @Test
    fun `slouching boundary just below threshold does not flag`() {
        val landmarks = LandmarkFactory.slouch(degrees = PostureRules.SLOUCH_DEG_THRESHOLD - 1f)
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).doesNotContain(PostureIssue.SLOUCHING)
    }

    @Test
    fun `slouching above threshold flags forward and backward`() {
        val forwardLandmarks = LandmarkFactory.slouch(degrees = PostureRules.SLOUCH_DEG_THRESHOLD + 5f)
        assertThat(PostureRules.analyze(forwardLandmarks).issues).contains(PostureIssue.SLOUCHING)

        // Rule uses abs() of tilt so a backward tilt (negative degrees) should also flag.
        val backwardLandmarks = LandmarkFactory.slouch(degrees = -(PostureRules.SLOUCH_DEG_THRESHOLD + 5f))
        assertThat(PostureRules.analyze(backwardLandmarks).issues).contains(PostureIssue.SLOUCHING)
    }

    // ───── Rounded shoulders: boundary + sign ─────

    @Test
    fun `rounded shoulders boundary just below threshold does not flag`() {
        val landmarks = LandmarkFactory.roundedShoulders(degrees = PostureRules.ROUNDED_SHOULDER_DEG_THRESHOLD - 2f)
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).doesNotContain(PostureIssue.ROUNDED_SHOULDERS)
    }

    @Test
    fun `rounded shoulders flags positive and negative tilt`() {
        val positive = LandmarkFactory.roundedShoulders(degrees = PostureRules.ROUNDED_SHOULDER_DEG_THRESHOLD + 5f)
        assertThat(PostureRules.analyze(positive).issues).contains(PostureIssue.ROUNDED_SHOULDERS)

        val negative = LandmarkFactory.roundedShoulders(degrees = -(PostureRules.ROUNDED_SHOULDER_DEG_THRESHOLD + 5f))
        assertThat(PostureRules.analyze(negative).issues).contains(PostureIssue.ROUNDED_SHOULDERS)
    }

    // ───── Side selection ─────

    @Test
    fun `low visibility on left falls through to right side`() {
        val landmarks = LandmarkFactory.forwardHead(ratio = 0.25f).toMutableList()
        // Kill left side visibility — right side still has the forward-head offset.
        listOf(PoseIndex.LEFT_EAR, PoseIndex.LEFT_SHOULDER, PoseIndex.LEFT_HIP).forEach { i ->
            landmarks[i] = landmarks[i].copy(visibility = 0.05f)
        }
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).contains(PostureIssue.FORWARD_HEAD)
    }

    @Test
    fun `both sides below visibility threshold drops forward head measurement`() {
        val landmarks = LandmarkFactory.forwardHead(ratio = 0.4f).toMutableList()
        listOf(
            PoseIndex.LEFT_EAR, PoseIndex.LEFT_SHOULDER, PoseIndex.LEFT_HIP,
            PoseIndex.RIGHT_EAR, PoseIndex.RIGHT_SHOULDER, PoseIndex.RIGHT_HIP,
        ).forEach { i -> landmarks[i] = landmarks[i].copy(visibility = 0.1f) }
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).doesNotContain(PostureIssue.FORWARD_HEAD)
        assertThat(result.angles).doesNotContainKey(PostureRules.ANGLE_FORWARD_HEAD_RATIO)
    }

    @Test
    fun `shoulder tilt skipped when visibility is too low`() {
        val landmarks = LandmarkFactory.roundedShoulders(degrees = 20f).toMutableList()
        landmarks[PoseIndex.LEFT_SHOULDER] = landmarks[PoseIndex.LEFT_SHOULDER].copy(visibility = 0.1f)
        landmarks[PoseIndex.RIGHT_SHOULDER] = landmarks[PoseIndex.RIGHT_SHOULDER].copy(visibility = 0.1f)
        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).doesNotContain(PostureIssue.ROUNDED_SHOULDERS)
        assertThat(result.angles).doesNotContainKey(PostureRules.ANGLE_SHOULDER_TILT_DEG)
    }

    // ───── Combined ─────

    @Test
    fun `multiple issues are detected together and deduplicated`() {
        val landmarks = LandmarkFactory.forwardHead(ratio = 0.3f)
        // Apply a slouch on top of forward head.
        val shoulderY = landmarks[PoseIndex.LEFT_SHOULDER].y
        val hipY = landmarks[PoseIndex.LEFT_HIP].y
        val torso = hipY - shoulderY
        val dx = (Math.tan(Math.toRadians(20.0)) * torso).toFloat()
        landmarks[PoseIndex.LEFT_SHOULDER] = landmarks[PoseIndex.LEFT_SHOULDER].copy(x = 0.5f + dx)
        landmarks[PoseIndex.RIGHT_SHOULDER] = landmarks[PoseIndex.RIGHT_SHOULDER].copy(x = 0.5f + dx)

        val result = PostureRules.analyze(landmarks)
        assertThat(result.issues).containsAtLeast(PostureIssue.FORWARD_HEAD, PostureIssue.SLOUCHING)
        // Deduplication: each issue appears at most once.
        assertThat(result.issues).hasSize(result.issues.distinct().size)
    }

    @Test
    fun `tiny torso length still computes finite ratio`() {
        val landmarks = LandmarkFactory.aligned().toMutableList()
        // Collapse hip onto shoulder; rule should still return a finite ratio thanks to coerceAtLeast.
        landmarks[PoseIndex.LEFT_HIP] = landmarks[PoseIndex.LEFT_HIP].copy(y = 0.32f)
        landmarks[PoseIndex.RIGHT_HIP] = landmarks[PoseIndex.RIGHT_HIP].copy(y = 0.32f)
        landmarks[PoseIndex.LEFT_EAR] = landmarks[PoseIndex.LEFT_EAR].copy(x = 0.6f)
        landmarks[PoseIndex.RIGHT_EAR] = landmarks[PoseIndex.RIGHT_EAR].copy(x = 0.6f)
        val result = PostureRules.analyze(landmarks)
        val ratio = result.angles[PostureRules.ANGLE_FORWARD_HEAD_RATIO]
        assertThat(ratio).isNotNull()
        assertThat(ratio!!.isFinite()).isTrue()
        assertThat(abs(ratio)).isGreaterThan(0f)
    }

    @Test
    fun `analyze preserves landmark visibility data unchanged`() {
        val landmarks: List<PoseLandmark> = LandmarkFactory.aligned()
        val before = landmarks.map { Triple(it.x, it.y, it.visibility) }
        PostureRules.analyze(landmarks)
        val after = landmarks.map { Triple(it.x, it.y, it.visibility) }
        assertThat(after).isEqualTo(before)
    }
}
