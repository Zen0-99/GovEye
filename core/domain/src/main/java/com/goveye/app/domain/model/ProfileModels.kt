package com.goveye.app.domain.model

data class BiographyExperience(
    val id: Int,
    val type: String?,
    val title: String?,
    val organisation: String?,
    val startMonth: Int?,
    val startYear: Int?,
    val endMonth: Int?,
    val endYear: Int?,
) {
    val dateRangeText: String
        get() = buildString {
            startYear?.let { append(it) }
            if (startMonth != null && startYear != null) {
                append("/").append(startMonth.toString().padStart(2, '0'))
            }
            append(" – ")
            endYear?.let { append(it) } ?: append("present")
            if (endMonth != null && endYear != null) {
                append("/").append(endMonth.toString().padStart(2, '0'))
            }
        }
}

data class BiographyItem(
    val house: String?,
    val name: String?,
    val startDate: String?,
    val endDate: String?,
    val isCurrent: Boolean,
)

data class Committee(
    val id: Int,
    val name: String,
    val house: String?,
    val categoryName: String?,
    val startDate: String?,
    val endDate: String?,
    val isActive: Boolean,
)

data class Contact(
    val type: String?,
    val isPreferred: Boolean?,
    val isWebAddress: Boolean?,
    val line1: String?,
    val line2: String?,
    val line3: String?,
    val line4: String?,
    val line5: String?,
    val postcode: String?,
    val phone: String?,
    val email: String?,
    val website: String?,
    val openingHours: String? = null,
) {
    val formattedAddress: String
        get() = listOf(line1, line2, line3, line4, line5, postcode)
            .filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
}
