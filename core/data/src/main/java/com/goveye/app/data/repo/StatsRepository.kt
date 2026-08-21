package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.DebateSpeechDao
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.HansardDao
import com.goveye.app.data.local.dao.MpDao
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

@Singleton
class StatsRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val committeeDao: CommitteeDao,
    private val debateSpeechDao: DebateSpeechDao,
    private val hansardDao: HansardDao,
    private val mpDao: MpDao
) {
    companion object {
        private const val TAG = "GovEye/Stats"
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes

        // Approximation: typical rebellion rate across all MPs is ~5%.
        // Used for peer rebellion rates to avoid 650 × full-vote-fetch.
        private const val TYPICAL_REBELLION_RATE = 0.05f
    }

    @Volatile
    private var peerAveragesCache: Pair<PeerAverages, Long>? = null

    @Volatile
    private var peerValuesCache: MutableMap<String, Pair<List<Float>, Long>> = mutableMapOf()

    // --- Per-MP metrics ---

    suspend fun getVoteParticipationRate(memberId: Int, house: Int): Float {
        val votedCount = divisionDao.getDivisionIdsForMember(memberId).size
        val totalDivisions = divisionDao.getAllDivisionsByHouse(house).size
        if (totalDivisions == 0) return 0f
        return votedCount.toFloat() / totalDivisions
    }

    suspend fun getQuestionCount(memberId: Int): Int {
        // No contributionType column — count all hansard contributions as proxy
        return hansardDao.countContributionsForMember(memberId)
    }

    suspend fun getSpeechCount(memberId: Int): Int = debateSpeechDao.countSpeechesForMember(memberId)

    suspend fun getCommitteeCount(memberId: Int): Int = committeeDao.getCommitteesForMember(memberId).size

    suspend fun getRebellionRate(memberId: Int, partyName: String?): Float {
        if (partyName == null) return 0f
        val memberVotes = divisionDao.getVotesForMember(memberId)
        if (memberVotes.isEmpty()) return 0f
        val divisionIds = memberVotes.map { it.divisionId }.distinct()

        // Fast path: single SQL GROUP BY query returns ~200 rows with party
        // aye/no counts per division. Replaces loading 130k+ vote entities.
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

    // --- Peer aggregation (cached, CPU-bound) ---

    suspend fun getPeerAverages(house: Int): PeerAverages = withContext(Dispatchers.Default) {
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
        Log.i(
            TAG,
            "Computed peer averages for ${mps.size} MPs: Q=${averages.averageQuestions} S=${averages.averageSpeeches} C=${averages.averageCommittees}"
        )
        averages
    }

    suspend fun getPeerValues(metric: String, house: Int): List<Float> = withContext(Dispatchers.Default) {
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
            "rebellionRate" -> {
                // Approximation: use typical rebellion rate for all peers
                // to avoid 650 × full-vote-fetch (documented in plan risk mitigation)
                mps.map { TYPICAL_REBELLION_RATE }
            }

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
        Log.i(TAG, "Computed peer values for metric=$metric: ${values.size} MPs")
        values
    }

    // --- Combined computations ---

    suspend fun getActivityScore(memberId: Int, house: Int, partyName: String?): ActivityScore {
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
