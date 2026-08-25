package com.goveye.app.ui.screens.feed

/**
 * Build the expandable detail fields for income entries from structured fields.
 *
 * Returns a list of [FinancialDetailField] pairs (label, value) for the
 * secondary structured fields — excludes donorName (shown in "by X") and
 * the description line (paymentDescription / visitPurpose / organisationDescription).
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
    paymentType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Payment type", it, "Payment")) }
    // paymentDescription / visitPurpose / organisationDescription shown in description line — skip
    if (donorStatus?.isNotBlank() == true || donorAddress?.isNotBlank() == true ||
        donorCompanyIdentifier?.isNotBlank() == true
    ) {
        donorStatus?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Donor status", it, "Donor")) }
        donorAddress?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Address", it, "Donor")) }
        donorCompanyIdentifier?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Company ID", it, "Donor")) }
    }
    destination?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Destination", it, "Visit")) }
    if (organisationName?.isNotBlank() == true) {
        organisationName?.let {
            if (it.isNotBlank()) fields.add(FinancialDetailField("Organisation", it, "Organisation"))
        }
    }
    if (propertyLocation?.isNotBlank() == true || propertyType?.isNotBlank() == true) {
        propertyLocation?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Location", it, "Property")) }
        propertyType?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Property type", it, "Property")) }
    }
    hoursWorked?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Hours", it, "Work")) }
    if (familyMemberName?.isNotBlank() == true || familyMemberRelationship?.isNotBlank() == true ||
        familyMemberRole?.isNotBlank() == true
    ) {
        familyMemberName?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Name", it, "Family")) }
        familyMemberRelationship?.let {
            if (it.isNotBlank()) fields.add(FinancialDetailField("Relationship", it, "Family"))
        }
        familyMemberRole?.let { if (it.isNotBlank()) fields.add(FinancialDetailField("Role", it, "Family")) }
    }
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
