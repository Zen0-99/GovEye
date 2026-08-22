package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.MpStatsDao
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.domain.model.VoteType
import com.goveye.app.domain.stats.ActivityScore
import com.goveye.app.domain.stats.ActivityScoreCalculator
import com.goveye.app.domain.stats.PeerAverages
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.TraitBar
import com.goveye.app.domain.stats.TraitBarCalculator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Stats repository with build-time precomputation (Phase 12).
 *
 * Primary path: reads precomputed `mp_stats` and `peer_averages` tables
 * (2 DAO calls for getTraitBars, down from 5,500+).
 *
 * Fallback path: if precomputed tables are empty (old DB without the
 * precompute step), falls back to runtime computation via the same
 * per-MP iteration path used before Phase 12.
 */
@Singleton
class StatsRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val committeeDao: CommitteeDao,
    private val debateSpeechDao: DebateSpeechDao,
    private val hansardDao: HansardDao,
    private val mpDao: MpDao,
    private val mpStatsDao: MpStatsDao
) {
    companion object {
        private const val TAG = "GovEye/Stats"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

        // Approximation: typical rebellion rate across all MPs is ~5%.
        // Used for peer rebellion rates in fallback mode to avoid 650 × full-vote-fetch.
        private const val TYPICAL_REBELLION_RATE = 0.05f
    }

    @Volatile
    private var peerAveragesCache: Pair<PeerAverages, Long>? = null

    @Volatile
    private var peerValuesCache: MutableMap<String, Pair<List<Float>, Long>> = mutableMapOf()

    // Lazy check: are precomputed tables populated?
    @Volatile
    private var precomputeChecked: Boolean = false

    @Volatile
    private var hasPrecomputedStats: Boolean = false

    private suspend fun usePrecomputed(): Boolean {
        if (precomputeChecked) return hasPrecomputedStats
        hasPrecomputedStats = mpStatsDao.countStats() > 0
        precomputeChecked = true
        if (hasPrecomputedStats) {
            Log.i(TAG, "Precomputed stats available — using fast path")
        } else {
            Log.i(TAG, "Precomputed stats empty — falling back to runtime computation")
        }
        return hasPrecomputedStats
    }

    // --- Per-MP metrics ---

    suspend fun getVoteParticipationRate(memberId: Int, house: Int): Float {
        if (usePrecomputed()) {
            return mpStatsDao.getStats(memberId)?.voteParticipationRate ?: 0f
        }
        val votedCount = divisionDao.getDivisionIdsForMember(memberId).size
        val totalDivisions = divisionDao.getAllDivisionsByHouse(house).size
        if (totalDivisions == 0) return 0f
        return votedCount.toFloat() / totalDivisions
    }

    suspend fun getQuestionCount(memberId: Int): Int {
        if (usePrecomputed()) {
            return mpStatsDao.getStats(memberId)?.questionCount ?: 0
        }
        return hansardDao.countContributionsForMember(memberId)
    }

    suspend fun getSpeechCount(memberId: Int): Int {
        if (usePrecomputed()) {
            return mpStatsDao.getStats(memberId)?.speechCount ?: 0
        }
        return debateSpeechDao.countSpeechesForMember(memberId)
    }

    suspend fun getCommitteeCount(memberId: Int): Int {
        if (usePrecomputed()) {
            return mpStatsDao.getStats(memberId)?.committeeCount ?: 0
        }
        return committeeDao.getCommitteesForMember(memberId).size
    }

    suspend fun getRebellionRate(memberId: Int, partyName: String?): Float {
        if (usePrecomputed()) {
            return mpStatsDao.getStats(memberId)?.rebellionRate ?: 0f
        }
        if (partyName == null) return 0f
        val memberVotes = divisionDao.getVotesForMember(memberId)
        if (memberVotes.isEmpty()) return 0f
        val divisionIds = memberVotes.map { it.divisionId }.distinct()

        val partyVoteCounts = divisionDao.getPartyVoteCounts(divisionIds, partyName)
            .associate {
                it.divisionId to com.goveye.app.domain.stats.PartyVoteSummary(
                    divisionId = it.divisionId,
                    partyAyes = it.partyAyes,
                    partyNoes = it.partyNoes
                )
            }

        val domainVotes = memberVotes.map { it.toDomainVote() }
        val stats = RebellionCalculator.computeAggregated(domainVotes, partyVoteCounts)
        return stats.rebellionRate
    }

    // --- Peer aggregation ---

    suspend fun getPeerAverages(house: Int): PeerAverages = withContext(Dispatchers.Default) {
        if (usePrecomputed()) {
            val entity = mpStatsDao.getPeerAverages(house)
            if (entity != null) {
                return@withContext PeerAverages(
                    averageQuestions = entity.avgQuestions,
                    averageSpeeches = entity.avgSpeeches,
                    averageCommittees = entity.avgCommittees,
                    averageParticipation = entity.avgParticipation,
                    averageRebellion = entity.avgRebellion,
                    mpCount = entity.mpCount
                )
            }
        }

        // Fallback: runtime computation (cached)
        val cached = peerAveragesCache
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            return@withContext cached.first
        }

        val mps = getActiveMps(house)
        if (mps.isEmpty()) {
            return@withContext PeerAverages(0f, 0f, 0f)
        }

        var totalQuestions = 0
        var totalSpeeches = 0
        var totalCommittees = 0

        for (mp in mps) {
            totalQuestions += getQuestionCount(mp.id)
            totalSpeeches += getSpeechCount(mp.id)
            totalCommittees += getCommitteeCount(mp.id)
        }

        val averages = PeerAverages(
            averageQuestions = totalQuestions.toFloat() / mps.size,
            averageSpeeches = totalSpeeches.toFloat() / mps.size,
            averageCommittees = totalCommittees.toFloat() / mps.size
        )

        peerAveragesCache = averages to System.currentTimeMillis()
        Log.i(TAG, "Computed peer averages (fallback) for ${mps.size} MPs")
        averages
    }

    suspend fun getPeerValues(metric: String, house: Int): List<Float> = withContext(Dispatchers.Default) {
        if (usePrecomputed()) {
            // Read a single column from mp_stats — one query instead of 650-MP iteration
            val stats = mpStatsDao.getStatsByHouse(house)
            return@withContext when (metric) {
                "rebellionRate" -> stats.map { it.rebellionRate }
                "participationRate" -> stats.map { it.voteParticipationRate }
                "questionCount" -> stats.map { it.questionCount.toFloat() }
                "speechCount" -> stats.map { it.speechCount.toFloat() }
                "committeeCount" -> stats.map { it.committeeCount.toFloat() }
                else -> emptyList()
            }
        }

        // Fallback: runtime computation (cached)
        val cacheKey = "$metric:$house"
        val cached = peerValuesCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.second < CACHE_TTL_MS) {
            return@withContext cached.first
        }

        val mps = getActiveMps(house)
        if (mps.isEmpty()) {
            return@withContext emptyList()
        }

        val values = when (metric) {
            "rebellionRate" -> mps.map { TYPICAL_REBELLION_RATE }

            "participationRate" -> mps.map { mp ->
                getVoteParticipationRate(mp.id, house)
            }

            "questionCount" -> mps.map { mp ->
                getQuestionCount(mp.id).toFloat()
            }

            "speechCount" -> mps.map { mp ->
                getSpeechCount(mp.id).toFloat()
            }

            "committeeCount" -> mps.map { mp ->
                getCommitteeCount(mp.id).toFloat()
            }

            else -> emptyList()
        }

        peerValuesCache[cacheKey] = values to System.currentTimeMillis()
        values
    }

    // --- Combined computations ---

    suspend fun getActivityScore(memberId: Int, house: Int, partyName: String?): ActivityScore {
        if (usePrecomputed()) {
            val stats = mpStatsDao.getStats(memberId)
            if (stats != null) {
                val peerAverages = getPeerAverages(house)
                return ActivityScoreCalculator.compute(
                    stats.voteParticipationRate,
                    stats.questionCount,
                    stats.speechCount,
                    stats.committeeCount,
                    peerAverages
                )
            }
        }

        // Fallback: runtime computation
        val voteParticipationRate = getVoteParticipationRate(memberId, house)
        val questionCount = getQuestionCount(memberId)
        val speechCount = getSpeechCount(memberId)
        val committeeCount = getCommitteeCount(memberId)
        val peerAverages = getPeerAverages(house)
        return ActivityScoreCalculator.compute(
            voteParticipationRate,
            questionCount,
            speechCount,
            committeeCount,
            peerAverages
        )
    }

    suspend fun getTraitBars(memberId: Int, house: Int, partyName: String?): List<TraitBar> {
        if (usePrecomputed()) {
            val stats = mpStatsDao.getStats(memberId)
            if (stats != null) {
                val peerAverages = getPeerAverages(house)
                // Build trait bars from precomputed percentiles — no peer iteration needed
                return listOf(
                    TraitBar(
                        label = "Rebellion",
                        percentile = stats.rebellionPercentile,
                        mpValue = stats.rebellionRate * 100,
                        peerAverage = peerAverages.averageRebellion * 100
                    ),
                    TraitBar(
                        label = "Participation",
                        percentile = stats.participationPercentile,
                        mpValue = stats.voteParticipationRate * 100,
                        peerAverage = peerAverages.averageParticipation * 100
                    ),
                    TraitBar(
                        label = "Questions",
                        percentile = stats.questionsPercentile,
                        mpValue = stats.questionCount.toFloat(),
                        peerAverage = peerAverages.averageQuestions
                    ),
                    TraitBar(
                        label = "Speeches",
                        percentile = stats.speechesPercentile,
                        mpValue = stats.speechCount.toFloat(),
                        peerAverage = peerAverages.averageSpeeches
                    ),
                    TraitBar(
                        label = "Committees",
                        percentile = stats.committeesPercentile,
                        mpValue = stats.committeeCount.toFloat(),
                        peerAverage = peerAverages.averageCommittees
                    )
                )
            }
        }

        // Fallback: full runtime computation
        val rebellionRate = getRebellionRate(memberId, partyName)
        val participationRate = getVoteParticipationRate(memberId, house)
        val questionCount = getQuestionCount(memberId)
        val speechCount = getSpeechCount(memberId)
        val committeeCount = getCommitteeCount(memberId)
        val peerAverages = getPeerAverages(house)

        val peerRebellionRates = getPeerValues("rebellionRate", house)
        val peerParticipationRates = getPeerValues("participationRate", house)
        val peerQuestionCounts = getPeerValues("questionCount", house).map { it.toInt() }
        val peerSpeechCounts = getPeerValues("speechCount", house).map { it.toInt() }
        val peerCommitteeCounts = getPeerValues("committeeCount", house).map { it.toInt() }

        return TraitBarCalculator.compute(
            rebellionRate, participationRate, questionCount, speechCount, committeeCount,
            peerRebellionRates, peerParticipationRates,
            peerQuestionCounts, peerSpeechCounts, peerCommitteeCounts,
            peerAverages
        )
    }

    // --- Helpers ---

    private suspend fun getActiveMps(house: Int): List<MpEntity> = mpDao.observeAllMps().first().filter {
        it.house == house
    }

    private fun DivisionVoteEntity.toDomainVote(): com.goveye.app.domain.model.DivisionVote {
        val voteType = when (vote) {
            "Aye" -> VoteType.AYE
            "No" -> VoteType.NO
            "NoVoteRecorded", "No Vote Recorded" -> VoteType.NO_VOTE_RECORDED
            else -> VoteType.NO_VOTE_RECORDED
        }
        return com.goveye.app.domain.model.DivisionVote(
            divisionId = divisionId,
            memberId = memberId,
            vote = voteType,
            memberName = memberName,
            partyName = partyName,
            partyColour = partyColour,
            constituencyName = constituencyName,
            isTeller = isTeller
        )
    }
}
