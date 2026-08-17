package com.goveye.app.domain.stats

import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.VoteType

/**
 * Monthly voting data for stacked bar chart.
 */
data class MonthlyVotingData(
    val month: String, // YYYY-MM
    val ayeCount: Int,
    val noCount: Int,
    val noVoteCount: Int,
)

/**
 * Monthly attendance rate for line chart.
 */
data class AttendanceTrend(
    val month: String,
    val attendanceRate: Float, // 0-1
)

/**
 * Monthly rebellion rate for line chart.
 */
data class RebellionTrend(
    val month: String,
    val rebellionRate: Float, // 0-1
)

/**
 * Computes chart data from voting records.
 */
object VotingStatsCalculator {
    /**
     * Group votes by month and count Aye/No/NoVote.
     */
    fun computeMonthlyVoting(votes: List<MemberVoteWithDivision>): List<MonthlyVotingData> {
        return votes
            .groupBy { it.divisionDate.take(7) } // YYYY-MM
            .map { (month, monthVotes) ->
                MonthlyVotingData(
                    month = month,
                    ayeCount = monthVotes.count { it.vote == VoteType.AYE },
                    noCount = monthVotes.count { it.vote == VoteType.NO },
                    noVoteCount = monthVotes.count { it.vote == VoteType.NO_VOTE_RECORDED },
                )
            }
            .sortedBy { it.month }
    }

    /**
     * Compute monthly attendance rate.
     * Attendance = (Ayes + Noes) / total votes in month.
     */
    fun computeAttendanceTrend(votes: List<MemberVoteWithDivision>): List<AttendanceTrend> {
        return votes
            .groupBy { it.divisionDate.take(7) }
            .map { (month, monthVotes) ->
                val attended = monthVotes.count { it.vote != VoteType.NO_VOTE_RECORDED }
                AttendanceTrend(
                    month = month,
                    attendanceRate = attended.toFloat() / monthVotes.size,
                )
            }
            .sortedBy { it.month }
    }

    /**
     * Compute monthly rebellion rate from rebellion instances.
     * @param rebellionInstances The MP's rebellion instances
     * @param allVotes The MP's full voting record (for total per month)
     */
    fun computeRebellionTrend(
        rebellionInstances: List<RebellionInstance>,
        allVotes: List<MemberVoteWithDivision>,
    ): List<RebellionTrend> {
        val rebellionByMonth = rebellionInstances.groupBy { instance ->
            allVotes.find { it.divisionId == instance.divisionId }?.divisionDate?.take(7) ?: ""
        }
        val totalByMonth = allVotes
            .filter { it.vote != VoteType.NO_VOTE_RECORDED }
            .groupBy { it.divisionDate.take(7) }

        return totalByMonth.keys.sorted().map { month ->
            val rebellions = rebellionByMonth[month]?.size ?: 0
            val total = totalByMonth[month]?.size ?: 0
            RebellionTrend(
                month = month,
                rebellionRate = if (total > 0) rebellions.toFloat() / total else 0f,
            )
        }
    }
}
