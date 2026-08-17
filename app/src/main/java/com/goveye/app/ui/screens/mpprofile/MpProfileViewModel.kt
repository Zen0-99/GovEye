package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.domain.stats.ActivityScore
import com.goveye.app.domain.stats.ActivityScoreCalculator
import com.goveye.app.domain.stats.PeerAverages
import com.goveye.app.domain.stats.RebellionCalculator
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.TraitBar
import com.goveye.app.domain.stats.TraitBarCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val activityScore: ActivityScore? = null,
    val traitBars: List<TraitBar> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val committeesRepository: CommitteesRepository,
    private val votesRepository: VotesRepository,
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

        // Load voting record + rebellion stats (lazy — loaded with profile)
        viewModelScope.launch {
            try {
                votesRepository.refreshMemberVoting(memberId, 1)
                val votes = votesRepository.getMemberVotingWithDivisions(memberId)
                _uiState.value = _uiState.value.copy(memberVotes = votes)

                // Compute rebellion stats if we have votes and party info
                val mp = _uiState.value.mp
                val partyName = mp?.party?.name
                if (votes.isNotEmpty() && partyName != null) {
                    val memberVotesResult = votesRepository.observeMemberVoting(memberId)
                        .first()
                    val memberVotes = memberVotesResult.data
                    val divisionIds = memberVotes.map { it.divisionId }.distinct()
                    val allVotesByDivision = votesRepository.getAllVotesForDivisions(divisionIds)
                    val stats = RebellionCalculator.compute(memberVotes, allVotesByDivision, partyName)
                    _uiState.value = _uiState.value.copy(rebellionStats = stats)

                    // Compute activity score (using placeholder peer averages for now)
                    val participationRate = if (votes.isNotEmpty()) {
                        votes.count { it.vote != com.goveye.app.domain.model.VoteType.NO_VOTE_RECORDED }.toFloat() / votes.size
                    } else 0f
                    val peerAverages = PeerAverages(
                        averageQuestions = 50f,
                        averageSpeeches = 100f,
                        averageCommittees = 2f,
                    )
                    val score = ActivityScoreCalculator.compute(
                        voteParticipationRate = participationRate,
                        questionCount = 0, // TODO: from Hansard
                        speechCount = 0, // TODO: from Hansard
                        committeeCount = _uiState.value.committees.size,
                        peerAverages = peerAverages,
                    )
                    _uiState.value = _uiState.value.copy(activityScore = score)

                    // Compute trait bars (using placeholder peer data for now)
                    val traits = TraitBarCalculator.compute(
                        rebellionRate = stats.rebellionRate,
                        participationRate = participationRate,
                        questionCount = 0,
                        speechCount = 0,
                        committeeCount = _uiState.value.committees.size,
                        peerRebellionRates = listOf(stats.rebellionRate),
                        peerParticipationRates = listOf(participationRate),
                        peerQuestionCounts = listOf(0),
                        peerSpeechCounts = listOf(0),
                        peerCommitteeCounts = listOf(_uiState.value.committees.size),
                        peerAverages = peerAverages,
                    )
                    _uiState.value = _uiState.value.copy(traitBars = traits)
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
