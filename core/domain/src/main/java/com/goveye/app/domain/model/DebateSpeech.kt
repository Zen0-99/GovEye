package com.goveye.app.domain.model

/**
 * A single speech within a parliamentary debate transcript.
 *
 * Scraped from TheyWorkForYou at build time. [twfyPersonId] is the
 * TheyWorkForYou person ID extracted from the debate page HTML — used
 * as the primary key to resolve speaker info via historical_members.
 *
 * [memberId] is the Parliament member ID (matched at build time via
 * TWFY ID lookup or name matching). Non-zero only for current MPs —
 * used to link to the full MP profile (photo, constituency, etc.).
 */
data class DebateSpeech(
    val debateGid: String,
    val speechGid: String,
    val divisionId: Int,
    val speakerName: String,
    val memberId: Int,
    val twfyPersonId: Int,
    val speakerPosition: String,
    val speechText: String,
    val speechOrder: Int,
    val isIntervention: Boolean
)
