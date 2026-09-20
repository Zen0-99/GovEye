package com.goveye.app.ui.utils

import java.time.LocalDate
import java.time.Period
import java.time.YearMonth
import java.time.format.DateTimeFormatter

// dateOfBirth can be a full date ("1964-01-27") or a partial "1964-01"
// (Companies House only exposes month+year). These helpers handle both.

/** Age in years from a full or partial DOB, or null if unparseable. */
fun ageFromDob(dob: String?): Int? {
    if (dob.isNullOrBlank()) return null
    return try {
        when {
            dob.length >= 10 -> Period.between(
                LocalDate.parse(dob.take(10)),
                LocalDate.now()
            ).years

            dob.length >= 7 -> Period.between(
                YearMonth.parse(dob.take(7)).atDay(1),
                LocalDate.now()
            ).years

            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/** "27 Jan 1964" for full dates, "Jan 1964" for partial, null if unparseable. */
fun formatDob(dob: String?): String? {
    if (dob.isNullOrBlank()) return null
    return try {
        when {
            dob.length >= 10 -> LocalDate.parse(dob.take(10))
                .format(DateTimeFormatter.ofPattern("d MMM yyyy"))

            dob.length >= 7 -> YearMonth.parse(dob.take(7))
                .format(DateTimeFormatter.ofPattern("MMM yyyy"))

            else -> null
        }
    } catch (e: Exception) {
        null
    }
}
