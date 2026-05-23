package com.posturecoach.domain.model

import com.posturecoach.R

enum class PostureIssue(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
) {
    FORWARD_HEAD("forward_head", R.string.issue_forward_head, R.string.issue_forward_head_desc),
    ROUNDED_SHOULDERS("rounded_shoulders", R.string.issue_rounded_shoulders, R.string.issue_rounded_shoulders_desc),
    SLOUCHING("slouching", R.string.issue_slouching, R.string.issue_slouching_desc);

    companion object {
        fun fromId(id: String): PostureIssue? = entries.firstOrNull { it.id == id }
    }
}
