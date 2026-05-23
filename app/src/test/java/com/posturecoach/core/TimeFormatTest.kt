package com.posturecoach.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TimeFormatTest {

    @Test
    fun `formats minutes when less than one hour`() {
        assertThat(TimeFormat.durationShort(0)).isEqualTo("0m")
        assertThat(TimeFormat.durationShort(45 * 60_000L)).isEqualTo("45m")
    }

    @Test
    fun `formats hours and minutes`() {
        assertThat(TimeFormat.durationShort(2 * 60 * 60_000L + 15 * 60_000L)).isEqualTo("2h 15m")
    }

    @Test
    fun `negatives clamp to zero`() {
        assertThat(TimeFormat.durationShort(-1L)).isEqualTo("0m")
    }
}
