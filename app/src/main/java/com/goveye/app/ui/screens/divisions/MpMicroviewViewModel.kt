package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.VotesRepository
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
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
)

/**
 * Lightweight ViewModel for the MP microview dialog.
 *
 * Unlike [ProfileViewModel], this doesn't depend on the MP being in the
 * local database. It receives the basic info (name, party, etc.) from the
 * DivisionVote and loads the voting record + follow state in the background.
 *
 * If the MP is in the local DB, it uses that data (which has thumbnailUrl,
 * party colors, etc.). If not, it fetches from the Members API.
 */
@HiltViewModel
class MpMicroviewViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val votesRepository: VotesRepository,
    private val followRepository: FollowRepository,
    private val mpDao: MpDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MpMicroviewUiState())
    val uiState: StateFlow<MpMicroviewUiState> = _uiState.asStateFlow()

    fun load(memberId: Int, fallbackName: String, fallbackPartyName: String?, fallbackPartyColour: String?, fallbackConstituency: String?) {
        viewModelScope.launch {
            // 1. Try to load from local DB first (fast path)
            val mpEntity = mpDao.getMp(memberId)
            if (mpEntity != null) {
                val mp = Mp(
                    id = mpEntity.id,
                    nameListAs = mpEntity.nameListAs,
                    nameDisplayAs = mpEntity.nameDisplayAs,
                    nameFullTitle = mpEntity.nameFullTitle,
                    gender = mpEntity.gender,
                    party = com.goveye.app.domain.model.Party(
                        mpEntity.partyId, mpEntity.partyName, mpEntity.partyAbbreviation,
                        mpEntity.partyBackgroundColour, mpEntity.partyForegroundColour,
                    ),
                    constituency = com.goveye.app.domain.model.Constituency(mpEntity.constituencyId, mpEntity.constituencyName),
                    house = mpEntity.house,
                    membershipStartDate = mpEntity.membershipStartDate,
                    isActive = mpEntity.isActive,
                    thumbnailUrl = mpEntity.thumbnailUrl,
                )
                _uiState.value = _uiState.value.copy(mp = mp, isLoading = false)
                loadVotesAndFollow(memberId, mpEntity.house, mpEntity.partyName)
            } else {
                // 2. MP not in DB — show fallback data immediately, fetch from API
                val fallbackMp = Mp(
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
                    thumbnailUrl = null,
                )
                _uiState.value = _uiState.value.copy(mp = fallbackMp, isLoading = false)

                // 3. Fetch from Members API in background
                try {
                    membersRepository.refreshMp(memberId)
                    val refreshed = mpDao.getMp(memberId)
                    if (refreshed != null) {
                        val mp = Mp(
                            id = refreshed.id,
                            nameListAs = refreshed.nameListAs,
                            nameDisplayAs = refreshed.nameDisplayAs,
                            nameFullTitle = refreshed.nameFullTitle,
                            gender = refreshed.gender,
                            party = com.goveye.app.domain.model.Party(
                                refreshed.partyId, refreshed.partyName, refreshed.partyAbbreviation,
                                refreshed.partyBackgroundColour, refreshed.partyForegroundColour,
                            ),
                            constituency = com.goveye.app.domain.model.Constituency(refreshed.constituencyId, refreshed.constituencyName),
                            house = refreshed.house,
                            membershipStartDate = refreshed.membershipStartDate,
                            isActive = refreshed.isActive,
                            thumbnailUrl = refreshed.thumbnailUrl,
                        )
                        _uiState.value = _uiState.value.copy(mp = mp)
                        loadVotesAndFollow(memberId, refreshed.house, refreshed.partyName)
                        return@launch
                    }
                } catch (e: Exception) {
                    // Keep fallback data
                }
                // Still load votes with fallback house + party
                loadVotesAndFollow(memberId, 1, fallbackPartyName)
            }
        }
    }

    private fun loadVotesAndFollow(memberId: Int, house: Int, partyName: String?) {
        viewModelScope.launch {
            // Check follow state
            val isFollowing = followRepository.isFollowing(memberId)
            _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
        }

        viewModelScope.launch {
            try {
                // Load all division dates for the house (for attendance calc)
                val allDates = votesRepository.getAllDivisionDates(house)

                // 1. Show cached votes immediately
                val cachedVotes = votesRepository.getMemberVotingWithDivisions(memberId)
                if (cachedVotes.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        memberVotes = cachedVotes,
                        allDivisionDates = allDates,
                    )
                    // Compute rebellion stats from cached data
                    if (partyName != null) {
                        val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                        val memberVotes = memberVotesResult.data
                        val divisionIds = memberVotes.map { it.divisionId }.distinct()
                        val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                        val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                        _uiState.value = _uiState.value.copy(rebellionStats = stats)
                    }
                }

                // 2. Refresh from API in background
                votesRepository.refreshMemberVoting(memberId, house)

                // 3. Batch-fetch full division details for rebellion calc
                val freshVotes = votesRepository.getMemberVotingWithDivisions(memberId)
                val freshDates = votesRepository.getAllDivisionDates(house)
                _uiState.value = _uiState.value.copy(
                    memberVotes = freshVotes,
                    allDivisionDates = freshDates,
                )
                if (freshVotes.isNotEmpty() && partyName != null) {
                    val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                    val memberVotes = memberVotesResult.data
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    votesRepository.batchFetchDivisionDetails(divisionIds, house, limit = 100)
                    val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                    val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                    _uiState.value = _uiState.value.copy(rebellionStats = stats)
                }
            } catch (e: Exception) {
                // Keep cached data
            }
        }
    }

    fun toggleFollow(memberId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                followRepository.unfollow(memberId)
            } else {
                followRepository.follow(memberId)
            }
            val isFollowing = followRepository.isFollowing(memberId)
            _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
        }
    }
}
