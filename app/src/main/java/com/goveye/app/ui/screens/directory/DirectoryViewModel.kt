package com.goveye.app.ui.screens.directory

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.CommitteeSummary
import com.goveye.app.data.local.dao.CouncilDao
import com.goveye.app.data.local.dao.CouncilSummary
import com.goveye.app.data.preference.DirectoryFilterPreferences
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.PostcodeRepository
import com.goveye.app.data.util.PostcodeDetector
import com.goveye.app.domain.model.Mp
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for postcode-based search.
 * When the user types a postcode, we call postcodes.io to resolve it.
 */
sealed class PostcodeSearchState {
    data object Idle : PostcodeSearchState()
    data object Loading : PostcodeSearchState()
    data object NotFound : PostcodeSearchState()
    data class Found(val result: com.goveye.app.data.repo.PostcodeLookupResult) : PostcodeSearchState()
}

/**
 * Holder for the 4 government announcement filter values (D-14),
 * used to combine them into [DirectoryFilterState] in a single step.
 */
private data class GovFilterExtras(
    val tags: Set<String>,
    val sources: Set<String>,
    val departments: Set<String>,
    val type: Int
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val membersRepository: MembersRepository,
    private val directoryPreferences: DirectoryPreferences,
    private val directoryFilterPreferences: DirectoryFilterPreferences,
    private val postcodeRepository: PostcodeRepository,
    private val councilDao: CouncilDao,
    private val committeeDao: CommitteeDao,
    private val governmentAnnouncementsRepository: com.goveye.app.data.repo.GovernmentAnnouncementsRepository
) : ViewModel() {

    val viewMode: StateFlow<DirectoryViewMode> =
        directoryPreferences.viewMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryViewMode.LIST)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Postcode search state — when a postcode is detected, we call
    // postcodes.io to resolve it to a constituency, then find the MP(s)
    private val _postcodeResult = MutableStateFlow<PostcodeSearchState>(PostcodeSearchState.Idle)
    val postcodeResult: StateFlow<PostcodeSearchState> = _postcodeResult.asStateFlow()

    // Filter state — combined from DataStore preferences
    val filterState: StateFlow<DirectoryFilterState> = combine(
        directoryFilterPreferences.includedParties,
        directoryFilterPreferences.excludedParties,
        directoryFilterPreferences.houseFilter,
        directoryFilterPreferences.currentOnly
    ) { included, excluded, house, currentOnly ->
        DirectoryFilterState(included, excluded, house, currentOnly)
    }.combine(
        combine(
            directoryFilterPreferences.tagFilter,
            directoryFilterPreferences.sourceFilter,
            directoryFilterPreferences.departmentFilter,
            directoryFilterPreferences.typeFilter
        ) { tags, sources, departments, type ->
            GovFilterExtras(tags, sources, departments, type)
        }
    ) { base, extras ->
        base.copy(
            tagFilter = extras.tags,
            sourceFilter = extras.sources,
            departmentFilter = extras.departments,
            typeFilter = extras.type
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DirectoryFilterState())

    // Distinct party names for the filter bottom sheet's Party section
    val distinctParties: StateFlow<List<String>> =
        membersRepository.observeDistinctParties()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Active parties with seat counts — for the Parties tab.
    // Uses SharingStarted.Eagerly so the one-shot query runs once and the
    // result is cached for the ViewModel's lifetime — avoids re-querying
    // the database every time DirectoryScreen enters composition.
    val parties: StateFlow<List<com.goveye.app.data.local.dao.PartySummary>> =
        flow {
            try {
                emit(membersRepository.getActiveParties())
            } catch (e: Exception) {
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // All councils — for the Councils tab.
    // Uses Eagerly so the one-shot query runs once and is cached.
    val councils: StateFlow<List<CouncilSummary>> =
        flow {
            try {
                emit(councilDao.getAllCouncils())
            } catch (e: Exception) {
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // All committees with member counts — for the Committees tab.
    // Uses Eagerly so the one-shot query runs once and is cached.
    val committees: StateFlow<List<CommitteeSummary>> =
        flow {
            try {
                emit(committeeDao.getAllCommittees())
            } catch (e: Exception) {
                emit(emptyList())
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Government tab data ---

    private val _governmentSourceType = MutableStateFlow(GovernmentSourceType.ALL)
    val governmentSourceType: StateFlow<GovernmentSourceType> = _governmentSourceType.asStateFlow()

    val governmentPublications: StateFlow<List<com.goveye.app.domain.model.GovernmentPublication>> =
        governmentAnnouncementsRepository.observePublications(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val governmentStatements: StateFlow<List<com.goveye.app.domain.model.WrittenStatement>> =
        governmentAnnouncementsRepository.observeStatements(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val governmentLegislation: StateFlow<List<com.goveye.app.domain.model.Legislation>> =
        governmentAnnouncementsRepository.observeLegislation(100)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val governmentLoading: StateFlow<Boolean> =
        combine(governmentPublications, governmentStatements, governmentLegislation) { pubs, stmts, leg ->
            // Loading is true until at least one emission has been received from all three flows.
            // Since StateFlow starts with emptyList (initial value), we consider it loaded when
            // the flows have emitted at least once. We use a simple heuristic: if all three are
            // empty, it could be loading or genuinely empty. We set loading=false after first emit.
            false
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setGovernmentSourceType(type: GovernmentSourceType) {
        _governmentSourceType.value = type
    }

    // --- Government filter data (D-14) ---

    /** All distinct announcement tags for the FilterBottomSheet Tags section. */
    val allAnnouncementTags: StateFlow<List<String>> =
        governmentAnnouncementsRepository.observeAllAnnouncementTags()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All distinct department (organisation) names from publications. */
    val allDepartments: StateFlow<List<String>> =
        governmentPublications
            .map { pubs -> pubs.map { it.organisation }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** All source recommendation names (department-stream pairs) for the FilterBottomSheet Sources section. */
    val allSources: StateFlow<List<String>> =
        governmentAnnouncementsRepository.observeAllRecommendations()
            .map { recs -> recs.map { it.organisationName }.distinct().sorted() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // FTS search results with filters applied in Kotlin (RESEARCH.md §5.3 Approach B)
    // Skip debounce for empty queries — avoids a 300ms delayed no-op emission
    // on every DirectoryScreen composition. Only debounce actual search input.
    // Postcode detection: if the query looks like a UK postcode, route to
    // postcodes.io API instead of FTS search.
    val searchResults: StateFlow<List<Mp>> = _searchQuery
        .debounce { query -> if (query.isBlank()) 0 else 300 }
        .flatMapLatest { query ->
            Log.i("GovEye/Directory", "searchResults flatMapLatest — query='$query'")
            if (query.isBlank()) {
                _postcodeResult.value = PostcodeSearchState.Idle
                flowOf(emptyList())
            } else if (PostcodeDetector.isPostcode(query.trim())) {
                // Postcode search — call postcodes.io, then find MP by constituency
                _postcodeResult.value = PostcodeSearchState.Loading
                flow {
                    val lookup = postcodeRepository.lookupPostcode(query.trim())
                    if (lookup == null || lookup.constituencyName == null) {
                        _postcodeResult.value = PostcodeSearchState.NotFound
                        emit(emptyList())
                    } else {
                        _postcodeResult.value = PostcodeSearchState.Found(lookup)
                        // Search for MPs by constituency name
                        val constituency = lookup.constituencyName
                        val mps = membersRepository.searchMpsByConstituency(constituency!!)
                        emit(mps)
                    }
                }
            } else {
                _postcodeResult.value = PostcodeSearchState.Idle
                membersRepository.searchAllMembersFts(query.trim())
            }
        }
        .combine(filterState) { results, filters ->
            Log.i(
                "GovEye/Directory",
                "searchResults combine — results=${results.size} hasFilters=${filters.hasActiveFilters}"
            )
            results.filter { mp -> filters.matches(mp) }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Paged MP list — used when NO filters are active (efficient lazy loading
    // via Room paging source + remote mediator). When filters ARE active, the
    // UI switches to filteredMps (below) because PagingData flows are
    // single-use and cannot be re-collected when filterState changes
    // (crash: "Attempt to collect twice from pageEventFlow").
    val pagedMps: Flow<PagingData<Mp>> = membersRepository.observePagedMps()
        .cachedIn(viewModelScope)

    // Filtered MP list — used when filters are active but no search query.
    // Uses flatMapLatest so observeAllMps() is only called when filters are
    // actually active — avoids loading all 650 MPs on every DirectoryScreen
    // composition when no filters are needed.
    // flowOn(Dispatchers.Default) moves the 650-MP filter off the main thread.
    val filteredMps: StateFlow<List<Mp>> = filterState
        .flatMapLatest { filters ->
            Log.i("GovEye/Directory", "filteredMps flatMapLatest — hasFilters=${filters.hasActiveFilters}")
            if (!filters.hasActiveFilters) {
                flowOf(emptyList())
            } else {
                membersRepository.observeAllMps().map { result ->
                    result.data.filter { filters.matches(it) }
                }
            }
        }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    // --- Government filter operations (D-14) ---

    fun toggleTagFilter(tag: String) {
        viewModelScope.launch {
            val current = filterState.value.tagFilter.toMutableSet()
            if (tag in current) current.remove(tag) else current.add(tag)
            directoryFilterPreferences.setTagFilter(current)
        }
    }

    fun toggleSourceFilter(source: String) {
        viewModelScope.launch {
            val current = filterState.value.sourceFilter.toMutableSet()
            if (source in current) current.remove(source) else current.add(source)
            directoryFilterPreferences.setSourceFilter(current)
        }
    }

    fun toggleDepartmentFilter(department: String) {
        viewModelScope.launch {
            val current = filterState.value.departmentFilter.toMutableSet()
            if (department in current) current.remove(department) else current.add(department)
            directoryFilterPreferences.setDepartmentFilter(current)
        }
    }

    fun setTypeFilter(type: Int) {
        viewModelScope.launch { directoryFilterPreferences.setTypeFilter(type) }
    }

    fun clearFilters() {
        viewModelScope.launch { directoryFilterPreferences.clearAll() }
    }
}
