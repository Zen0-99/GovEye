package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "follows")
data class FollowEntity(
    @PrimaryKey val memberId: Int,
    val followedAt: Long,
    val isMuted: Boolean = false,
)
