package com.goveye.app.ui.screens.feed

/**
 * Build the expandable detail fields for income entries from structured fields.
 *
 * Returns a list of [FinancialDetailField] pairs (label, value) for the
 * SECONDARY structured fields — excludes donorName (shown in "by X" line)
 * and paymentDescription / visitPurpose / organisationDescription (shown in
 * the card's description line, truncated to 2 lines).
 *
 * No group sub-headings — field labels are self-explanatory.
 *
 * Returns empty list if no structured fields are available (caller falls
 * back to the full summary text via expandableContent).
 */
fun formatInterestStructuredFields(
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
): List<FinancialDetailField> {
    val fields = mutableListOf<FinancialDetailField>()
    // donorName is shown in "by X" — skip
    // paymentDescription / visitPurpose / organisationDescription are shown in
    // the card's description line (truncated to 2 lines) — skip to avoid duplication
    paymentType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Payment type", it)) }
    donorStatus?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Donor status", it)) }
    donorAddress?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Address", it)) }
    donorCompanyIdentifier?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Company ID", it)) }
    destination?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Destination", it)) }
    organisationName?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Organisation", it)) }
    propertyLocation?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Location", it)) }
    propertyType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Property type", it)) }
    hoursWorked?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Hours", it)) }
    familyMemberName?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Name", it)) }
    familyMemberRelationship?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Relationship", it)) }
    familyMemberRole?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Role", it)) }
    return fields
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
