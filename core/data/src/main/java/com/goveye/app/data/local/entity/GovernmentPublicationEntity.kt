package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Government publication from the GOV.UK Content API.
 *
 * Produced by `build_gov_publications.py` at build time. Stores the last
 * 90 days of publications (D-02) with title, summary, URL, tags, image_url,
 * and bodyText (stripped plain text from the Content API, used for in-app
 * reading without linking out to GOV.UK).
 *
 * Fields mirror the GOV.UK Search API + Content API response shapes:
 * - id: sequential integer ID assigned by the build script
 * - title / summary / url: from Search API results
 * - documentType: e.g. "news_article", "press_release", "speech"
 * - organisation / organisationSlug: publishing department
 * - firstPublishedAt / publicUpdatedAt: ISO timestamp strings
 * - imageUrl: from Content API details.image / details.images field (nullable)
 * - bodyText: full article body as plain text (HTML stripped via BeautifulSoup)
 */
@Entity(tableName = "government_publications")
data class GovernmentPublicationEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val summary: String,
    val url: String,
    val documentType: String,
    val organisation: String,
    val organisationSlug: String,
    val firstPublishedAt: String,
    val publicUpdatedAt: String,
    val imageUrl: String?,
    val bodyText: String?,
    val lastUpdated: Long
)
