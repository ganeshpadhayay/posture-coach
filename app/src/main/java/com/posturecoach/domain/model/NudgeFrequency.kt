package com.posturecoach.domain.model

enum class NudgeFrequency {
    LOW,
    MEDIUM,
    HIGH;

    companion object {
        fun fromName(name: String?): NudgeFrequency =
            entries.firstOrNull { it.name == name } ?: MEDIUM
    }
}
