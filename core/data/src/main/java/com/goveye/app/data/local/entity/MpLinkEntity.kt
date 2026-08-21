package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mp_links")
data class MpLinkEntity(
    @PrimaryKey val mpId: Int,
    val twitterHandle: String? = null,
    val facebookUrl: String? = null,
    val instagramUrl: String? = null,
    val linkedinUrl: String? = null,
    val wikipediaUrl: String? = null,
    val personalWebsiteUrl: String? = null,
    val lastUpdated: Long
)
