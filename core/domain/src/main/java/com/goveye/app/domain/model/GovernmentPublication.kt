package com.goveye.app.domain.model

data class GovernmentPublication(
    val id: Int,
    val title: String,
    val summary: String,
    val url: String,
    val documentType: String,
    val organisation: String,
    val organisationSlug: String,
    val firstPublishedAt: String,
    val publicUpdatedAt: String,
    val imageUrl: String?,
    val bodyText: String?
)
