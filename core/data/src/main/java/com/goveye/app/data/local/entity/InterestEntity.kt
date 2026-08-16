package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val summary: String,
    val categoryId: Int,
    val categoryNumber: String,
    val categoryName: String,
    val registrationDate: String? = null,
    val publishedDate: String? = null,
    val rectified: Boolean,
    val fieldsJson: String,
    val lastUpdated: Long,
)
