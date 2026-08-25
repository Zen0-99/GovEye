package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Pre-fetched MP synopsis (biography text) from the Members API.
 * Stored in the bundled DB so profile screens don't need a live API call.
 */
@Serializable
@Entity(tableName = "mp_synopsis")
data class MpSynopsisEntity(@PrimaryKey val mpId: Int, val synopsisText: String?, val lastUpdated: Long)
