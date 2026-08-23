package com.goveye.app.data.local.dao

/**
 * Summary of a council for the Directory list view.
 * Lightweight projection of [com.goveye.app.data.local.entity.CouncilEntity].
 */
data class CouncilSummary(
    val id: Int,
    val name: String,
    val website: String?,
    val localAuthorityType: String?,
    val region: String?
)
