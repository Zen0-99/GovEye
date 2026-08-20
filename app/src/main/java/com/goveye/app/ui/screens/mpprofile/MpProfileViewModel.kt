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
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.Interest
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
    val allVotesByDivision: Map<Int, List<DivisionVote>> = emptyMap(),
    val memberPartyName: String? = null,
    val interests: List<Interest> = emptyList(),
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
    private val interestsRepository: InterestsRepository,
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

        // Load voting record from the bundled DB (the DB has ALL votes for ALL divisions)
        viewModelScope.launch {
            try {
                // 0. Load all division dates for the house (for attendance calc)
                val mp = mpDao.getMp(memberId)
                val house = mp?.house ?: 1
                val allDates = votesRepository.getAllDivisionDates(house)
                _uiState.value = _uiState.value.copy(allDivisionDates = allDates)

                // 1. Load all votes from the bundled DB
                val votes = votesRepository.getMemberVotingWithDivisions(memberId)
                _uiState.value = _uiState.value.copy(memberVotes = votes)

                // 2. Compute rebellion stats from the bundled data
                val partyName = _uiState.value.mp?.party?.name
                _uiState.value = _uiState.value.copy(memberPartyName = partyName)
                if (votes.isNotEmpty() && partyName != null) {
                    val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                    val memberVotes = memberVotesResult.data
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                    _uiState.value = _uiState.value.copy(allVotesByDivision = allVotesByDivision)
                    val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                    _uiState.value = _uiState.value.copy(rebellionStats = stats)
                }
            } catch (e: Exception) {
            }
        }

        // Load interests from the bundled DB
        viewModelScope.launch {
            interestsRepository.observeInterestsForMember(memberId).collect { result ->
                _uiState.value = _uiState.value.copy(interests = result.data)
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

    /**
     * No-op — the bundled DB is the source of truth, updated via patches.
     * Kept for pull-to-refresh UI compatibility; the observe flows re-emit
     * on their own when the DB changes.
     */
    fun refresh(memberId: Int) {
        // No-op — DB is pre-populated and updated via patches (D-09, D-10a)
    }

    fun toggleFollow(memberId: Int) {
        viewModelScope.launch {
            if (_uiState.value.isFollowing) {
                followRepository.unfollow(memberId)
            } else {
                followRepository.follow(memberId)
                // Auto-enable vote notifications when following (FotMob behavior)
                notificationPrefRepository.setVotesEnabled(memberId, true)
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
