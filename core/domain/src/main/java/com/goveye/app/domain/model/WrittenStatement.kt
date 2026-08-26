package com.goveye.app.domain.model

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
    val url: String? = null
)
