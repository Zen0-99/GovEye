package com.goveye.app.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.util.DateUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class FeedFilterState(
    val followingOnly: Boolean,
    val query: String,
    val house: Int,
    val limit: Int,
    val tagFilter: Set<String>,
    val sourceFilter: Set<String>,
    val departmentFilter: Set<String>,
    val typeFilter: Set<CardType>
)

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class FilterQuad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(
    private val feedRepository: FeedRepository,
    private val governmentAnnouncementsRepository: GovernmentAnnouncementsRepository
) : ViewModel() {

    private val followingOnlyState = MutableStateFlow(false)
    private val searchQueryState = MutableStateFlow("")
    private val houseFilterState = MutableStateFlow(0)
    private val currentRecess = MutableStateFlow<RecessDateEntity?>(null)

    // New filter states for announcement curation (D-12, D-14)
    private val tagFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val sourceFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val departmentFilterState = MutableStateFlow<Set<String>>(emptySet())
    private val typeFilterState = MutableStateFlow<Set<CardType>>(emptySet())

    // Pagination — start with 50 divisions, increase as user scrolls.
    // Each loadMore() call adds 50 more.
    private val feedLimit = MutableStateFlow(50)

    init {
        Log.i("GovEye/Feed", "FeedViewModel init — fetching recess status")
        viewModelScope.launch {
            currentRecess.value = feedRepository.getCurrentRecess(1)
            Log.i("GovEye/Feed", "Recess status fetched: ${currentRecess.value}")
        }
    }

    @Suppress("UNCHECKED_CAST")
    val state: StateFlow<FeedUiState> =
        combine(
            combine(
                followingOnlyState,
                searchQueryState,
                houseFilterState,
                feedLimit
            ) { followingOnly, query, house, limit ->
                Quad(followingOnly, query, house, limit)
            },
            combine(
                tagFilterState,
                sourceFilterState,
                departmentFilterState,
                typeFilterState
            ) { tags, sources, departments, types ->
                FilterQuad(tags, sources, departments, types)
            }
        ) { core, filters ->
            FeedFilterState(
                followingOnly = core.a,
                query = core.b,
                house = core.c,
                limit = core.d,
                tagFilter = filters.a,
                sourceFilter = filters.b,
                departmentFilter = filters.c,
                typeFilter = filters.d
            )
        }.flatMapLatest { filter ->
            Log.i(
                "GovEye/Feed",
                "flatMapLatest — followingOnly=${filter.followingOnly} query='${filter.query}' " +
                    "house=${filter.house} limit=${filter.limit} tags=${filter.tagFilter} " +
                    "sources=${filter.sourceFilter} depts=${filter.departmentFilter} types=${filter.typeFilter}"
            )
            val feedFlow = if (filter.followingOnly) {
                feedRepository.observeFeedDataFiltered(filter.limit)
            } else {
                feedRepository.observeFeedData(filter.limit)
            }
            // Combine divisions + publications + statements + legislation flows
            val publicationsFlow = governmentAnnouncementsRepository.observePublications(filter.limit)
            val statementsFlow = governmentAnnouncementsRepository.observeStatements(filter.limit)
            val legislationFlow = governmentAnnouncementsRepository.observeLegislation(filter.limit)

            combine(feedFlow, publicationsFlow, statementsFlow, legislationFlow, currentRecess) {
                    feedData,
                    publications,
                    statements,
                    legislation,
                    recess
                ->
                val processingStart = System.currentTimeMillis()

                // Build division items (filter-based, soft — D-12)
                val divisionItems = feedData.divisions
                    .filter { filter.house == 0 || it.house == filter.house }
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .map { division ->
                        FeedItem.DivisionItem(
                            division = division,
                            tags = feedData.divisionTags[division.id] ?: emptyList()
                        )
                    }

                // Build publication items (filter-based, soft — D-12)
                val publicationItems = publications
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .filter { filter.departmentFilter.isEmpty() || it.organisationSlug in filter.departmentFilter }
                    .map { publication ->
                        FeedItem.PublicationItem(
                            publication = publication,
                            tags = emptyList() // Tags loaded per-card from announcementTags map
                        )
                    }

                // Build statement items (filter-based, soft — D-12)
                val statementItems = statements
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .filter { filter.departmentFilter.isEmpty() || it.answeringBodyName in filter.departmentFilter }
                    .map { statement ->
                        FeedItem.StatementItem(
                            statement = statement,
                            tags = emptyList()
                        )
                    }

                // Build legislation items (filter-based, soft — D-12)
                val legislationItems = legislation
                    .filter { filter.query.isBlank() || it.title.contains(filter.query, ignoreCase = true) }
                    .map { legislation ->
                        FeedItem.LegislationItem(
                            legislation = legislation,
                            tags = emptyList()
                        )
                    }

                // Apply type filter
                val allItems = mutableListOf<FeedItem>()
                if (filter.typeFilter.isEmpty() || CardType.DIVISION in filter.typeFilter) {
                    allItems.addAll(divisionItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.PUBLICATION in filter.typeFilter) {
                    allItems.addAll(publicationItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.STATEMENT in filter.typeFilter) {
                    allItems.addAll(statementItems)
                }
                if (filter.typeFilter.isEmpty() || CardType.LEGISLATION in filter.typeFilter) {
                    allItems.addAll(legislationItems)
                }

                // Group by date (substring 0,10 of ISO date)
                val dateGroups = allItems
                    .groupBy { it.date.substring(0, 10) }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (dateKey, items) ->
                        FeedDateGroup(
                            dateHeader = DateUtils.formatRelativeDate(dateKey),
                            dateKey = dateKey,
                            items = items.sortedByDescending { it.date }
                        )
                    }

                // Determine empty state
                val isEmpty = allItems.isEmpty() && !feedData.isLoading
                val isRecessEmpty = isEmpty && recess != null && filter.query.isBlank()
                val recentForRecess = if (isRecessEmpty) {
                    feedData.divisions.take(3)
                } else {
                    emptyList()
                }
                val hasMore = feedData.divisions.size >= filter.limit
                val totalDivisions = divisionItems.size
                val processingTime = System.currentTimeMillis() - processingStart
                Log.i(
                    "GovEye/Feed",
                    "State built — dateGroups=${dateGroups.size} divisions=${divisionItems.size} " +
                        "publications=${publicationItems.size} statements=${statementItems.size} " +
                        "legislation=${legislationItems.size} isEmpty=$isEmpty " +
                        "isRecessEmpty=$isRecessEmpty hasMore=$hasMore processingTime=${processingTime}ms"
                )
                FeedUiState(
                    dateGroups = dateGroups,
                    followedMemberIds = feedData.followedMemberIds,
                    divisionsWithFollowedVotes = feedData.divisionsWithFollowedVotes,
                    divisionTags = feedData.divisionTags,
                    followingOnly = filter.followingOnly,
                    searchQuery = filter.query,
                    houseFilter = filter.house,
                    tagFilter = filter.tagFilter,
                    sourceFilter = filter.sourceFilter,
                    departmentFilter = filter.departmentFilter,
                    typeFilter = filter.typeFilter,
                    currentRecess = recess,
                    isLoading = feedData.isLoading,
                    isEmpty = isEmpty,
                    isRecessEmpty = isRecessEmpty,
                    recentDivisionsForRecess = recentForRecess,
                    hasMore = hasMore,
                    totalDivisions = totalDivisions
                )
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(60_000), FeedUiState())

    fun setFollowingOnly(value: Boolean) {
        followingOnlyState.value = value
    }

    fun setSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun setHouseFilter(house: Int) {
        houseFilterState.value = house
    }

    fun setTagFilter(tags: Set<String>) {
        tagFilterState.value = tags
    }

    fun setSourceFilter(sources: Set<String>) {
        sourceFilterState.value = sources
    }

    fun setDepartmentFilter(departments: Set<String>) {
        departmentFilterState.value = departments
    }

    fun setTypeFilter(types: Set<CardType>) {
        typeFilterState.value = types
    }

    fun clearFilters() {
        followingOnlyState.value = false
        searchQueryState.value = ""
        houseFilterState.value = 0
        tagFilterState.value = emptySet()
        sourceFilterState.value = emptySet()
        departmentFilterState.value = emptySet()
        typeFilterState.value = emptySet()
    }

    /**
     * Load 50 more divisions. Called by the UI when the user scrolls near
     * the bottom of the current list.
     */
    fun loadMore() {
        feedLimit.value += 50
    }
}
