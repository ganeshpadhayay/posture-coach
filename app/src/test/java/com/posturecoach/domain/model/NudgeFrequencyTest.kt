package com.posturecoach.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NudgeFrequencyTest {

    @Test
    fun `null falls back to MEDIUM`() {
        assertThat(NudgeFrequency.fromName(null)).isEqualTo(NudgeFrequency.MEDIUM)
    }

    @Test
    fun `empty string falls back to MEDIUM`() {
        assertThat(NudgeFrequency.fromName("")).isEqualTo(NudgeFrequency.MEDIUM)
    }

    @Test
    fun `every enum name round trips through fromName`() {
        NudgeFrequency.entries.forEach { value ->
            assertThat(NudgeFrequency.fromName(value.name)).isEqualTo(value)
        }
    }

    @Test
    fun `garbage falls back to MEDIUM`() {
        assertThat(NudgeFrequency.fromName("FRIDAY")).isEqualTo(NudgeFrequency.MEDIUM)
        assertThat(NudgeFrequency.fromName("low")).isEqualTo(NudgeFrequency.MEDIUM) // case-sensitive
    }
}
