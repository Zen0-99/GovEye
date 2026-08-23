package com.goveye.app.domain.model

data class SourceRecommendation(
    val tag: String,
    val organisationSlug: String,
    val organisationName: String,
    val hitCount: Int,
    val isRecommended: Boolean
)
