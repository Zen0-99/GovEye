package com.goveye.app.ui.screens.feed

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.FeedRepository
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

private data class FeedFilterState(val followingOnly: Boolean, val query: String, val house: Int, val limit: Int)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(private val feedRepository: FeedRepository) : ViewModel() {

    private val followingOnlyState = MutableStateFlow(false)
    private val searchQueryState = MutableStateFlow("")
    private val houseFilterState = MutableStateFlow(0)
    private val currentRecess = MutableStateFlow<com.goveye.app.data.local.entity.RecessDateEntity?>(null)

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

    val state: StateFlow<FeedUiState> =
        combine(
            followingOnlyState,
            searchQueryState,
            houseFilterState,
            feedLimit
        ) { followingOnly, query, house, limit ->
            FeedFilterState(followingOnly, query, house, limit)
        }.flatMapLatest { (followingOnly, query, house, limit) ->
            Log.i(
                "GovEye/Feed",
                "flatMapLatest — followingOnly=$followingOnly query='$query' house=$house limit=$limit"
            )
            val feedFlow = if (followingOnly) {
                feedRepository.observeFeedDataFiltered(limit)
            } else {
                feedRepository.observeFeedData(limit)
            }
            combine(feedFlow, currentRecess) { feedData, recess ->
                val processingStart = System.currentTimeMillis()
                Log.i(
                    "GovEye/Feed",
                    "combine emit — divisions=${feedData.divisions.size} followed=${feedData.followedMemberIds.size} recess=$recess isLoading=${feedData.isLoading}"
                )
                // Apply house filter
                val houseFiltered = if (house != 0) {
                    feedData.divisions.filter { it.house == house }
                } else {
                    feedData.divisions
                }
                // Apply search filter
                val searchFiltered = if (query.isNotBlank()) {
                    houseFiltered.filter { it.title.contains(query, ignoreCase = true) }
                } else {
                    houseFiltered
                }
                // Group by date
                val dateGroups = searchFiltered
                    .groupBy { it.date.substring(0, 10) }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (dateKey, divisions) ->
                        FeedDateGroup(
                            dateHeader = DateUtils.formatRelativeDate(dateKey),
                            dateKey = dateKey,
                            divisions = divisions
                        )
                    }
                // Determine empty state — but never show recess empty state
                // when the user is actively searching. A search with no results
                // should show "no results", not the recess banner.
                val isEmpty = searchFiltered.isEmpty() && !feedData.isLoading
                val isRecessEmpty = isEmpty && recess != null && query.isBlank()
                val recentForRecess = if (isRecessEmpty) {
                    feedData.divisions.take(3)
                } else {
                    emptyList()
                }
                // hasMore — whether there are more divisions to load
                val hasMore = feedData.divisions.size >= limit
                val processingTime = System.currentTimeMillis() - processingStart
                Log.i(
                    "GovEye/Feed",
                    "State built — dateGroups=${dateGroups.size} isEmpty=$isEmpty isRecessEmpty=$isRecessEmpty hasMore=$hasMore processingTime=${processingTime}ms"
                )
                FeedUiState(
                    dateGroups = dateGroups,
                    followedMemberIds = feedData.followedMemberIds,
                    divisionsWithFollowedVotes = feedData.divisionsWithFollowedVotes,
                    followingOnly = followingOnly,
                    searchQuery = query,
                    houseFilter = house,
                    currentRecess = recess,
                    isLoading = feedData.isLoading,
                    isEmpty = isEmpty,
                    isRecessEmpty = isRecessEmpty,
                    recentDivisionsForRecess = recentForRecess,
                    hasMore = hasMore
                )
            }
        }.flowOn(Dispatchers.Default)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

    fun setFollowingOnly(value: Boolean) {
        followingOnlyState.value = value
    }

    fun setSearchQuery(query: String) {
        searchQueryState.value = query
    }

    fun setHouseFilter(house: Int) {
        houseFilterState.value = house
    }

    fun clearFilters() {
        followingOnlyState.value = false
        searchQueryState.value = ""
        houseFilterState.value = 0
    }

    /**
     * Load 50 more divisions. Called by the UI when the user scrolls near
     * the bottom of the current list.
     */
    fun loadMore() {
        feedLimit.value += 50
    }
}
