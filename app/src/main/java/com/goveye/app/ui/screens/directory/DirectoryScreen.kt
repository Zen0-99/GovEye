package com.goveye.app.ui.screens.directory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.ui.components.FloatingSearchBar
import com.goveye.app.ui.components.TabTextWithBadge
import com.goveye.app.ui.screens.bills.BillsTabContent
import com.goveye.app.ui.screens.divisions.DivisionsTabContent
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.launch

private enum class DirectoryTab(val title: String) {
    OFFICIALS("Officials"),
    PARTIES("Parties"),
    BILLS("Bills"),
    DIVISIONS("Divisions"),
    DEBATES("Debates")
}

@Composable
fun DirectoryScreen(
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToBill: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: DirectoryViewModel = hiltViewModel()
) {
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.pagedMps.collectAsLazyPagingItems()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle(emptyList())
    val filteredMps by viewModel.filteredMps.collectAsStateWithLifecycle(emptyList())
    val tabCounts by viewModel.tabCounts.collectAsStateWithLifecycle(emptyMap())
    val filterState by viewModel.filterState.collectAsStateWithLifecycle(DirectoryFilterState())
    val distinctParties by viewModel.distinctParties.collectAsStateWithLifecycle(emptyList())
    var showFilterSheet by remember { mutableStateOf(false) }

    // Save tab index across navigation — rememberSaveable survives screen changes
    var savedTabIndex by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedTabIndex,
        pageCount = { DirectoryTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope()

    // Persist tab changes
    LaunchedEffect(pagerState.currentPage) {
        savedTabIndex = pagerState.currentPage
    }

    // Context-aware placeholder based on current tab
    val currentTab = DirectoryTab.entries[pagerState.currentPage]
    val searchPlaceholder = when (currentTab) {
        DirectoryTab.DIVISIONS -> "Search divisions…"
        DirectoryTab.OFFICIALS -> "Search MPs, parties, constituencies…"
        else -> "Search…"
    }

    // Configure the global search bar — rendered at the app shell level
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = searchQuery,
            placeholder = searchPlaceholder,
            onQueryChange = viewModel::updateSearchQuery,
            onFilterClick = { showFilterSheet = true },
            hasActiveFilters = filterState.hasActiveFilters
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row — Miko Updates-style scrollable sub-tabs under the search bar.
        // ScrollableTabRow allows variable-width tabs so longer names like
        // "Divisions" fit without truncation.
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            edgePadding = 16.dp
        ) {
            DirectoryTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    selectedContentColor = MaterialTheme.colorScheme.onSurface,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    text = {
                        TabTextWithBadge(
                            text = tab.title,
                            badgeCount = tabCounts[index]
                        )
                    }
                )
            }
        }

        // Pager — each tab has its own content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().weight(1f)
        ) { page ->
            when (DirectoryTab.entries[page]) {
                DirectoryTab.OFFICIALS -> OfficialsTabContent(
                    searchQuery = searchQuery,
                    viewMode = viewMode,
                    lazyPagingItems = lazyPagingItems,
                    searchResults = searchResults,
                    filteredMps = filteredMps,
                    hasActiveFilters = filterState.hasActiveFilters,
                    onNavigateToProfile = onNavigateToProfile
                )

                DirectoryTab.PARTIES -> PlaceholderTabContent("Parties")

                DirectoryTab.BILLS -> BillsTabContent(
                    onNavigateToBill = onNavigateToBill,
                    searchQuery = searchQuery
                )

                DirectoryTab.DIVISIONS -> DivisionsTabContent(
                    onNavigateToDivision = onNavigateToDivision,
                    houseFilter = filterState.houseFilter,
                    searchQuery = searchQuery
                )

                DirectoryTab.DEBATES -> DivisionsTabContent(
                    onNavigateToDivision = onNavigateToDivision,
                    houseFilter = filterState.houseFilter,
                    searchQuery = searchQuery
                )
            }
        }
    }

    // Filter bottom sheet — tab-aware
    if (showFilterSheet) {
        val currentTabType = when (DirectoryTab.entries[pagerState.currentPage]) {
            DirectoryTab.OFFICIALS -> FilterTabType.OFFICIALS
            DirectoryTab.DIVISIONS -> FilterTabType.DIVISIONS
            DirectoryTab.DEBATES -> FilterTabType.DIVISIONS
            else -> FilterTabType.OTHER
        }
        FilterBottomSheet(
            distinctParties = distinctParties,
            filterState = filterState,
            tabType = currentTabType,
            viewMode = viewMode,
            onPartyToggle = viewModel::togglePartyFilter,
            onHouseChange = viewModel::setHouseFilter,
            onCurrentOnlyChange = viewModel::setCurrentOnly,
            onViewModeChange = viewModel::setViewMode,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

/**
 * Officials tab — shows the MP directory (list or grid).
 * This is the only tab with real data; others are placeholders.
 */
@Composable
private fun OfficialsTabContent(
    searchQuery: String,
    viewMode: DirectoryViewMode,
    lazyPagingItems: androidx.paging.compose.LazyPagingItems<com.goveye.app.domain.model.Mp>,
    searchResults: List<com.goveye.app.domain.model.Mp>,
    filteredMps: List<com.goveye.app.domain.model.Mp>,
    hasActiveFilters: Boolean,
    onNavigateToProfile: (Int) -> Unit
) {
    // Priority: search results > filtered browsing > paged browsing
    if (searchQuery.isNotBlank()) {
        if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No MPs found for \"$searchQuery\"",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                items(searchResults, key = { it.id }) { mp ->
                    MpListRow(
                        mp = mp,
                        onClick = { onNavigateToProfile(mp.id) }
                    )
                }
            }
        }
    } else if (hasActiveFilters) {
        // Filtered browsing — no search query, but filters are active.
        // Uses filteredMps (all MPs loaded + filtered in Kotlin) instead of
        // the paged flow, which can't be re-collected when filters change.
        if (filteredMps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No MPs match these filters",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            when (viewMode) {
                DirectoryViewMode.LIST -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredMps, key = { it.id }) { mp ->
                            MpListRow(
                                mp = mp,
                                onClick = { onNavigateToProfile(mp.id) }
                            )
                        }
                    }
                }

                DirectoryViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = MaterialTheme.padding.medium,
                            vertical = MaterialTheme.padding.small
                        ),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
                    ) {
                        items(filteredMps, key = { it.id }) { mp ->
                            MpGridCard(
                                mp = mp,
                                onClick = { onNavigateToProfile(mp.id) }
                            )
                        }
                    }
                }
            }
        }
    } else {
        val refreshState = lazyPagingItems.loadState.refresh
        when {
            refreshState is androidx.paging.LoadState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            refreshState is androidx.paging.LoadState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't load MPs",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Check your connection and try again",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            refreshState is androidx.paging.LoadState.NotLoading && lazyPagingItems.itemCount == 0 -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No MPs found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                when (viewMode) {
                    DirectoryViewMode.LIST -> {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id },
                                contentType = { "mp_row" }
                            ) { index ->
                                val mp = lazyPagingItems[index]
                                if (mp != null) {
                                    MpListRow(
                                        mp = mp,
                                        onClick = { onNavigateToProfile(mp.id) }
                                    )
                                }
                            }
                        }
                    }

                    DirectoryViewMode.GRID -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                horizontal = MaterialTheme.padding.medium,
                                vertical = MaterialTheme.padding.small
                            ),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = lazyPagingItems.itemKey { it.id },
                                contentType = { "mp_grid" }
                            ) { index ->
                                val mp = lazyPagingItems[index]
                                if (mp != null) {
                                    MpGridCard(
                                        mp = mp,
                                        onClick = { onNavigateToProfile(mp.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Placeholder for tabs that don't have data yet.
 */
@Composable
private fun PlaceholderTabContent(label: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$label\n\nComing soon.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
