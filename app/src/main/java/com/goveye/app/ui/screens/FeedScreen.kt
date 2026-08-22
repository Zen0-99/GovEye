package com.goveye.app.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.components.StickyInfoCard
import com.goveye.app.ui.screens.directory.DirectoryFilterState
import com.goveye.app.ui.screens.directory.FilterBottomSheet
import com.goveye.app.ui.screens.directory.FilterTabType
import com.goveye.app.ui.screens.feed.FeedDateHeader
import com.goveye.app.ui.screens.feed.FeedDivisionCard
import com.goveye.app.ui.screens.feed.FeedNoActivityEmptyState
import com.goveye.app.ui.screens.feed.FeedNoFollowsEmptyState
import com.goveye.app.ui.screens.feed.FeedRecessEmptyState
import com.goveye.app.ui.screens.divisions.TagMicroviewDialog
import com.goveye.app.ui.screens.feed.FeedViewModel

/**
 * Feed tab — chronological divisions feed with sticky date headers,
 * followed-MP highlighting, filter, and recess-aware empty states.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    onNavigateToDivision: (Int, Int) -> Unit,
    showInfoCards: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }

    Log.i(
        "GovEye/Feed",
        "FeedScreen compose — isLoading=${state.isLoading} isEmpty=${state.isEmpty} " +
            "isRecessEmpty=${state.isRecessEmpty} dateGroups=${state.dateGroups.size} " +
            "totalDivisions=${state.totalDivisions}"
    )

    // Stable lambdas — prevents ConfigureSearchBar's DisposableEffect from
    // firing on every recomposition (which would trigger FloatingSearchBar
    // recompositions at the app shell level during navigation transitions).
    val onQueryChange = remember(viewModel) { viewModel::setSearchQuery }
    val onFilterClick = remember { { showFilterSheet = true } }

    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            placeholder = "Search feed…",
            query = state.searchQuery,
            onQueryChange = onQueryChange,
            onFilterClick = onFilterClick,
            hasActiveFilters = state.followingOnly || state.houseFilter != 0
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator()
                }
            }

            state.followingOnly && state.followedMemberIds.isEmpty() -> {
                FeedNoFollowsEmptyState()
            }

            state.isRecessEmpty -> {
                FeedRecessEmptyState(
                    recessEndDate = state.currentRecess?.endDate ?: "",
                    lastDivisions = state.recentDivisionsForRecess,
                    onDivisionClick = onNavigateToDivision
                )
            }

            state.isEmpty && !state.isRecessEmpty -> {
                FeedNoActivityEmptyState()
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Recess info card — always visible when parliament is in
                    // recess, even if there's feed content. Previously this
                    // only showed as an empty state.
                    val recess = state.currentRecess
                    if (recess != null && showInfoCards) {
                        stickyHeader(key = "recess-info") {
                            StickyInfoCard(
                                title = "Parliament is in recess",
                                subtitle = "Returns ${com.goveye.app.domain.util.DateUtils.formatRelativeDate(
                                    recess.endDate
                                )}."
                            )
                        }
                    }

                    state.dateGroups.forEach { group ->
                        stickyHeader(key = "header-${group.dateKey}") {
                            FeedDateHeader(dateHeader = group.dateHeader)
                        }
                        items(group.divisions, key = {
                            "division-${it.id}"
                        }, contentType = { "feed_division" }) { division ->
                            val hasFollowed = division.id in state.divisionsWithFollowedVotes
                            val tags = state.divisionTags[division.id] ?: emptyList()
                            FeedDivisionCard(
                                division = division,
                                hasFollowedVotes = hasFollowed,
                                onClick = { onNavigateToDivision(division.id, division.house) },
                                tags = tags,
                                onTagClick = { tag -> selectedTag = tag }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter bottom sheet — FEED tab type
    if (showFilterSheet) {
        val feedFilterState = DirectoryFilterState(
            houseFilter = state.houseFilter,
            followingOnly = state.followingOnly
        )
        FilterBottomSheet(
            distinctParties = emptyList(),
            filterState = feedFilterState,
            tabType = FilterTabType.FEED,
            viewMode = com.goveye.app.data.preference.DirectoryViewMode.LIST,
            onPartyToggle = {},
            onHouseChange = viewModel::setHouseFilter,
            onCurrentOnlyChange = {},
            onFollowingOnlyChange = viewModel::setFollowingOnly,
            onViewModeChange = {},
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }

    // Tag microview dialog
    selectedTag?.let { tag ->
        TagMicroviewDialog(
            tag = tag,
            onNavigateToDivision = onNavigateToDivision,
            onDismiss = { selectedTag = null }
        )
    }
}
