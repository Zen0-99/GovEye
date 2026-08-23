package com.goveye.app.data.local.dao

data class CommitteeSummary(
    val id: Int,
    val name: String,
    val house: String?,
    val categoryName: String?,
    val memberCount: Int,
    val isActive: Boolean
)
