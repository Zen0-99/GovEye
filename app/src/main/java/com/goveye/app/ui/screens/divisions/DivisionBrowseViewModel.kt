package com.goveye.app.ui.screens.divisions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.model.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DivisionBrowseState(
    val divisions: List<Division> = emptyList(),
    val isLoading: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus.EMPTY,
    val searchQuery: String = "",
    val houseFilter: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DivisionBrowseViewModel @Inject constructor(
    private val votesRepository: VotesRepository,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _houseFilter = MutableStateFlow(0)
    private val _isRefreshing = MutableStateFlow(false)

    val state: StateFlow<DivisionBrowseState> =
        combine(
            _searchQuery,
            _houseFilter,
            _isRefreshing,
        ) { query, house, refreshing ->
            Triple(query, house, refreshing)
        }.flatMapLatest { (query, house, refreshing) ->
            val resultFlow = if (query.isNotBlank()) {
                votesRepository.searchDivisions(query, house)
            } else {
                votesRepository.observeDivisionsByHouse(house)
            }
            resultFlow.combine(flowOf(refreshing)) { result, isRefreshing ->
                DivisionBrowseState(
                    divisions = result.data,
                    isLoading = isRefreshing,
                    syncStatus = result.status,
                    searchQuery = query,
                    houseFilter = house,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DivisionBrowseState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHouseFilter(house: Int) {
        _houseFilter.value = house
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                votesRepository.refresh()
                votesRepository.refreshLords()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    init {
        refresh()
    }
}
