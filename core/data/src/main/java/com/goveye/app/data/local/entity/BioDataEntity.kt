package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bio_data")
data class BioDataEntity(
    @PrimaryKey val mpId: Int,
    val maidenSpeechDate: String? = null,
    val dateOfBirth: String? = null,
    val townOfBirth: String? = null,
    val countryOfBirth: String? = null,
    val honoursJson: String? = null,
    val postsJson: String? = null,
    val committeesJson: String? = null,
    val lastUpdated: Long
)
