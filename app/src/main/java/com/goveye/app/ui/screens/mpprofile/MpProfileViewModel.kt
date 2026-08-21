package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.VotesRepository
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
    val isLoading: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val committeesRepository: CommitteesRepository,
    private val votesRepository: VotesRepository,
    private val followRepository: FollowRepository,
    private val notificationPrefRepository: NotificationPreferenceRepository,
    private val mpDao: MpDao,
    private val interestsRepository: InterestsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(memberId: Int) {
        // Load everything in a single coroutine to avoid staged loading.
        // The bundled DB is local — all queries are fast. Running them
        // sequentially in one coroutine ensures the UI state updates in
        // one batch instead of trickling in over multiple coroutine
        // scheduling cycles.
        viewModelScope.launch {
            // 1. MP basic data (one-shot, not Flow — no need to observe a
            //    bundled DB that doesn't change during a session)
            val mpResult = membersRepository.observeMp(memberId).first()
            val mp = mpResult.data
            _uiState.value = _uiState.value.copy(
                mp = mp,
                syncStatus = mpResult.status,
                isLoading = false
            )

            // 2. Follow state + notification prefs (one-shot)
            val isFollowing = followRepository.observeIsFollowing(memberId).first()
            _uiState.value = _uiState.value.copy(isFollowing = isFollowing)

            val notifPref = notificationPrefRepository.observe(memberId).first()
            _uiState.value = _uiState.value.copy(
                notificationsEnabled = notifPref.notificationsEnabled,
                votesNotificationsEnabled = notifPref.votesEnabled,
                speechesNotificationsEnabled = notifPref.speechesEnabled
            )

            // 3. Synopsis, contacts, experiences (one-shot)
            try {
                val synopsis = membersRepository.getSynopsis(memberId)
                _uiState.value = _uiState.value.copy(synopsis = synopsis)
            } catch (e: Exception) {
            }
            try {
                val contacts = membersRepository.getContact(memberId)
                _uiState.value = _uiState.value.copy(contacts = contacts)
            } catch (e: Exception) {
            }
            try {
                val experiences = membersRepository.getExperience(memberId)
                _uiState.value = _uiState.value.copy(experiences = experiences)
            } catch (e: Exception) {
            }

            // 4. Committees (one-shot)
            val committeesResult = committeesRepository.observeCommitteesForMember(memberId).first()
            _uiState.value = _uiState.value.copy(committees = committeesResult.data)

            // 5. Same-party MPs
            mp?.party?.id?.let { partyId ->
                val entities = mpDao.getMpsByParty(partyId, memberId)
                _uiState.value = _uiState.value.copy(samePartyMps = entities.map { it.toDomainMp() })
            }

            // 6. Votes + rebellion stats — all queries in one batch
            try {
                val house = mp?.house ?: 1
                val allDates = votesRepository.getAllDivisionDates(house)
                val votes = votesRepository.getMemberVotingWithDivisions(memberId)
                android.util.Log.i("GovEye/Profile", "Loaded ${votes.size} votes for MP $memberId (house=$house)")

                val partyName = mp?.party?.name
                val rebellionStats = if (votes.isNotEmpty() && partyName != null) {
                    val memberVotesResult = votesRepository.observeMemberVoting(memberId).first()
                    val memberVotes = memberVotesResult.data
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                    _uiState.value = _uiState.value.copy(allVotesByDivision = allVotesByDivision)
                    RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                } else {
                    null
                }

                _uiState.value = _uiState.value.copy(
                    allDivisionDates = allDates,
                    memberVotes = votes,
                    memberPartyName = partyName,
                    rebellionStats = rebellionStats
                )
            } catch (e: Exception) {
                android.util.Log.e("GovEye/Profile", "Failed to load votes for MP $memberId", e)
            }

            // 7. Interests (one-shot)
            val interestsResult = interestsRepository.observeInterestsForMember(memberId).first()
            android.util.Log.i("GovEye/Profile", "Loaded ${interestsResult.data.size} interests for MP $memberId")
            _uiState.value = _uiState.value.copy(interests = interestsResult.data)
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

    private fun MpEntity.toDomainMp(): Mp = Mp(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = nameFullTitle,
        gender = gender,
        party = com.goveye.app.domain.model.Party(
            partyId,
            partyName,
            partyAbbreviation,
            partyBackgroundColour,
            partyForegroundColour
        ),
        constituency = com.goveye.app.domain.model.Constituency(constituencyId, constituencyName),
        house = house,
        membershipStartDate = membershipStartDate,
        isActive = isActive,
        thumbnailUrl = thumbnailUrl
    )
}
