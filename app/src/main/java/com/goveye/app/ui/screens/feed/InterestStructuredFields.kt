package com.goveye.app.ui.screens.feed

/**
 * Build the expandable detail text for income entries from structured fields.
 * Build the expandable detail text for income entries from structured fields.
 *
 * Shows all non-null structured fields as "Label: Value" lines, excluding
 * the primary field already shown in "by X" (donorName) and the description
 * line (paymentDescription / visitPurpose / organisationDescription).
 *
 * Returns empty string if no structured fields are available (caller falls
 * back to the full summary text).
 */
fun formatInterestStructuredDetail(
    donorName: String?,
    paymentType: String?,
    paymentDescription: String?,
    donorStatus: String?,
    donorAddress: String?,
    donorCompanyIdentifier: String?,
    destination: String?,
    visitPurpose: String?,
    organisationName: String?,
    organisationDescription: String?,
    propertyLocation: String?,
    propertyType: String?,
    hoursWorked: String?,
    familyMemberName: String?,
    familyMemberRelationship: String?,
    familyMemberRole: String?
): String {
    val parts = mutableListOf<String>()
    // donorName is shown in "by X" — skip
    paymentType?.let { if (it.isNotBlank()) parts.add("Payment type: $it") }
    // paymentDescription / visitPurpose / organisationDescription shown in description line — skip
    donorStatus?.let { if (it.isNotBlank()) parts.add("Donor status: $it") }
    donorAddress?.let { if (it.isNotBlank()) parts.add("Address: $it") }
    donorCompanyIdentifier?.let { if (it.isNotBlank()) parts.add("Company ID: $it") }
    destination?.let { if (it.isNotBlank()) parts.add("Destination: $it") }
    organisationName?.let { if (it.isNotBlank()) parts.add("Organisation: $it") }
    propertyLocation?.let { if (it.isNotBlank()) parts.add("Location: $it") }
    propertyType?.let { if (it.isNotBlank()) parts.add("Property type: $it") }
    hoursWorked?.let { if (it.isNotBlank()) parts.add("Hours: $it") }
    familyMemberName?.let { if (it.isNotBlank()) parts.add("Name: $it") }
    familyMemberRelationship?.let { if (it.isNotBlank()) parts.add("Relationship: $it") }
    familyMemberRole?.let { if (it.isNotBlank()) parts.add("Role: $it") }
    return parts.joinToString("\n")
}

/**
 * Pick the best description line from structured fields.
 * Priority: paymentDescription > visitPurpose > organisationDescription.
 * Returns null if none available (caller falls back to empty/summary).
 */
fun interestDescriptionLine(
    paymentDescription: String?,
    visitPurpose: String?,
    organisationDescription: String?
): String? = paymentDescription?.takeIf { it.isNotBlank() }
    ?: visitPurpose?.takeIf { it.isNotBlank() }
    ?: organisationDescription?.takeIf { it.isNotBlank() }
