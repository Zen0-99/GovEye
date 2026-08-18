package com.goveye.app.ui.screens.bills

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.BillsRepository
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BillBrowseUiState(
    val bills: List<Bill> = emptyList(),
    val searchQuery: String = "",
    val searchResults: List<Bill> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val isLoading: Boolean = true,
)

@HiltViewModel
class BillBrowseViewModel @Inject constructor(
    private val billsRepository: BillsRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Bill>>(emptyList())

    val state: StateFlow<BillBrowseUiState> =
        combine(
            billsRepository.observeBills(limit = 100),
            _searchQuery,
            _searchResults,
        ) { result, query, searchResults ->
            BillBrowseUiState(
                bills = result.data,
                searchQuery = query,
                searchResults = searchResults,
                syncStatus = result.status,
                isLoading = false,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = BillBrowseUiState(),
        )

    init {
        viewModelScope.launch { billsRepository.refresh() }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            _searchResults.value = if (query.isBlank()) {
                emptyList()
            } else {
                billsRepository.searchBills(query)
            }
        }
    }
}
