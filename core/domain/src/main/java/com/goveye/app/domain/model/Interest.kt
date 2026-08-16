package com.goveye.app.domain.model

data class Interest(
    val id: Int,
    val memberId: Int,
    val summary: String,
    val categoryName: String,
    val categoryNumber: String,
    val registrationDate: String?,
    val publishedDate: String?,
    val fieldsJson: String,
)
