package com.goveye.app.data.repo

object CacheTtl {
    val MPS_MS: Long = 7 * 24 * 60 * 60 * 1000L
    val PARTIES_MS: Long = 30 * 24 * 60 * 60 * 1000L
    val DIVISIONS_MS: Long = 60 * 60 * 1000L
    val DIVISION_VOTES_MS: Long = 60 * 60 * 1000L
    val BILLS_MS: Long = 60 * 60 * 1000L
    val BILL_STAGES_MS: Long = 60 * 60 * 1000L
    val HANSARD_MS: Long = 4 * 60 * 60 * 1000L
    val INTERESTS_MS: Long = 30 * 24 * 60 * 60 * 1000L
}
