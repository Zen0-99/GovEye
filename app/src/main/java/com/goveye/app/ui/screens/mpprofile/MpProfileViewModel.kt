package com.goveye.app.ui.screens.mpprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

data class MpProfileUiState(
    val mp: Mp? = null,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class MpProfileViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
) : ViewModel() {

    fun observeProfile(memberId: Int): Flow<MpProfileUiState> =
        membersRepository.observeMp(memberId).map { result ->
            MpProfileUiState(
                mp = result.data,
                syncStatus = result.status,
                isLoading = false,
            )
        }

    fun refresh(memberId: Int) {
        viewModelScope.launch { membersRepository.refreshMp(memberId) }
    }
}
