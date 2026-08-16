package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeyEntity(
    @PrimaryKey val label: String,
    val nextKey: Int?,
    val lastUpdated: Long,
)
