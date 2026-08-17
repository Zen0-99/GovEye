package com.goveye.app.ui.screens.directory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.goveye.app.data.preference.DirectoryFilterPreferences
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.domain.model.Mp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val directoryPreferences: DirectoryPreferences,
    private val directoryFilterPreferences: DirectoryFilterPreferences,
) : ViewModel() {

    val viewMode: StateFlow<DirectoryViewMode> =
        directoryPreferences.viewMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryViewMode.LIST)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Filter state — combined from DataStore preferences
    val filterState: StateFlow<DirectoryFilterState> = combine(
        directoryFilterPreferences.selectedParties,
        directoryFilterPreferences.houseFilter,
        directoryFilterPreferences.currentOnly,
    ) { parties, house, currentOnly ->
        DirectoryFilterState(parties, house, currentOnly)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryFilterState())

    // Distinct party names for the filter bottom sheet's Party section
    val distinctParties: StateFlow<List<String>> =
        membersRepository.observeDistinctParties()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // FTS search results with filters applied in Kotlin (RESEARCH.md §5.3 Approach B)
    val searchResults: Flow<List<Mp>> = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                flowOf(emptyList())
            } else {
                // Switched from searchMpsViaApi (one-shot suspend) to searchMpsFts (reactive Flow)
                // FTS searches local Room cache across name, party, and constituency
                membersRepository.searchMpsFts(query.trim())
            }
        }
        // Apply filters on top of FTS results
        .combine(filterState) { results, filters ->
            results.filter { mp ->
                (filters.houseFilter == 0 || mp.house == filters.houseFilter) &&
                (filters.currentOnly.not() || mp.isActive) &&
                (filters.selectedParties.isEmpty() || mp.party?.name in filters.selectedParties)
            }
        }

    val pagedMps: Flow<PagingData<Mp>> = membersRepository.observePagedMps()
        .cachedIn(viewModelScope)

    // Tab counts — show badges during active search OR when filters are active
    val tabCounts: StateFlow<Map<Int, Int>> =
        combine(_searchQuery, filterState, searchResults) { query, filters, results ->
            if (query.isBlank() && !filters.hasActiveFilters) {
                emptyMap()
            } else {
                mapOf(
                    0 to results.size,  // OFFICIALS
                    1 to 0,             // PARTIES
                    2 to 0,             // BILLS
                    3 to 0,             // DIVISIONS
                    4 to 0,             // DEBATES
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setViewMode(mode: DirectoryViewMode) {
        viewModelScope.launch { directoryPreferences.setViewMode(mode) }
    }

    // Filter update functions
    fun togglePartyFilter(party: String) {
        viewModelScope.launch {
            val current = filterState.value.selectedParties.toMutableSet()
            if (party in current) current.remove(party) else current.add(party)
            directoryFilterPreferences.setSelectedParties(current)
        }
    }

    fun setHouseFilter(house: Int) {
        viewModelScope.launch { directoryFilterPreferences.setHouseFilter(house) }
    }

    fun setCurrentOnly(currentOnly: Boolean) {
        viewModelScope.launch { directoryFilterPreferences.setCurrentOnly(currentOnly) }
    }

    fun clearFilters() {
        viewModelScope.launch { directoryFilterPreferences.clearAll() }
    }
}
