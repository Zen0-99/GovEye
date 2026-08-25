package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "bill_follows")
data class BillFollowEntity(@PrimaryKey val billId: Int, val followedAt: Long, val isMuted: Boolean = false)
