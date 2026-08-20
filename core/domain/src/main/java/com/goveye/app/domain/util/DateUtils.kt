package com.goveye.app.domain.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Date formatting utilities for the activity feed.
 *
 * Uses java.time (already used in the codebase — see InterestDateFilterBottomSheet.kt).
 * Pure utility — no DI, no Android dependencies.
 */
object DateUtils {
    private val longDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")

    /**
     * Format an ISO date string as a relative date label:
     * - "Today" if the date is today
     * - "Yesterday" if the date is yesterday
     * - "d MMMM yyyy" (e.g. "20 August 2026") for older dates
     *
     * Handles ISO dates with optional time components (e.g. "2026-08-20T15:30:00").
     * Returns the original string if parsing fails (defensive fallback).
     */
    fun formatRelativeDate(isoDateString: String): String {
        return try {
            val today = LocalDate.now()
            val givenDate = LocalDate.parse(isoDateString.substring(0, 10))
            when (givenDate) {
                today -> "Today"
                today.minusDays(1) -> "Yesterday"
                else -> givenDate.format(longDateFormatter)
            }
        } catch (e: Exception) {
            isoDateString
        }
    }
}
