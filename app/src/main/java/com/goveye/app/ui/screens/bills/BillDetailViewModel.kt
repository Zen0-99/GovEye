package com.goveye.app.ui.screens.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.BillStage
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class BillDetailUiState(
    val bill: Bill? = null,
    val stages: List<BillStage> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isFollowing: Boolean = false,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BillDetailViewModel @Inject constructor(
    private val billsRepository: BillsRepository,
    private val billFollowRepository: BillFollowRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BillDetailUiState())
    val state: StateFlow<BillDetailUiState> = _state.asStateFlow()

    private var loadedBillId: Int? = null

    fun load(billId: Int) {
        if (loadedBillId == billId) return
        loadedBillId = billId

        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            // Refresh bill detail + stages from API
            billsRepository.refreshBillDetail(billId)
            billsRepository.refreshBillStages(billId)
        }

        viewModelScope.launch {
            combine(
                billsRepository.observeBill(billId),
                billsRepository.observeBillStages(billId),
                billFollowRepository.observeIsFollowing(billId),
            ) { billResult, stages, isFollowing ->
                BillDetailUiState(
                    bill = billResult.data,
                    stages = stages,
                    syncStatus = billResult.status,
                    isFollowing = isFollowing,
                    isLoading = false,
                )
            }.collect { _state.value = it }
        }
    }

    fun toggleFollow(billId: Int) {
        viewModelScope.launch {
            if (_state.value.isFollowing) {
                billFollowRepository.unfollow(billId)
            } else {
                billFollowRepository.follow(billId)
            }
        }
    }
}
