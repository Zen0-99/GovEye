package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Tag attached to an MP, derived from recency-weighted tag hits across
 * their debate speeches. Produced by `build_mp_tags.py` at build time.
 *
 * hitCount is a recency-weighted score (not a raw count) — recent speeches
 * count more than old ones (D-08).
 *
 * Composite key: (memberId, tag).
 */
@Entity(
    tableName = "mp_tags",
    primaryKeys = ["memberId", "tag"]
)
data class MpTagEntity(val memberId: Int, val tag: String, val hitCount: Int)
