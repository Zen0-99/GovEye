package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached recess date from the Egg Timer API.
 *
 * Used by [com.goveye.app.domain.stats.SittingDayResolver] to determine
 * if Parliament is sitting on a given day. Refreshed weekly by the
 * VotePollingWorker (D-01).
 */
@Entity(tableName = "recess_dates")
data class RecessDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val house: Int,
    val description: String,
    val startDate: String, // ISO format: yyyy-MM-dd
    val endDate: String,   // ISO format: yyyy-MM-dd
)

/**
 * Metadata for the recess dates cache — tracks when it was last refreshed.
 */
@Entity(tableName = "recess_dates_meta", primaryKeys = ["house"])
data class RecessDatesMetaEntity(
    val house: Int,
    val lastRefreshedAt: Long,
)
