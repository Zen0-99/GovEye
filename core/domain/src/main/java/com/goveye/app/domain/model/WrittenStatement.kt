package com.goveye.app.domain.model

/**
 * @param linkedStatements parsed from linkedStatementsJson; null if no linked statements
 */
data class WrittenStatement(
    val id: Int,
    val memberId: Int,
    val memberRole: String,
    val uin: String,
    val dateMade: String,
    val answeringBodyId: Int,
    val answeringBodyName: String,
    val title: String,
    val text: String,
    val house: Int,
    val url: String? = null,
    val hasLinkedStatements: Boolean = false,
    val linkedStatements: List<LinkedStatement>? = null
)

data class LinkedStatement(val linkedStatementId: Int, val linkType: String, val linkDate: String)
