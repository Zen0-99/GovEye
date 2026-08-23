package com.goveye.app.ui.screens.committee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Mp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CommitteeUiState(
    val committee: Committee? = null,
    val members: List<Mp> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class CommitteeViewModel @Inject constructor(private val committeesRepository: CommitteesRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CommitteeUiState())
    val uiState = _uiState.asStateFlow()

    fun loadCommittee(committeeId: Int) {
        viewModelScope.launch {
            val committee = committeesRepository.getCommittee(committeeId)
            val members = committeesRepository.getCommitteeMembers(committeeId)
            _uiState.value = CommitteeUiState(
                committee = committee,
                members = members,
                isLoading = false
            )
        }
    }
}
