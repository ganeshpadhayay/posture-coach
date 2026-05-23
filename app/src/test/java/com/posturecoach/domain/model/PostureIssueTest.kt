package com.posturecoach.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PostureIssueTest {

    @Test
    fun `fromId resolves every known id`() {
        PostureIssue.entries.forEach { issue ->
            assertThat(PostureIssue.fromId(issue.id)).isEqualTo(issue)
        }
    }

    @Test
    fun `fromId returns null for unknown id`() {
        assertThat(PostureIssue.fromId("unknown")).isNull()
        assertThat(PostureIssue.fromId("")).isNull()
        assertThat(PostureIssue.fromId("FORWARD_HEAD")).isNull() // ids are lower_snake
    }

    @Test
    fun `every entry has a distinct id`() {
        val ids = PostureIssue.entries.map { it.id }
        assertThat(ids).containsNoDuplicates()
    }
}
