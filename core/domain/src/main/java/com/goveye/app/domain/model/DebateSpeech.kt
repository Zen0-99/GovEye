package com.goveye.app.domain.model

/**
 * A single speech within a parliamentary debate transcript.
 *
 * Scraped from TheyWorkForYou at build time. [memberId] is the Parliament
 * member ID matched by name (0 if unmatched — show name without profile link).
 */
data class DebateSpeech(
    val debateGid: String,
    val speechGid: String,
    val divisionId: Int,
    val speakerName: String,
    val memberId: Int,
    val speakerPosition: String,
    val speechText: String,
    val speechOrder: Int,
    val isIntervention: Boolean
)
