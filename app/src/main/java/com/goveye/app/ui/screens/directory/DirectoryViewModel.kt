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
    private val directoryFilterPreferences: DirectoryFilterPreferences
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
        directoryFilterPreferences.includedParties,
        directoryFilterPreferences.excludedParties,
        directoryFilterPreferences.houseFilter,
        directoryFilterPreferences.currentOnly
    ) { included, excluded, house, currentOnly ->
        DirectoryFilterState(included, excluded, house, currentOnly)
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
            results.filter { mp -> filters.matches(mp) }
        }

    // Paged MP list — used when NO filters are active (efficient lazy loading
    // via Room paging source + remote mediator). When filters ARE active, the
    // UI switches to filteredMps (below) because PagingData flows are
    // single-use and cannot be re-collected when filterState changes
    // (crash: "Attempt to collect twice from pageEventFlow").
    val pagedMps: Flow<PagingData<Mp>> = membersRepository.observePagedMps()
        .cachedIn(viewModelScope)

    // Filtered MP list — used when filters are active but no search query.
    // Loads all MPs via observeAllMps() and filters in Kotlin. 650 MPs is
    // small enough to hold in memory. When no filters are active, this
    // emits an empty list and the UI falls back to pagedMps.
    val filteredMps: Flow<List<Mp>> = combine(
        filterState,
        membersRepository.observeAllMps()
    ) { filters, result ->
        if (!filters.hasActiveFilters) emptyList() else result.data.filter { filters.matches(it) }
    }

    // Tab counts — show badges during active search OR when filters are active
    val tabCounts: StateFlow<Map<Int, Int>> =
        combine(_searchQuery, filterState, searchResults) { query, filters, results ->
            if (query.isBlank() && !filters.hasActiveFilters) {
                emptyMap()
            } else {
                mapOf(
                    0 to results.size, // OFFICIALS
                    1 to 0, // PARTIES
                    2 to 0, // BILLS
                    3 to 0, // DIVISIONS
                    4 to 0 // DEBATES
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
    // Party toggle cycles through TriState: DISABLED → INCLUDED → EXCLUDED → DISABLED
    fun togglePartyFilter(party: String) {
        viewModelScope.launch {
            val state = filterState.value.partyState(party)
            val included = filterState.value.includedParties.toMutableSet()
            val excluded = filterState.value.excludedParties.toMutableSet()
            when (state) {
                PartyFilterState.DISABLED -> {
                    included.add(party)
                }

                PartyFilterState.INCLUDED -> {
                    included.remove(party)
                    excluded.add(party)
                }

                PartyFilterState.EXCLUDED -> {
                    excluded.remove(party)
                }
            }
            directoryFilterPreferences.setIncludedParties(included)
            directoryFilterPreferences.setExcludedParties(excluded)
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
