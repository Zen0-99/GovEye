package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.RebellionStats
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ProfileUiState(
    val mp: Mp? = null,
    val synopsis: String? = null,
    val contacts: List<Contact> = emptyList(),
    val committees: List<Committee> = emptyList(),
    val experiences: List<BiographyExperience> = emptyList(),
    val samePartyMps: List<Mp> = emptyList(),
    val committeePeerMps: List<Mp> = emptyList(),
    val memberVotes: List<MemberVoteWithDivision> = emptyList(),
    val rebellionStats: RebellionStats? = null,
    val allDivisionDates: List<String> = emptyList(),
    val isFollowing: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val votesNotificationsEnabled: Boolean = false,
    val speechesNotificationsEnabled: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val committeesRepository: CommitteesRepository,
    private val votesRepository: VotesRepository,
    private val followRepository: FollowRepository,
    private val notificationPrefRepository: NotificationPreferenceRepository,
    private val mpDao: MpDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(memberId: Int) {
        viewModelScope.launch {
            membersRepository.observeMp(memberId).collect { result ->
                _uiState.value = _uiState.value.copy(
                    mp = result.data,
                    syncStatus = result.status,
                    isLoading = false,
                )
                result.data?.let { mp ->
                    loadSamePartyMps(memberId, mp.party?.id)
                }
            }
        }

        // Observe follow state
        viewModelScope.launch {
            followRepository.observeIsFollowing(memberId).collect { isFollowing ->
                _uiState.value = _uiState.value.copy(isFollowing = isFollowing)
            }
        }

        // Observe per-MP notification preferences
        viewModelScope.launch {
            notificationPrefRepository.observe(memberId).collect { pref ->
                _uiState.value = _uiState.value.copy(
                    notificationsEnabled = pref.notificationsEnabled,
                    votesNotificationsEnabled = pref.votesEnabled,
                    speechesNotificationsEnabled = pref.speechesEnabled,
                )
            }
        }

        viewModelScope.launch {
            try {
                val synopsis = membersRepository.getSynopsis(memberId)
                _uiState.value = _uiState.value.copy(synopsis = synopsis)
            } catch (e: Exception) {
            }
        }

        viewModelScope.launch {
            try {
                val contacts = membersRepository.getContact(memberId)
                _uiState.value = _uiState.value.copy(contacts = contacts)
            } catch (e: Exception) {
            }
        }

        viewModelScope.launch {
            try {
                val experiences = membersRepository.getExperience(memberId)
                _uiState.value = _uiState.value.copy(experiences = experiences)
            } catch (e: Exception) {
            }
        }

        viewModelScope.launch {
            committeesRepository.observeCommitteesForMember(memberId).collect { result ->
                _uiState.value = _uiState.value.copy(committees = result.data)
            }
        }

        viewModelScope.launch {
            committeesRepository.refresh(memberId)
        }

        // Load voting record — show cached data first, then refresh in background
        viewModelScope.launch {
            try {
                // 0. Load all division dates for the house (for attendance calc)
                val mp = mpDao.getMp(memberId)
                val house = mp?.house ?: 1
                val allDates = votesRepository.getAllDivisionDates(house)
                _uiState.value = _uiState.value.copy(allDivisionDates = allDates)

                // 1. Show cached votes immediately (from previous fetches)
                val cachedVotes = votesRepository.getMemberVotingWithDivisions(memberId)
                if (cachedVotes.isNotEmpty()) {
                    _uiState.value = _uiState.value.copy(memberVotes = cachedVotes)
                    // Compute rebellion stats from cached data right away
                    val partyName = _uiState.value.mp?.party?.name
                    if (partyName != null) {
                        val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                        val memberVotes = memberVotesResult.data
                        val divisionIds = memberVotes.map { it.divisionId }.distinct()
                        val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                        val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                        _uiState.value = _uiState.value.copy(rebellionStats = stats)
                    }
                }

                // 2. Refresh from API in background (upserts per-page, so DB updates progressively)
                votesRepository.refreshMemberVoting(memberId, house)

                // 3. Batch-fetch full division details (all votes) for the
                // MP's divisions so rebellion can be computed against actual
                // party majorities. Without this, only the MP's own vote is
                // stored per division, making rebellion always 0%.
                val freshVotes = votesRepository.getMemberVotingWithDivisions(memberId)
                _uiState.value = _uiState.value.copy(memberVotes = freshVotes)
                val partyName = _uiState.value.mp?.party?.name
                if (freshVotes.isNotEmpty() && partyName != null) {
                    val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                    val memberVotes = memberVotesResult.data
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    // Fetch full voter lists for up to 100 most recent divisions
                    votesRepository.batchFetchDivisionDetails(divisionIds, house, limit = 100)
                    // Recompute with the now-populated voter lists
                    val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                    val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                    _uiState.value = _uiState.value.copy(rebellionStats = stats)
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun loadSamePartyMps(memberId: Int, partyId: Int?) {
        if (partyId == null) return
        viewModelScope.launch {
            val entities = mpDao.getMpsByParty(partyId, memberId)
            val mps = entities.map { it.toDomainMp() }
            _uiState.value = _uiState.value.copy(samePartyMps = mps)
        }
    }

    fun refresh(memberId: Int) {
        viewModelScope.launch {
            membersRepository.refreshMp(memberId)
            committeesRepository.refresh(memberId)
        }
    }

    fun toggleFollow(memberId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                followRepository.unfollow(memberId)
            } else {
                followRepository.follow(memberId)
            }
        }
    }

    // --- Notification preferences ---

    fun setNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setNotificationsEnabled(memberId, enabled)
        }
    }

    fun setVotesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setVotesEnabled(memberId, enabled)
        }
    }

    fun setSpeechesNotificationsEnabled(memberId: Int, enabled: Boolean) {
        viewModelScope.launch {
            notificationPrefRepository.setSpeechesEnabled(memberId, enabled)
        }
    }

    private fun MpEntity.toDomainMp(): Mp =
        Mp(
            id = id,
            nameListAs = nameListAs,
            nameDisplayAs = nameDisplayAs,
            nameFullTitle = nameFullTitle,
            gender = gender,
            party = com.goveye.app.domain.model.Party(
                partyId, partyName, partyAbbreviation,
                partyBackgroundColour, partyForegroundColour,
            ),
            constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
            house = house,
            membershipStartDate = membershipStartDate,
            isActive = isActive,
            thumbnailUrl = thumbnailUrl,
        )
}
