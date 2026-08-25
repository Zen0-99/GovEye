package com.goveye.app.domain.model

/**
 * Unified activity entry — represents one item in the mixed chronological
 * activity feed on the MP profile Activity tab.
 *
 * Each [ActivityEntryType] has a distinct card layout (D-01). Type-specific
 * fields are nullable and only populated for the relevant type.
 *
 * @param entryType The activity type (VOTE, QUESTION, INCOME, EXPENSE, COMMITTEE, CAREER)
 * @param id Unique key: type prefix + numeric id (e.g. "vote_123", "question_456")
 * @param date ISO date string (YYYY-MM-DD) used for sorting and date grouping
 * @param summary Primary display text — division title, question excerpt, etc.
 * @param divisionTitle Full division title (VOTE only)
 * @param divisionId Parliament division ID (VOTE only) — used for navigation
 * @param voteResult "Aye", "No", or "—" (VOTE only)
 * @param house 1 (Commons) or 2 (Lords) (VOTE only)
 * @param questionText Full question text (QUESTION only)
 * @param answeringBodyName Government department name (QUESTION only)
 * @param uin Unique Identification Number (QUESTION only) — used for TheyWorkForYou link
 * @param categoryName Interest category name (INCOME only)
 * @param amountPence Parsed amount in pence (INCOME only)
 * @param bucket High-level bucket label (INCOME only)
 * @param bucketLabel Expense bucket label, e.g. "Travel" (EXPENSE only)
 * @param totalAmountPence Aggregated monthly total in pence (EXPENSE only)
 * @param claimCount Number of claims in the monthly bucket (EXPENSE only)
 * @param committeeName Committee name (COMMITTEE only)
 * @param isJoin true = joined, false = left (COMMITTEE only)
 * @param roleTitle Role title, e.g. "Secretary of State for Health" (CAREER only)
 * @param contextLine Context line, e.g. department name (CAREER only)
 * @param milestoneType "POST", "MAIDEN_SPEECH", or "HONOUR" (CAREER only)
 * @param speechText Full speech text (SPEECH only)
 * @param speechTags Tags inherited from the parent division (SPEECH only)
 */
data class ActivityEntry(
    val entryType: ActivityEntryType,
    val id: String,
    val date: String,
    val summary: String,
    // Vote-specific (D-02 — simplified: no weight score, no rebellion indicator)
    val divisionTitle: String? = null,
    val divisionId: Int? = null,
    val voteResult: String? = null,
    val house: Int? = null,
    // Question-specific (D-03)
    val questionText: String? = null,
    val answeringBodyName: String? = null,
    val uin: String? = null,
    // Income-specific (D-10 — all interests as events on registration date)
    val categoryName: String? = null,
    val amountPence: Long? = null,
    val bucket: String? = null,
    // Income structured fields (Phase 18) — null for non-income entries
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
    // Expense-specific (D-11 — monthly bucket totals with claim count)
    val bucketLabel: String? = null,
    val totalAmountPence: Long? = null,
    val claimCount: Int? = null,
    // Committee-specific
    val committeeName: String? = null,
    val isJoin: Boolean? = null,
    // Career-specific (D-04 — role title + context line from MNIS bio_data)
    val roleTitle: String? = null,
    val contextLine: String? = null,
    val milestoneType: String? = null,
    // Speech-specific (17-07 — debate speeches in activity tab)
    val speechText: String? = null,
    val speechTags: List<String>? = null
)

enum class ActivityEntryType { VOTE, QUESTION, INCOME, EXPENSE, COMMITTEE, CAREER, SPEECH }
