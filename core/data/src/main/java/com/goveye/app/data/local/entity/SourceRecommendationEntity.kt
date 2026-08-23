package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Source recommendation: tag → department mapping for onboarding source
 * suggestions (D-06). Produced by `build_source_recs.py` at build time.
 *
 * Hybrid mapping: hardcoded base mapping refined by data-driven publication
 * tag hit counts. Departments with high hit counts for a tag get recommended
 * even if not in the hardcoded mapping.
 *
 * Composite key: (tag, organisationSlug).
 */
@Entity(
    tableName = "source_recommendations",
    primaryKeys = ["tag", "organisationSlug"]
)
data class SourceRecommendationEntity(
    val tag: String,
    val organisationSlug: String,
    val organisationName: String,
    val hitCount: Int,
    val isRecommended: Boolean
)
