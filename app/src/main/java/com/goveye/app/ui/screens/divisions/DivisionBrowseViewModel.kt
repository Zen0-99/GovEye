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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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

    val state: StateFlow<DivisionBrowseState> =
        combine(
            _searchQuery,
            _houseFilter,
        ) { query, house ->
            query to house
        }.flatMapLatest { (query, house) ->
            val resultFlow = if (query.isNotBlank()) {
                votesRepository.searchDivisions(query, house)
            } else {
                votesRepository.observeDivisionsByHouse(house)
            }
            resultFlow.map { result ->
                DivisionBrowseState(
                    divisions = result.data,
                    isLoading = false,
                    syncStatus = result.status,
                    searchQuery = query,
                    houseFilter = house,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DivisionBrowseState())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setHouseFilter(house: Int) {
        _houseFilter.value = house
    }
}
