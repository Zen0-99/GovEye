package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * A signatory of an Early Day Motion, from the detail endpoint
 * `EarlyDayMotion/{id}` `Sponsors[]` array.
 *
 * One row per (edmId, memberId). `sponsoringOrder` = `SponsoringOrder`
 * (1 = primary sponsor row); `signedAt` = `CreatedWhen` (ISO datetime).
 * Withdrawn signatures are kept as facts (`isWithdrawn` = true,
 * `withdrawnDate` set) rather than deleted — a withdrawal is itself a
 * fact worth storing (D-03).
 */
@Serializable
@Entity(
    tableName = "edm_sponsors",
    primaryKeys = ["edmId", "memberId"],
    indices = [Index("memberId")]
)
data class EdmSponsorEntity(
    val edmId: Int,
    val memberId: Int,
    val sponsoringOrder: Int?, // SponsoringOrder from detail call (1 = primary sponsor row)
    val signedAt: String?, // CreatedWhen, ISO datetime
    val isWithdrawn: Boolean, // keep the flag, don't drop the row
    val withdrawnDate: String?,
    val lastUpdated: Long
)
