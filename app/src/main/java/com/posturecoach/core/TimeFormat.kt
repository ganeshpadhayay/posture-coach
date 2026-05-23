package com.posturecoach.core

object TimeFormat {
    fun durationShort(ms: Long): String {
        val totalMin = (ms / 60_000L).coerceAtLeast(0L)
        val h = totalMin / 60
        val m = totalMin % 60
        return when {
            h > 0 -> "${h}h ${m}m"
            else -> "${m}m"
        }
    }
}
