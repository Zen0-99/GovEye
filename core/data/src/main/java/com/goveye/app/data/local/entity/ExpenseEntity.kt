package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mpId: Int,
    val category: String,
    val bucket: String,
    val amountPence: Long,
    val claimDate: String? = null,
    val status: String? = null,
    val lastUpdated: Long
)
