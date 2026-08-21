package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Cached recess date from the Egg Timer API.
 *
 * Populated by build_recess.py and bundled into the seed DB. Updated via
 * the recess-latest patch stream (D-09, D-10a).
 */
@Entity(tableName = "recess_dates")
data class RecessDateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val house: Int,
    val description: String,
    // ISO format: yyyy-MM-dd
    val startDate: String,
    // ISO format: yyyy-MM-dd
    val endDate: String
)

/**
 * Metadata for the recess dates cache — tracks when it was last refreshed.
 */
@Entity(tableName = "recess_dates_meta", primaryKeys = ["house"])
data class RecessDatesMetaEntity(val house: Int, val lastRefreshedAt: Long)
