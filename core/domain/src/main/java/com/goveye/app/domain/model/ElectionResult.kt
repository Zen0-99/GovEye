package com.goveye.app.domain.model

/**
 * A single candidate row in a constituency election — one per
 * (constituencyId, electionId, rankOrder); rankOrder 1 is the winner.
 */
data class ElectionCandidate(
    val rankOrder: Int,
    val memberId: Int?, // non-null when the candidate is/was an MP
    val name: String?,
    val partyId: Int?,
    val partyName: String?,
    val partyAbbreviation: String?,
    val partyColour: String?, // hex, no '#', nullable
    val resultChange: String?,
    val votes: Int?
) {
    val isWinner: Boolean get() = rankOrder == 1
}

/**
 * An election result for one constituency (general election or
 * by-election) with its full candidate list.
 */
data class ConstituencyElection(
    val constituencyId: Int,
    val electionId: Int,
    val result: String?,
    val isNotional: Boolean,
    val electorate: Int?,
    val turnout: Int?,
    val majority: Int?,
    val winningPartyId: Int?,
    val winningPartyName: String?,
    val winningPartyColour: String?,
    val electionTitle: String?,
    val electionDate: String?,
    val isGeneralElection: Boolean,
    val constituencyName: String?,
    val candidates: List<ElectionCandidate> = emptyList()
) {
    /** "Jul 2024" style label from the ISO electionDate. */
    val formattedDate: String get() = formatIsoMonthYear(electionDate)

    val isByElection: Boolean get() = !isGeneralElection
}

/** Bundle for the career-tab section (D-05). */
data class MpElectionResults(
    /** Latest recorded election for the MP's current seat — context headline. */
    val currentSeatLatest: ConstituencyElection?,
    /** Every election the MP personally contested (candidate.memberId == mp.id), newest first. */
    val contested: List<ConstituencyElection>
)

/**
 * "Jul 2024" style label from an ISO date ("2024-07-04T00:00:00" or
 * "2024-07-04"). Mirrors [CareerEvent]'s private formatDate — duplicated
 * here rather than extracted so CareerEvent stays untouched.
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
