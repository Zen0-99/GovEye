package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Cached historical government publication for on-demand fetch (D-02).
 *
 * Publications older than 90 days are not in the bundled DB — they are
 * fetched on-demand from the GOV.UK Content API per-user and cached in
 * [com.goveye.app.data.local.LocalDatabase]. This is a new architectural
 * pattern (on-demand fetch + cache) not present elsewhere in the app.
 *
 * Fields:
 * - url: the GOV.UK content path (primary key, unique per publication)
 * - title / summary / bodyText: full content from Content API
 * - documentType / organisation: metadata
 * - imageUrl: from Content API details.image (nullable)
 * - firstPublishedAt: ISO timestamp
 * - fetchedAt: epoch millis when this cache entry was created (for TTL cleanup)
 */
@Serializable
@Entity(tableName = "cached_publications")
data class CachedPublicationEntity(
    @PrimaryKey val url: String,
    val title: String,
    val summary: String,
    val bodyText: String,
    val documentType: String,
    val organisation: String,
    val imageUrl: String?,
    val firstPublishedAt: String,
    val fetchedAt: Long
)
