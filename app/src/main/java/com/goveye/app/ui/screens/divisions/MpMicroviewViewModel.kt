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
import kotlinx.coroutines.launch

data class MpMicroviewUiState(
    val mp: Mp? = null,
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    val allDivisionDates: List<String> = emptyList(),
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true
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
    private val mpDao: MpDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MpMicroviewUiState())
    val uiState: StateFlow<MpMicroviewUiState> = _uiState.asStateFlow()

    fun load(
        memberId: Int,
        fallbackName: String,
        fallbackPartyName: String?,
        fallbackPartyColour: String?,
        fallbackConstituency: String?
    ) {
        viewModelScope.launch {
            // 1. Try to load from bundled DB first (all 650 MPs are in the DB)
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
                _uiState.value = _uiState.value.copy(mp = mp, isLoading = false)
                loadVotesAndFollow(memberId, mpEntity.house, mpEntity.partyName)
            } else {
                // 2. MP not in bundled DB (e.g., a Lord who voted but isn't in the
                // Commons directory) — show fallback data from the DivisionVote.
                // Lords aren't in the bundled MP directory by design (D-02).
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
                    thumbnailUrl = null
                )
                _uiState.value = _uiState.value.copy(mp = fallbackMp, isLoading = false)
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

                // Load all votes from the bundled DB (the DB has ALL votes for ALL divisions)
                val votes = votesRepository.getMemberVotingWithDivisions(memberId)
                _uiState.value = _uiState.value.copy(
                    memberVotes = votes,
                    allDivisionDates = allDates
                )

                // Compute rebellion stats — fast path using SQL aggregation
                if (votes.isNotEmpty() && partyName != null) {
                    val memberVotes = votesRepository.getMemberVotes(memberId)
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
