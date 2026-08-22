package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * Tag attached to a bill, derived from the bill title + aggregated tags
 * from all divisions related to that bill. Produced by build_tags.py.
 */
@Entity(
    tableName = "bill_tags",
    primaryKeys = ["billId", "tag"]
)
data class BillTagEntity(
    val billId: Int,
    val tag: String,
    val hitCount: Int
)
