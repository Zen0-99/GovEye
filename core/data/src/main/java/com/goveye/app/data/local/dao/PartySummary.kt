package com.goveye.app.data.local.dao

data class PartySummary(
    val partyId: Int,
    val partyName: String,
    val partyAbbreviation: String,
    val partyBackgroundColour: String,
    val partyForegroundColour: String,
    val seats: Int
)
