package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Precomputed peer averages per house — one row per house (1=Commons, 2=Lords).
 *
 * Produced by `build_precompute.py` at build time from the `mp_stats` table.
 * Eliminates 1,950 runtime DAO calls (650 MPs × 3 count queries per house).
 */
@Entity(tableName = "peer_averages")
data class PeerAveragesEntity(
    @PrimaryKey val house: Int,
    val avgQuestions: Float,
    val avgSpeeches: Float,
    val avgCommittees: Float,
    val avgParticipation: Float,
    val avgRebellion: Float,
    val mpCount: Int
)
