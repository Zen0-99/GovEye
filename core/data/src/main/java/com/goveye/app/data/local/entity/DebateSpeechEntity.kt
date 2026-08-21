package com.goveye.app.data.local.entity

import androidx.room.Entity

/**
 * A single speech within a parliamentary debate, scraped from TheyWorkForYou
 * at build time. Each debate (linked to one or more divisions via the
 * divisions.twfyDebateUrl column) contains multiple speeches, interventions,
 * and procedural statements.
 *
 * Speaker matching: [memberId] is the Parliament member ID matched by name
 * at build time (against the MPs table's nameDisplayAs). 0 if unmatched —
 * the app shows the name without a profile link in that case.
 *
 * [twfyPersonId] is the TheyWorkForYou person ID (from the debate page HTML),
 * kept for debugging/future matching but not used at runtime.
 */
@Entity(tableName = "debate_speeches", primaryKeys = ["debateGid", "speechGid"])
data class DebateSpeechEntity(
    val debateGid: String,
    val speechGid: String,
    val divisionId: Int,
    val speakerName: String,
    val memberId: Int = 0,
    val twfyPersonId: Int = 0,
    val speakerPosition: String = "",
    val speechText: String,
    val speechOrder: Int,
    val isIntervention: Boolean = false,
    val lastUpdated: Long
)
