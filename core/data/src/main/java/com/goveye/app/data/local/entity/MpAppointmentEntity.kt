package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import kotlinx.serialization.Serializable

/**
 * A Companies House officer appointment (directorship/secretary/LLP role)
 * held by an MP's matched officer record. Covers current and historical
 * appointments — resignedOn NULL means still serving.
 */
@Serializable
@Entity(
    tableName = "mp_appointments",
    primaryKeys = ["mpId", "companyNumber", "officerRole", "appointedOn"],
    indices = [Index("mpId")]
)
data class MpAppointmentEntity(
    val mpId: Int,
    val companyNumber: String,
    val officerRole: String,
    val appointedOn: String,
    val officerId: String? = null,
    val companyName: String,
    val resignedOn: String? = null,
    val isCurrent: Boolean = true,
    // Company enrichment from /company/{num}
    val companyStatus: String? = null,
    val companyType: String? = null,
    val companySicCodes: String? = null, // JSON array of SIC strings
    val companyIncorporated: String? = null,
    // natures-of-control if the officer is a PSC of this company
    val pscNatures: String? = null, // JSON array
    val lastUpdated: Long
)
