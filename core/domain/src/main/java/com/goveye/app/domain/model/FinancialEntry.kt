package com.goveye.app.domain.model

/**
 * Unified financial entry — represents either a registered interest (income)
 * or an expense claim. Used by [FinancialBucketDetailScreen] so both types
 * share the same rendering logic.
 *
 * @param entryType INCOME (registered interest) or EXPENSE (IPSA claim)
 * @param id Unique identifier (interest id or expense id)
 * @param summary Display text — interest.summary or expense.shortDescription + details
 * @param categoryName Interest category name or expense category
 * @param categoryNumber Interest category number (null for expenses)
 * @param date Published date (interests) or claim date (expenses)
 * @param amountPence Parsed amount in pence (null if not applicable)
 * @param bucket High-level bucket label
 * @param claimNumber IPSA claim number (expenses only)
 * @param journeyType IPSA journey type (expenses only)
 * @param journeyFrom IPSA journey origin (expenses only)
 * @param journeyTo IPSA journey destination (expenses only)
 * @param travel IPSA travel cost text (expenses only)
 * @param nights IPSA nights count (expenses only)
 * @param mileage IPSA mileage (expenses only)
 * @param amountPaidPence IPSA amount paid (expenses only)
 * @param amountNotPaidPence IPSA amount not paid (expenses only)
 * @param amountRepaidPence IPSA amount repaid (expenses only)
 * @param reasonIfNotPaid IPSA reason for non-payment (expenses only)
 * @param supplyMonth IPSA supply month (expenses only)
 * @param supplyPeriod IPSA supply period (expenses only)
 * @param status IPSA claim status (expenses only)
 */
data class FinancialEntry(
    val entryType: FinancialEntryType,
    val id: Int,
    val summary: String,
    val categoryName: String?,
    val categoryNumber: String? = null,
    val date: String?,
    val amountPence: Long?,
    val bucket: String,
    // Income structured fields (null for expense entries) — Phase 18
    val donorName: String? = null,
    val paymentType: String? = null,
    val paymentDescription: String? = null,
    val donorStatus: String? = null,
    val donorAddress: String? = null,
    val donorCompanyIdentifier: String? = null,
    val destination: String? = null,
    val visitPurpose: String? = null,
    val organisationName: String? = null,
    val organisationDescription: String? = null,
    val propertyLocation: String? = null,
    val propertyType: String? = null,
    val hoursWorked: String? = null,
    val familyMemberName: String? = null,
    val familyMemberRelationship: String? = null,
    val familyMemberRole: String? = null,
    // Expense-specific fields (null for income entries)
    val claimNumber: String? = null,
    val journeyType: String? = null,
    val journeyFrom: String? = null,
    val journeyTo: String? = null,
    val travel: String? = null,
    val nights: String? = null,
    val mileage: String? = null,
    val amountPaidPence: Long? = null,
    val amountNotPaidPence: Long? = null,
    val amountRepaidPence: Long? = null,
    val reasonIfNotPaid: String? = null,
    val supplyMonth: String? = null,
    val supplyPeriod: String? = null,
    val status: String? = null
)

enum class FinancialEntryType { INCOME, EXPENSE }
