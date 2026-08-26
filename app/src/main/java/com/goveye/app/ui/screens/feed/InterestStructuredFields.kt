package com.goveye.app.ui.screens.feed

/**
 * Build the expandable detail fields for income entries from structured fields.
 *
 * Returns a list of [FinancialDetailField] pairs (label, value) for all
 * available structured fields, grouped by category. donorName is excluded
 * (shown in "by X" line on the card).
 *
 * paymentDescription / visitPurpose / organisationDescription ARE included
 * here — the card's description line is truncated to 2 lines, so the full
 * text belongs in the expansion.
 *
 * Fields are grouped: Payment, Donor, Visit, Organisation, Property, Work, Family.
 * The [group] renders as a styled sub-heading (small, semibold, primary color,
 * with a thin divider above) — NOT as plain text.
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
    familyMemberRole: String?,
    descriptionLine: String? = null
): List<FinancialDetailField> {
    val fields = mutableListOf<FinancialDetailField>()
    // donorName is shown in "by X" — skip
    // paymentDescription / visitPurpose / organisationDescription are shown as
    // the card description line — skip whichever one is used to avoid duplication
    // Payment group
    paymentType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Payment type", it, "Payment")) }
    // Visit group — skip visitPurpose if it's the description line
    if (visitPurpose != descriptionLine) {
        visitPurpose?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Purpose", it, "Visit")) }
    }
    destination?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Destination", it, "Visit")) }
    // Donor group
    donorStatus?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Donor status", it, "Donor")) }
    donorAddress?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Address", it, "Donor")) }
    donorCompanyIdentifier?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Company ID", it, "Donor")) }
    // Organisation group — skip organisationDescription if it's the description line
    organisationName?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Organisation", it, "Organisation")) }
    if (organisationDescription != descriptionLine) {
        organisationDescription?.let {
            if (it.isNotBlank()) fields.add(FinancialDetailField("Description", it, "Organisation"))
        }
    }
    // Property group
    propertyLocation?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Location", it, "Property")) }
    propertyType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Property type", it, "Property")) }
    // Work group
    hoursWorked?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Hours", it, "Work")) }
    // Family group
    familyMemberName?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Name", it, "Family")) }
    familyMemberRelationship?.let {
        if (it.isNotBlank()) fields.add(FinancialDetailField("Relationship", it, "Family"))
    }
    familyMemberRole?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Role", it, "Family")) }
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
