package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * An Early Day Motion from the oralquestionsandmotions-api
 * `EarlyDayMotions/list` endpoint (this parliament only).
 *
 * One row per EDM (`edmId` = `Id`). `primarySponsorMemberId` is
 * `PrimarySponsor.MnisId` — verified equal to top-level `MemberId` on the
 * live payload; kept nullable defensively for EDMs whose primary sponsor
 * has left the Commons or is absent from the payload.
 *
 * Field types verified against the live API (2026-09-21): `uin` and
 * `siNumber` are JSON numbers; `siYear` is a JSON *string* ("2026");
 * `status` is a JSON number (enum ordinal, e.g. 0 = Open), NOT a string.
 * `uinDisplay` is `UINWithAmendmentSuffix` (e.g. "123A1").
 */
@Serializable
@Entity(
    tableName = "early_day_motions",
    indices = [Index("primarySponsorMemberId")]
)
data class EarlyDayMotionEntity(
    @PrimaryKey val edmId: Int,
    val uin: Int?, // UIN — JSON number (verified)
    val uinDisplay: String?, // UINWithAmendmentSuffix, e.g. "123A1"
    val title: String?,
    val motionText: String?,
    val dateTabled: String?, // ISO datetime
    val statusDate: String?, // ISO datetime
    val status: Int?, // JSON number enum (verified), e.g. 0 = Open
    val primarySponsorMemberId: Int?, // PrimarySponsor.MnisId — joins mps.id
    val sponsorsCount: Int?,
    val amendmentToMotionId: Int?, // links to another early_day_motions row when this is an amendment
    val prayingAgainstSiId: Int?,
    val siNumber: Int?, // StatutoryInstrumentNumber — JSON number (verified)
    val siYear: String?, // StatutoryInstrumentYear — JSON string "2026" (verified)
    val siTitle: String?,
    val lastUpdated: Long
)
