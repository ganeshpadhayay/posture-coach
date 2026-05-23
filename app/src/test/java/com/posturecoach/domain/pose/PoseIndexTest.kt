package com.posturecoach.domain.pose

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PoseIndexTest {

    @Test
    fun `all named indices are within MediaPipe 33-keypoint range`() {
        PoseIndex.NAMES.keys.forEach { index ->
            assertThat(index).isAtLeast(0)
            assertThat(index).isLessThan(33)
        }
    }

    @Test
    fun `skeleton edges reference only known indices`() {
        val knownIndices = PoseIndex.NAMES.keys
        PoseIndex.SKELETON_EDGES.forEach { (a, b) ->
            assertThat(knownIndices).contains(a)
            assertThat(knownIndices).contains(b)
        }
    }

    @Test
    fun `skeleton edges have no duplicates ignoring direction`() {
        val canonical = PoseIndex.SKELETON_EDGES.map { (a, b) -> setOf(a, b) }
        assertThat(canonical).containsNoDuplicates()
    }

    @Test
    fun `every left landmark has a right counterpart in NAMES`() {
        val lefts = PoseIndex.NAMES.values.filter { it.startsWith("left") }
        val rights = PoseIndex.NAMES.values.filter { it.startsWith("right") }.toSet()
        lefts.forEach { left ->
            val expectedRight = "right" + left.removePrefix("left")
            assertThat(rights).contains(expectedRight)
        }
    }

    @Test
    fun `key landmark constants match expected MediaPipe indices`() {
        assertThat(PoseIndex.NOSE).isEqualTo(0)
        assertThat(PoseIndex.LEFT_EAR).isEqualTo(7)
        assertThat(PoseIndex.RIGHT_EAR).isEqualTo(8)
        assertThat(PoseIndex.LEFT_SHOULDER).isEqualTo(11)
        assertThat(PoseIndex.RIGHT_SHOULDER).isEqualTo(12)
        assertThat(PoseIndex.LEFT_HIP).isEqualTo(23)
        assertThat(PoseIndex.RIGHT_HIP).isEqualTo(24)
    }
}
