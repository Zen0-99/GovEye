package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.HistoricalMemberRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.RebellionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class MpMicroviewUiState(
    val mp: Mp? = null,
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    val allDivisionDates: List<String> = emptyList(),
    val interests: List<Interest> = emptyList(),
    val expenseBucketTotals: List<ExpenseBucketTotal> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true
)

/**
 * Lightweight ViewModel for the MP microview dialog.
 *
 * Uses the centralized ID resolution via [historicalMemberRepository]:
 * 1. Try `mps` table (current Commons MPs) — ID = Parliament API member ID
 * 2. Try `historical_members` table (Lords, former MPs) — lookup by parliamentMemberId
 *    - If house=2 (Lords), votes are stored with memberId + LORDS_ID_OFFSET (1,000,000)
 *    - If house=1 (former Commons MP), votes use the memberId directly
 * 3. Fall back to the DivisionVote data passed by the caller
 */
@HiltViewModel
class MpMicroviewViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val votesRepository: VotesRepository,
    private val followRepository: FollowRepository,
    private val mpDao: MpDao,
    private val historicalMemberRepository: HistoricalMemberRepository,
    private val interestsRepository: InterestsRepository,
    private val expensesRepository: ExpensesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MpMicroviewUiState())
    val uiState: StateFlow<MpMicroviewUiState> = _uiState.asStateFlow()

    companion object {
        /** Lords votes are stored with memberId + this offset (see build_lords_votes.py) */
        private const val LORDS_ID_OFFSET = 1_000_000
    }

    fun load(
        memberId: Int,
        fallbackName: String,
        fallbackPartyName: String?,
        fallbackPartyColour: String?,
        fallbackConstituency: String?
    ) {
        // Optimistic header: build a temporary Mp from fallback data so the
        // header (gradient, avatar, name, party) renders instantly before the
        // DB lookup completes. Same pattern as MpProfileScreen.
        val optimisticMp = Mp(
            id = memberId,
            nameListAs = fallbackName,
            nameDisplayAs = fallbackName,
            nameFullTitle = null,
            gender = null,
            party = fallbackPartyName?.let {
                com.goveye.app.domain.model.Party(0, it, "", fallbackPartyColour ?: "", "")
            },
            constituency = fallbackConstituency?.let {
                com.goveye.app.domain.model.Constituency(0, it)
            },
            house = 1,
            membershipStartDate = null,
            isActive = true,
            thumbnailUrl = null
        )
        _uiState.value = _uiState.value.copy(mp = optimisticMp, isLoading = false)

        viewModelScope.launch {
            // 1. Try to load from bundled DB first (all 650 Commons MPs are in the DB)
            val mpEntity = mpDao.getMp(memberId)
            if (mpEntity != null) {
                val mp = Mp(
                    id = mpEntity.id,
                    nameListAs = mpEntity.nameListAs,
                    nameDisplayAs = mpEntity.nameDisplayAs,
                    nameFullTitle = mpEntity.nameFullTitle,
                    gender = mpEntity.gender,
                    party = com.goveye.app.domain.model.Party(
                        mpEntity.partyId,
                        mpEntity.partyName,
                        mpEntity.partyAbbreviation,
                        mpEntity.partyBackgroundColour,
                        mpEntity.partyForegroundColour
                    ),
                    constituency = com.goveye.app.domain.model.Constituency(
                        mpEntity.constituencyId,
                        mpEntity.constituencyName
                    ),
                    house = mpEntity.house,
                    membershipStartDate = mpEntity.membershipStartDate,
                    isActive = mpEntity.isActive,
                    thumbnailUrl = mpEntity.thumbnailUrl
                )
                _uiState.value = _uiState.value.copy(mp = mp)
                loadVotesAndFollow(memberId, mpEntity.house, mpEntity.partyName)
                loadFinances(memberId)
            } else {
                // 2. Not in Commons DB — look up in historical_members (Lords, former MPs)
                val historicalMember = historicalMemberRepository.getByParliamentMemberId(memberId)
                if (historicalMember != null) {
                    val house = historicalMember.house
                    // For Lords (house=2), votes are stored with memberId + LORDS_ID_OFFSET
                    val votesMemberId = if (house == 2) memberId + LORDS_ID_OFFSET else memberId
                    val mp = Mp(
                        id = memberId,
                        nameListAs = historicalMember.displayName,
                        nameDisplayAs = historicalMember.displayName,
                        nameFullTitle = null,
                        gender = null,
                        party = (historicalMember.party ?: fallbackPartyName)?.let {
                            com.goveye.app.domain.model.Party(0, it, "", fallbackPartyColour ?: "", "")
                        },
                        constituency = (historicalMember.constituency ?: fallbackConstituency)?.let {
                            com.goveye.app.domain.model.Constituency(0, it)
                        },
                        house = house,
                        membershipStartDate = historicalMember.startDate,
                        isActive = historicalMember.isCurrent == 1,
                        thumbnailUrl = null
                    )
                    _uiState.value = _uiState.value.copy(mp = mp)
                    loadVotesAndFollow(votesMemberId, house, historicalMember.party ?: fallbackPartyName)
                    loadFinances(memberId)
                } else {
                    // 3. Fallback — keep the optimistic Mp, load votes with the raw memberId
                    loadVotesAndFollow(memberId, 1, fallbackPartyName)
                    loadFinances(memberId)
                }
            }
        }
    }

    private fun loadVotesAndFollow(votesMemberId: Int, house: Int, partyName: String?) {
        viewModelScope.launch {
            // Check follow state — use the canonical memberId (without Lords offset)
            val followId = if (votesMemberId >= LORDS_ID_OFFSET) votesMemberId - LORDS_ID_OFFSET else votesMemberId
            val isFollowing = followRepository.isFollowing(followId)
            _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
        }

        viewModelScope.launch {
            try {
                // Load all division dates for the house (for attendance calc)
                val allDates = votesRepository.getAllDivisionDates(house)

                // Load all votes using the correct memberId (with Lords offset if applicable)
                val votes = votesRepository.getMemberVotingWithDivisions(votesMemberId)
                _uiState.value = _uiState.value.copy(
                    memberVotes = votes,
                    allDivisionDates = allDates
                )

                // Compute rebellion stats — fast path using SQL aggregation
                if (votes.isNotEmpty() && partyName != null) {
                    val memberVotes = votesRepository.getMemberVotes(votesMemberId)
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    val partyVoteCounts = votesRepository.getPartyVoteCounts(divisionIds, partyName)
                    val stats = RebellionCalculator.computeAggregated(memberVotes, partyVoteCounts)
                    _uiState.value = _uiState.value.copy(rebellionStats = stats)
                }
            } catch (e: Exception) {
                // Keep cached data
            }
        }
    }

    private fun loadFinances(memberId: Int) {
        viewModelScope.launch {
            try {
                val interests = interestsRepository.observeInterestsForMember(memberId).first()
                _uiState.value = _uiState.value.copy(interests = interests.data)
            } catch (e: Exception) {
                // Keep cached data
            }
        }
        viewModelScope.launch {
            try {
                val bucketTotals = expensesRepository.getBucketTotals(memberId)
                _uiState.value = _uiState.value.copy(expenseBucketTotals = bucketTotals)
            } catch (e: Exception) {
                // Keep cached data
            }
        }
    }

    fun toggleFollow(memberId: Int) {
        // Optimistic UI: flip the follow icon immediately so the user sees
        // the change without waiting for the repository round-trip (issue #7).
        val wasFollowing = _uiState.value.isFollowing
        _uiState.value = _uiState.value.copy(isFollowing = !wasFollowing)
        viewModelScope.launch {
            try {
                if (wasFollowing) {
                    followRepository.unfollow(memberId)
                } else {
                    followRepository.follow(memberId)
                }
            } catch (e: Exception) {
                // Revert on failure
                _uiState.value = _uiState.value.copy(isFollowing = wasFollowing)
            }
        }
    }
}
