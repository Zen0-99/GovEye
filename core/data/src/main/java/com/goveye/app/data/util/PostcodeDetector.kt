package com.goveye.app.data.util

/**
 * UK postcode validation and detection.
 *
 * UK postcode format: area + district + sector + unit
 * Examples: SW1A 1AA, M1 1AE, WN3 4HA, EC1A 1BB
 *
 * The regex matches both full postcodes (e.g., "WN3 4HA") and
 * partial postcodes (e.g., "WN3", "M1") for渐进式 search.
 */
object PostcodeDetector {
    // UK postcode regex — matches full and partial postcodes
    // Format: area (1-2 letters) + district (1-2 digits, optional letter) + space + sector (1 digit) + unit (2 letters)
    private val POSTCODE_REGEX = Regex(
        "^[A-Z]{1,2}[0-9][A-Z0-9]?\\s?[0-9][A-Z]{2}$",
        RegexOption.IGNORE_CASE
    )

    // Partial postcode (outcode only) — e.g., "WN3", "M1", "SW1A"
    private val OUTCODE_REGEX = Regex(
        "^[A-Z]{1,2}[0-9][A-Z0-9]?$",
        RegexOption.IGNORE_CASE
    )

    // Loose detection — anything that looks like it could be a postcode
    // (starts with 1-2 letters followed by a digit)
    private val POSTCODE_LIKE_REGEX = Regex(
        "^[A-Z]{1,2}[0-9]",
        RegexOption.IGNORE_CASE
    )

    /**
     * Check if the query looks like a UK postcode.
     * Returns true for full postcodes, outcodes, and partial inputs
     * that start with a postcode pattern.
     */
    fun isPostcode(query: String): Boolean {
        val trimmed = query.trim()
        if (trimmed.length < 2) return false
        // Check if it starts with a postcode-like pattern
        // and doesn't contain typical name characters (like multiple words)
        return POSTCODE_LIKE_REGEX.containsMatchIn(trimmed) &&
            !trimmed.contains(",") &&
            trimmed.split(" ").size <= 2
    }

    /**
     * Check if the query is a full, valid UK postcode (with or without space).
     */
    fun isFullPostcode(query: String): Boolean {
        val trimmed = query.trim()
        return POSTCODE_REGEX.matches(trimmed)
    }

    /**
     * Check if the query is a valid outcode (partial postcode).
     */
    fun isOutcode(query: String): Boolean {
        val trimmed = query.trim()
        return OUTCODE_REGEX.matches(trimmed)
    }

    /**
     * Normalize a postcode for API calls — remove spaces, uppercase.
     */
    fun normalize(postcode: String): String = postcode.replace(" ", "").uppercase()
}
