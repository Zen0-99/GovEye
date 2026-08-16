package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MpProfileUiState(
    val mp: Mp? = null,
    val synopsis: String? = null,
    val contacts: List<Contact> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class MpProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MpProfileUiState())
    val uiState: StateFlow<MpProfileUiState> = _uiState.asStateFlow()

    fun loadProfile(memberId: Int) {
        viewModelScope.launch {
            membersRepository.observeMp(memberId).collect { result ->
                _uiState.value = _uiState.value.copy(
                    mp = result.data,
                    syncStatus = result.status,
                    isLoading = false,
                )
            }
        }

        viewModelScope.launch {
            try {
                val synopsis = membersRepository.getSynopsis(memberId)
                _uiState.value = _uiState.value.copy(synopsis = synopsis)
            } catch (e: Exception) {
                // Synopsis is optional
            }
        }

        viewModelScope.launch {
            try {
                val contacts = membersRepository.getContact(memberId)
                _uiState.value = _uiState.value.copy(contacts = contacts)
            } catch (e: Exception) {
                // Contacts are optional
            }
        }
    }

    fun refresh(memberId: Int) {
        viewModelScope.launch {
            membersRepository.refreshMp(memberId)
        }
    }
}
