package com.goveye.app.domain.stats

import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType

/**
 * Monthly voting data for stacked bar chart.
 */
data class MonthlyVotingData(
    val month: String, // YYYY-MM or YYYY-Q# or YYYY
    val ayeCount: Int,
    val noCount: Int,
    val noVoteCount: Int,
) {
    /** Short label for chart X-axis, e.g. "Jul", "Q3", "2024" */
    val monthLabel: String
        get() {
            // Yearly: YYYY
            if (month.length == 4) return month
            // Quarterly: YYYY-Q#
            if (month.contains("-Q")) {
                val q = month.substringAfter("-Q")
                return "Q$q ${month.take(4)}"
            }
            // Monthly: YYYY-MM
            return try {
                val parts = month.split("-")
                val monthNum = parts[1].toIntOrNull() ?: 1
                listOf("Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec").getOrElse(monthNum - 1) { month }
            } catch (e: Exception) { month }
        }
}

/**
 * Monthly attendance rate for line chart.
 */
data class AttendanceTrend(
    val month: String,
    val attendanceRate: Float, // 0-1
) {
    val monthLabel: String
        get() {
            if (month.length == 4) return month
            if (month.contains("-Q")) {
                val q = month.substringAfter("-Q")
                return "Q$q ${month.take(4)}"
            }
            return try {
                val parts = month.split("-")
                val monthNum = parts[1].toIntOrNull() ?: 1
                listOf("Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec").getOrElse(monthNum - 1) { month }
            } catch (e: Exception) { month }
        }
}

/**
 * Monthly rebellion rate for line chart.
 */
data class RebellionTrend(
    val month: String,
    val rebellionRate: Float, // 0-1
) {
    val monthLabel: String
        get() {
            if (month.length == 4) return month
            if (month.contains("-Q")) {
                val q = month.substringAfter("-Q")
                return "Q$q ${month.take(4)}"
            }
            return try {
                val parts = month.split("-")
                val monthNum = parts[1].toIntOrNull() ?: 1
                listOf("Jan","Feb","Mar","Apr","May","Jun",
                    "Jul","Aug","Sep","Oct","Nov","Dec").getOrElse(monthNum - 1) { month }
            } catch (e: Exception) { month }
        }
}

/**
 * Computes chart data from voting records.
 * Automatically aggregates by month, quarter, or year depending on data span
 * to prevent chart overflow with multi-year data.
 */
object VotingStatsCalculator {
    private const val MAX_POINTS = 12

    private fun periodKey(date: String, totalMonths: Int): String {
        if (totalMonths <= MAX_POINTS) {
            return date.take(7) // YYYY-MM
        }
        // Aggregate by quarter
        val parts = date.take(7).split("-")
        val year = parts.getOrNull(0) ?: date.take(4)
        val monthNum = parts.getOrNull(1)?.toIntOrNull() ?: 1
        val quarter = (monthNum - 1) / 3 + 1
        val quarterKey = "$year-Q$quarter"
        // If even quarters exceed max points, aggregate by year
        val totalQuarters = (totalMonths + 2) / 3
        if (totalQuarters <= MAX_POINTS) return quarterKey
        return year
    }

    private fun countUniqueMonths(votes: List<MemberVoteWithDivision>): Int {
        return votes.mapNotNull { it.divisionDate.take(7).ifBlank { null } }.distinct().size
    }

    /**
     * Group votes by period and count Aye/No/NoVote.
     * Aggregates by month (≤12 months), quarter (13-36 months), or year (>36 months).
     */
    fun computeMonthlyVoting(votes: List<MemberVoteWithDivision>): List<MonthlyVotingData> {
        val totalMonths = countUniqueMonths(votes)
        return votes
            .groupBy { periodKey(it.divisionDate, totalMonths) }
            .map { (period, periodVotes) ->
                MonthlyVotingData(
                    month = period,
                    ayeCount = periodVotes.count { it.vote == VoteType.AYE },
                    noCount = periodVotes.count { it.vote == VoteType.NO },
                    noVoteCount = periodVotes.count { it.vote == VoteType.NO_VOTE_RECORDED },
                )
            }
            .sortedBy { it.month }
    }

    /**
     * Compute attendance rate per period.
     *
     * Attendance = (divisions MP voted in) / (total divisions in period).
     *
     * @param votes The MP's voting record
     * @param allDivisionDates Dates of ALL divisions in the house (not just the
     *   MP's). When provided, attendance is computed against the total number
     *   of divisions per period. When empty, falls back to the MP's own votes
     *   (which always yields 100% since the API only returns divisions where
     *   the MP voted).
     */
    fun computeAttendanceTrend(
        votes: List<MemberVoteWithDivision>,
        allDivisionDates: List<String> = emptyList(),
    ): List<AttendanceTrend> {
        if (allDivisionDates.isEmpty()) {
            // Fallback: old behavior (always 100% since API only returns voted divisions)
            val totalMonths = countUniqueMonths(votes)
            return votes
                .groupBy { periodKey(it.divisionDate, totalMonths) }
                .map { (period, periodVotes) ->
                    val attended = periodVotes.count { it.vote != VoteType.NO_VOTE_RECORDED }
                    AttendanceTrend(
                        month = period,
                        attendanceRate = attended.toFloat() / periodVotes.size,
                    )
                }
                .sortedBy { it.month }
        }

        // Real attendance: MP's voted divisions / total divisions per period.
        // Use the union of all division dates and MP's vote dates to determine
        // the period key consistently.
        val allDates = (allDivisionDates + votes.map { it.divisionDate })
            .filter { it.isNotBlank() }
        val totalMonths = allDates.map { it.take(7) }.distinct().size

        val totalByPeriod = allDivisionDates
            .filter { it.isNotBlank() }
            .groupBy { periodKey(it, totalMonths) }
            .mapValues { it.value.size }

        val attendedByPeriod = votes
            .filter { it.vote != VoteType.NO_VOTE_RECORDED }
            .groupBy { periodKey(it.divisionDate, totalMonths) }
            .mapValues { it.value.size }

        return totalByPeriod.keys.sorted().map { period ->
            val total = totalByPeriod[period] ?: 0
            val attended = attendedByPeriod[period] ?: 0
            AttendanceTrend(
                month = period,
                attendanceRate = if (total > 0) attended.toFloat() / total else 0f,
            )
        }
    }

    /**
     * Compute rebellion rate per period from rebellion instances.
     * @param rebellionInstances The MP's rebellion instances
     * @param allVotes The MP's full voting record (for total per period)
     */
    fun computeRebellionTrend(
        rebellionInstances: List<RebellionInstance>,
        allVotes: List<MemberVoteWithDivision>,
    ): List<RebellionTrend> {
        val totalMonths = countUniqueMonths(allVotes)
        val rebellionByPeriod = rebellionInstances.groupBy { instance ->
            val date = allVotes.find { it.divisionId == instance.divisionId }?.divisionDate ?: ""
            periodKey(date, totalMonths)
        }
        val totalByPeriod = allVotes
            .filter { it.vote != VoteType.NO_VOTE_RECORDED }
            .groupBy { periodKey(it.divisionDate, totalMonths) }

        return totalByPeriod.keys.sorted().map { period ->
            val rebellions = rebellionByPeriod[period]?.size ?: 0
            val total = totalByPeriod[period]?.size ?: 0
            RebellionTrend(
                month = period,
                rebellionRate = if (total > 0) rebellions.toFloat() / total else 0f,
            )
        }
    }
}
