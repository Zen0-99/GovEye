package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "divisions")
data class DivisionEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val date: String,
    val publicationUpdated: String? = null,
    val number: Int? = null,
    val isDeferred: Boolean,
    val ayeCount: Int,
    val noCount: Int,
    val house: Int = 1,
    val lastUpdated: Long
)
