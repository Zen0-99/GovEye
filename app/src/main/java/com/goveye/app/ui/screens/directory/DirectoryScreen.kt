package com.goveye.app.ui.screens.directory

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import com.goveye.app.ui.components.InfoCard
import com.goveye.app.ui.components.StickyInfoCard
import com.goveye.app.ui.components.TabTextWithBadge
import com.goveye.app.ui.screens.bills.BillsTabContent
import com.goveye.app.ui.screens.divisions.DivisionsTabContent
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.launch

private enum class DirectoryTab(val title: String) {
    OFFICIALS("Officials"),
    PARTIES("Parties"),
    BILLS("Bills"),
    DIVISIONS("Debates")
}

@Composable
fun DirectoryScreen(
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToBill: (Int) -> Unit = {},
    onNavigateToParty: (Int) -> Unit = {},
    showInfoCards: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: DirectoryViewModel = hiltViewModel()
) {
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.pagedMps.collectAsLazyPagingItems()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val filteredMps by viewModel.filteredMps.collectAsStateWithLifecycle()
    val tabCounts by viewModel.tabCounts.collectAsStateWithLifecycle(emptyMap())
    val filterState by viewModel.filterState.collectAsStateWithLifecycle(DirectoryFilterState())
    val distinctParties by viewModel.distinctParties.collectAsStateWithLifecycle(emptyList())
    val parties by viewModel.parties.collectAsStateWithLifecycle(emptyList())
    var showFilterSheet by remember { mutableStateOf(false) }

    // Save tab index across navigation — rememberSaveable survives screen changes
    var savedTabIndex by rememberSaveable { mutableStateOf(0) }
    val pagerState = rememberPagerState(
        initialPage = savedTabIndex,
        pageCount = { DirectoryTab.entries.size }
    )

    Log.i(
        "GovEye/Directory",
        "DirectoryScreen compose — searchQuery='$searchQuery' pagingItems=${lazyPagingItems.itemCount} " +
            "searchResults=${searchResults.size} filteredMps=${filteredMps.size} " +
            "hasFilters=${filterState.hasActiveFilters} currentPage=${pagerState.currentPage}"
    )
    val coroutineScope = rememberCoroutineScope()

    // Persist tab changes
    LaunchedEffect(pagerState.currentPage) {
        savedTabIndex = pagerState.currentPage
    }

    // Context-aware placeholder based on current tab
    val currentTab = DirectoryTab.entries[pagerState.currentPage]
    val searchPlaceholder = when (currentTab) {
        DirectoryTab.DIVISIONS -> "Search debates…"
        DirectoryTab.OFFICIALS -> "Search MPs, parties, constituencies…"
        else -> "Search…"
    }

    // Stable lambdas — prevents ConfigureSearchBar's DisposableEffect from
    // firing on every recomposition (which would trigger FloatingSearchBar
    // recompositions at the app shell level during navigation transitions).
    val onQueryChange = remember(viewModel) { viewModel::updateSearchQuery }
    val onFilterClick = remember { { showFilterSheet = true } }

    // Configure the global search bar — rendered at the app shell level
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = searchQuery,
            placeholder = searchPlaceholder,
            onQueryChange = onQueryChange,
            onFilterClick = onFilterClick,
            hasActiveFilters = filterState.hasActiveFilters
        )
    )

    Column(modifier = modifier.fillMaxSize()) {
        // Tab row — 4 tabs fill the full width evenly.
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
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

        // Pager — each tab has its own content.
        // Offscreen page limiting: only compose the current page ± 1 to
        // avoid jank from all 4 tabs composing their lists simultaneously.
        // Pattern from Miko's AnimeLibraryPager.
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().weight(1f)
        ) { page ->
            if (page !in ((pagerState.currentPage - 1)..(pagerState.currentPage + 1))) {
                Log.i("GovEye/Directory", "Pager page $page skipped (offscreen)")
                return@HorizontalPager
            }
            Log.i("GovEye/Directory", "Pager composing page $page: ${DirectoryTab.entries[page]}")
            when (DirectoryTab.entries[page]) {
                DirectoryTab.OFFICIALS -> OfficialsTabContent(
                    searchQuery = searchQuery,
                    viewMode = viewMode,
                    lazyPagingItems = lazyPagingItems,
                    searchResults = searchResults,
                    filteredMps = filteredMps,
                    hasActiveFilters = filterState.hasActiveFilters,
                    onNavigateToProfile = onNavigateToProfile,
                    showInfoCards = showInfoCards
                )

                DirectoryTab.PARTIES -> PartiesTabContent(
                    parties = parties,
                    onNavigateToParty = onNavigateToParty
                )

                DirectoryTab.BILLS -> BillsTabContent(
                    onNavigateToBill = onNavigateToBill,
                    searchQuery = searchQuery,
                    showInfoCards = showInfoCards
                )

                DirectoryTab.DIVISIONS -> DivisionsTabContent(
                    onNavigateToDivision = onNavigateToDivision,
                    houseFilter = filterState.houseFilter,
                    searchQuery = searchQuery,
                    showInfoCards = showInfoCards
                )
            }
        }
    }

    // Filter bottom sheet — tab-aware
    if (showFilterSheet) {
        val currentTabType = when (DirectoryTab.entries[pagerState.currentPage]) {
            DirectoryTab.OFFICIALS -> FilterTabType.OFFICIALS
            DirectoryTab.DIVISIONS -> FilterTabType.DIVISIONS
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
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OfficialsTabContent(
    searchQuery: String,
    viewMode: DirectoryViewMode,
    lazyPagingItems: androidx.paging.compose.LazyPagingItems<com.goveye.app.domain.model.Mp>,
    searchResults: List<com.goveye.app.domain.model.Mp>,
    filteredMps: List<com.goveye.app.domain.model.Mp>,
    hasActiveFilters: Boolean,
    onNavigateToProfile: (Int) -> Unit,
    showInfoCards: Boolean = true
) {
    Log.i(
        "GovEye/Directory",
        "OfficialsTabContent compose — searchQuery='$searchQuery' pagingItems=${lazyPagingItems.itemCount} searchResults=${searchResults.size} filteredMps=${filteredMps.size} hasFilters=$hasActiveFilters"
    )
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showInfoCards) {
                    stickyHeader(key = "tab-info") {
                        StickyInfoCard(
                            title = "Officials",
                            subtitle = "Browse all MPs and Lords in the UK Parliament."
                        )
                    }
                }
                items(searchResults, key = { it.id }, contentType = { "mp_row" }) { mp ->
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
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (showInfoCards) {
                            stickyHeader(key = "tab-info") {
                                StickyInfoCard(
                                    title = "Officials",
                                    subtitle = "Browse all MPs and Lords in the UK Parliament."
                                )
                            }
                        }
                        items(filteredMps, key = { it.id }, contentType = { "mp_row" }) { mp ->
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
                        if (showInfoCards) {
                            item(key = "tab-info", span = { GridItemSpan(maxLineSpan) }) {
                                InfoCard(
                                    title = "Officials",
                                    subtitle = "Browse all MPs and Lords in the UK Parliament."
                                )
                            }
                        }
                        items(filteredMps, key = { it.id }, contentType = { "mp_grid" }) { mp ->
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
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showInfoCards) {
                                stickyHeader(key = "tab-info") {
                                    StickyInfoCard(
                                        title = "Officials",
                                        subtitle = "Browse all MPs and Lords in the UK Parliament."
                                    )
                                }
                            }
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
                            if (showInfoCards) {
                                item(key = "tab-info", span = { GridItemSpan(maxLineSpan) }) {
                                    InfoCard(
                                        title = "Officials",
                                        subtitle = "Browse all MPs and Lords in the UK Parliament."
                                    )
                                }
                            }
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
