package com.goveye.app.ui.screens.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.repo.FeedRepository
import com.goveye.app.domain.model.Division
import com.goveye.app.domain.util.DateUtils
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FeedViewModel @Inject constructor(private val feedRepository: FeedRepository) : ViewModel() {

    private val followingOnlyState = MutableStateFlow(false)
    private val searchQueryState = MutableStateFlow("")
    private val houseFilterState = MutableStateFlow(0)
    private val currentRecess = MutableStateFlow<com.goveye.app.data.local.entity.RecessDateEntity?>(null)

    init {
        // Fetch recess status on init — re-check would happen if feed data emits empty
        viewModelScope.launch {
            currentRecess.value = feedRepository.getCurrentRecess(1)
        }
    }

    val state: StateFlow<FeedUiState> =
        combine(
            followingOnlyState,
            searchQueryState,
            houseFilterState
        ) { followingOnly, query, house ->
            Triple(followingOnly, query, house)
        }.flatMapLatest { (followingOnly, query, house) ->
            val feedFlow = if (followingOnly) {
                feedRepository.observeFeedDataFiltered()
            } else {
                feedRepository.observeFeedData()
            }
            combine(feedFlow, currentRecess) { feedData, recess ->
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
                // Determine empty state
                val isEmpty = searchFiltered.isEmpty() && !feedData.isLoading
                val isRecessEmpty = isEmpty && recess != null
                val recentForRecess = if (isRecessEmpty) {
                    feedData.divisions.take(3)
                } else {
                    emptyList()
                }
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
                    recentDivisionsForRecess = recentForRecess
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedUiState())

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
}
