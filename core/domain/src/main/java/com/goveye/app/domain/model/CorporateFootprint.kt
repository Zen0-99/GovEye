package com.goveye.app.domain.model

/**
 * A Companies House officer appointment (directorship/secretary/LLP
 * role) held by an MP's matched officer record.
 */
data class CompanyAppointment(
    val companyNumber: String,
    val companyName: String,
    val officerRole: String, // raw CH value, e.g. "director", "llp-member"
    val appointedOn: String, // ISO date
    val resignedOn: String?, // null = still serving
    val isCurrent: Boolean,
    val companyStatus: String?, // e.g. "active", "dissolved"
    val companyType: String?, // e.g. "ltd", "llp"
    val sicCodes: List<String> = emptyList(),
    val pscNatures: List<String> = emptyList()
) {
    /** "Director", "LLP Member" — title-cased, "llp" uppercased. */
    val roleDisplay: String
        get() = officerRole.split('-').joinToString(" ") { part ->
            if (part.equals("llp", ignoreCase = true)) {
                "LLP"
            } else {
                part.replaceFirstChar { it.uppercaseChar() }
            }
        }

    /**
     * "director", "LLP member" — sentence-case variant for
     * "Appointed {role} of X" titles. Lowercases per word but keeps
     * all-caps acronyms (never "lLP member" / "llp member").
     */
    val roleDisplaySentence: String
        get() = officerRole.split('-').joinToString(" ") { part ->
            if (part.equals("llp", ignoreCase = true)) {
                "LLP"
            } else {
                part.lowercase()
            }
        }

    /** "Mar 2015" style label for appointedOn. */
    val appointedLabel: String get() = formatIsoMonthYear(appointedOn)

    /** "Jun 2019" style label for resignedOn ("" when null). */
    val resignedLabel: String get() = formatIsoMonthYear(resignedOn)

    /** Tenure span shown on each timeline entry (D-01): "Mar 2015 – Jun 2019" or "Mar 2015 – present". */
    val tenureLabel: String
        get() = buildString {
            append(appointedLabel)
            append(" – ")
            append(if (resignedOn != null) resignedLabel else "present")
        }
}

/**
 * Quiet enrichment facts from an MP's matched Companies House officer
 * identity (D-02). Deliberately excludes officerId, matchMethod,
 * matchConfidence, and dobMonth/dobYear — no provenance UI, and DOB
 * already renders via bio_data + formatDob.
 */
data class OfficerIdentity(val nationality: String?, val countryOfResidence: String?, val isDisqualified: Boolean)

/** Bundle for profile plumbing (mirrors MpElectionResults). */
data class CorporateFootprint(val appointments: List<CompanyAppointment>, val officerIdentity: OfficerIdentity?)

/**
 * "Jul 2024" style label from an ISO date ("2024-07-04T00:00:00" or
 * "2024-07-04"). Duplicate of ElectionResult.kt's private formatter —
 * same deliberate-duplication convention.
 */
private fun formatIsoMonthYear(iso: String?): String {
    if (iso == null) return ""
    val parts = iso.take(10).split("-")
    return if (parts.size == 3) {
        val (y, m, _) = parts
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
