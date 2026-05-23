package com.posturecoach.data.db

import com.google.common.truth.Truth.assertThat
import com.posturecoach.domain.model.PoseLandmark
import com.posturecoach.domain.model.PostureIssue
import com.posturecoach.testing.LandmarkFactory
import com.posturecoach.testing.ScanFactory
import kotlinx.serialization.json.Json
import org.junit.Test

class MappersTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun `round trip preserves all simple fields`() {
        val scan = ScanFactory.sample(
            id = "abc-123",
            timestampMs = 42L,
            imagePath = "/tmp/x.jpg",
            imageWidth = 720,
            imageHeight = 1280,
        )

        val roundTripped = scan.toEntity(json).toDomain(json)

        assertThat(roundTripped.id).isEqualTo("abc-123")
        assertThat(roundTripped.timestampMs).isEqualTo(42L)
        assertThat(roundTripped.imagePath).isEqualTo("/tmp/x.jpg")
        assertThat(roundTripped.imageWidth).isEqualTo(720)
        assertThat(roundTripped.imageHeight).isEqualTo(1280)
    }

    @Test
    fun `round trip preserves all issues`() {
        val scan = ScanFactory.sample(
            issues = listOf(
                PostureIssue.FORWARD_HEAD,
                PostureIssue.ROUNDED_SHOULDERS,
                PostureIssue.SLOUCHING,
            ),
        )

        val out = scan.toEntity(json).toDomain(json)

        assertThat(out.issues).containsExactly(
            PostureIssue.FORWARD_HEAD,
            PostureIssue.ROUNDED_SHOULDERS,
            PostureIssue.SLOUCHING,
        ).inOrder()
    }

    @Test
    fun `round trip preserves angles to float precision`() {
        val scan = ScanFactory.sample(
            angles = mapOf(
                "forwardHeadRatio" to 0.2345f,
                "spineTiltDeg" to -7.5f,
                "shoulderTiltDeg" to 12.125f,
            ),
        )

        val out = scan.toEntity(json).toDomain(json)

        assertThat(out.angles.keys).containsExactlyElementsIn(scan.angles.keys)
        scan.angles.forEach { (k, v) ->
            assertThat(out.angles[k]!!).isWithin(1e-5f).of(v)
        }
    }

    @Test
    fun `round trip preserves 33 landmarks with names and visibility`() {
        val landmarks = LandmarkFactory.aligned()
        val scan = ScanFactory.sample(landmarks = landmarks)

        val out = scan.toEntity(json).toDomain(json)

        assertThat(out.landmarks).hasSize(33)
        out.landmarks.forEachIndexed { i, lm ->
            val expected: PoseLandmark = landmarks[i]
            assertThat(lm.name).isEqualTo(expected.name)
            assertThat(lm.x).isWithin(1e-5f).of(expected.x)
            assertThat(lm.y).isWithin(1e-5f).of(expected.y)
            assertThat(lm.visibility).isWithin(1e-5f).of(expected.visibility)
        }
    }

    @Test
    fun `empty issues and angles round trip cleanly`() {
        val scan = ScanFactory.sample(
            issues = emptyList(),
            angles = emptyMap(),
            landmarks = emptyList(),
        )

        val entity = scan.toEntity(json)
        assertThat(entity.issuesCsv).isEmpty()

        val out = entity.toDomain(json)
        assertThat(out.issues).isEmpty()
        assertThat(out.angles).isEmpty()
        assertThat(out.landmarks).isEmpty()
    }

    @Test
    fun `unknown issue id in csv is dropped silently`() {
        val scan = ScanFactory.sample(issues = listOf(PostureIssue.FORWARD_HEAD))
        val entity = scan.toEntity(json)
        val polluted = entity.copy(issuesCsv = "${entity.issuesCsv},garbage_issue,")

        val out = polluted.toDomain(json)

        assertThat(out.issues).containsExactly(PostureIssue.FORWARD_HEAD)
    }

    @Test
    fun `malformed angles json falls back to empty map`() {
        val scan = ScanFactory.sample()
        val entity = scan.toEntity(json).copy(anglesJson = "not_json{")

        val out = entity.toDomain(json)

        assertThat(out.angles).isEmpty()
    }

    @Test
    fun `malformed landmarks json falls back to empty list`() {
        val scan = ScanFactory.sample()
        val entity = scan.toEntity(json).copy(landmarksJson = "{broken")

        val out = entity.toDomain(json)

        assertThat(out.landmarks).isEmpty()
    }
}
