package com.goveye.app.domain.model

/**
 * Unified career timeline event for an MP.
 *
 * Merges data from the Parliament Biography API and Wikipedia/Wikidata
 * into a single timeline sorted by date.
 */
data class CareerEvent(
    val id: Int,
    val category: CareerCategory,
    val name: String,
    val house: Int? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val additionalInfo: String? = null,
    val additionalInfoLink: String? = null,
    val constituencyName: String? = null,
    val constituencyId: Int? = null,
    val source: String = "parliament"
) {
    val isCurrent: Boolean
        get() = endDate == null

    val formattedDateRange: String
        get() = buildString {
            startDate?.let { append(formatDate(it)) }
            if (endDate != null) {
                append(" → ")
                append(formatDate(endDate))
            } else if (startDate != null) {
                append(" → present")
            }
        }

    val yearForSorting: Int?
        get() = startDate?.take(4)?.toIntOrNull()

    private fun formatDate(iso: String): String {
        // ISO format: 2024-07-04T00:00:00 or 2024-07-04
        val parts = iso.take(10).split("-")
        return if (parts.size == 3) {
            val (y, m, d) = parts
            val monthNames = listOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val monthIdx = m.toIntOrNull()?.minus(1)
            if (monthIdx != null && monthIdx in monthNames.indices) {
                "${monthNames[monthIdx]} $y"
            } else {
                y
            }
        } else {
            iso.take(4)
        }
    }
}

enum class CareerCategory(val apiName: String, val displayName: String) {
    GOVERNMENT_POST("government_post", "Government Post"),
    OPPOSITION_POST("opposition_post", "Opposition Post"),
    OTHER_POST("other_post", "Other Post"),
    COMMITTEE("committee", "Committee"),
    REPRESENTATION("representation", "Elected"),
    PARTY_AFFILIATION("party_affiliation", "Party"),
    HOUSE_MEMBERSHIP("house_membership", "Parliament"),
    EDUCATION("education", "Education"),
    OCCUPATION("occupation", "Career"),
    EXPERIENCE("experience", "Experience"),
    COMPANY_APPOINTMENT("company_appointment", "Company");

    companion object {
        fun fromApiName(name: String): CareerCategory = entries.find { it.apiName == name } ?: EXPERIENCE
    }
}
