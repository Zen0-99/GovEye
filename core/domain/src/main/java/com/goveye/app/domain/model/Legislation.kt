package com.goveye.app.domain.model

data class Legislation(
    val id: Int,
    val title: String,
    val type: String,
    val year: Int,
    val number: Int,
    val date: String,
    val url: String
)
