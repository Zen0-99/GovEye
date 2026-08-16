package com.goveye.app.domain.model

data class Mp(
    val id: Int,
    val nameListAs: String,
    val nameDisplayAs: String,
    val nameFullTitle: String?,
    val gender: String?,
    val party: Party?,
    val constituency: Constituency?,
    val house: Int,
    val membershipStartDate: String?,
    val isActive: Boolean,
    val thumbnailUrl: String?,
)

data class Party(
    val id: Int,
    val name: String,
    val abbreviation: String,
    val backgroundColour: String,
    val foregroundColour: String,
)

data class Constituency(
    val id: Int,
    val name: String,
)
