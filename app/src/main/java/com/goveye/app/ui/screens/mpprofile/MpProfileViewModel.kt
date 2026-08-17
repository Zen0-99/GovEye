package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val mp: Mp? = null,
    val synopsis: String? = null,
    val contacts: List<Contact> = emptyList(),
    val committees: List<Committee> = emptyList(),
    val experiences: List<BiographyExperience> = emptyList(),
    val samePartyMps: List<Mp> = emptyList(),
    val committeePeerMps: List<Mp> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val committeesRepository: CommitteesRepository,
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
