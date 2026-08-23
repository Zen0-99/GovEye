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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.goveye.app.ui.screens.bills.BillBrowseViewModel
import com.goveye.app.ui.screens.bills.BillsTabContent
import com.goveye.app.ui.screens.divisions.DivisionBrowseViewModel
import com.goveye.app.ui.screens.divisions.DivisionsTabContent
import com.goveye.app.ui.theme.padding

private enum class DirectoryTab(val title: String) {
    OFFICIALS("Officials"),
    PARTIES("Parties"),
    COMMITTEES("Committees"),
    COUNCILS("Councils"),
    BILLS("Bills"),
    DIVISIONS("Debates"),
    GOVERNMENT("Government")
}

@Composable
fun DirectoryScreen(
    onNavigateToProfile: (Int) -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit = { _, _ -> },
    onNavigateToBill: (Int) -> Unit = {},
    onNavigateToParty: (Int) -> Unit = {},
    onNavigateToCommittee: (Int) -> Unit = {},
    onNavigateToCouncil: (Int) -> Unit = {},
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
    val councils by viewModel.councils.collectAsStateWithLifecycle(emptyList())
    val committees by viewModel.committees.collectAsStateWithLifecycle(emptyList())
    val governmentSourceType by viewModel.governmentSourceType.collectAsStateWithLifecycle(GovernmentSourceType.ALL)
    val governmentPublications by viewModel.governmentPublications.collectAsStateWithLifecycle(emptyList())
    val governmentStatements by viewModel.governmentStatements.collectAsStateWithLifecycle(emptyList())
    val governmentLegislation by viewModel.governmentLegislation.collectAsStateWithLifecycle(emptyList())
    val governmentLoading by viewModel.governmentLoading.collectAsStateWithLifecycle(true)
    val allAnnouncementTags by viewModel.allAnnouncementTags.collectAsStateWithLifecycle(emptyList())
    val allDepartments by viewModel.allDepartments.collectAsStateWithLifecycle(emptyList())
    val allSources by viewModel.allSources.collectAsStateWithLifecycle(emptyList())
    val postcodeState by viewModel.postcodeResult.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    // Eagerly create Bills and Divisions ViewModels at the DirectoryScreen
    // level so their flow chains start immediately — not on first tab visit.
    // Without this, the first composition of each tab happens during a
    // navigation transition, causing jank (ViewModel creation + flow startup
    // + DB query on the main thread during the 150ms animation).
    val billsViewModel: BillBrowseViewModel = hiltViewModel()
    val billsState by billsViewModel.state.collectAsStateWithLifecycle()
    val divisionsViewModel: DivisionBrowseViewModel = hiltViewModel()
    val divisionsState by divisionsViewModel.state.collectAsStateWithLifecycle()

    // Save tab index across navigation — rememberSaveable survives screen changes
    var savedTabIndex by rememberSaveable { mutableStateOf(0) }

    // Track current page for search bar config and filter sheet
    var currentPage by remember { mutableIntStateOf(savedTabIndex) }

    Log.i(
        "GovEye/Directory",
        "DirectoryScreen compose — searchQuery='$searchQuery' pagingItems=${lazyPagingItems.itemCount} " +
            "searchResults=${searchResults.size} filteredMps=${filteredMps.size} " +
            "hasFilters=${filterState.hasActiveFilters} currentPage=$currentPage"
    )

    // Context-aware placeholder based on current tab
    val currentTab = DirectoryTab.entries[currentPage]
    val searchPlaceholder = when (currentTab) {
        DirectoryTab.DIVISIONS -> "Search debates…"
        DirectoryTab.OFFICIALS -> "Search MPs, parties, constituencies, postcodes…"
        DirectoryTab.COMMITTEES -> "Search committees…"
        DirectoryTab.COUNCILS -> "Search councils…"
        DirectoryTab.GOVERNMENT -> "Search government announcements…"
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
        com.goveye.app.ui.components.SubTabPager(
            tabs = DirectoryTab.entries.mapIndexed { index, tab ->
                com.goveye.app.ui.components.SubTab(
                    label = tab.title,
                    badgeCount = tabCounts[index]
                )
            },
            initialPage = savedTabIndex,
            edgePadding = 0.dp,
            onPageChange = { page ->
                currentPage = page
                savedTabIndex = page
            },
            modifier = Modifier.fillMaxWidth()
        ) { page ->
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
                    showInfoCards = showInfoCards,
                    postcodeState = postcodeState
                )

                DirectoryTab.PARTIES -> PartiesTabContent(
                    parties = parties,
                    onNavigateToParty = onNavigateToParty
                )

                DirectoryTab.COMMITTEES -> CommitteesTabContent(
                    committees = committees,
                    searchQuery = searchQuery,
                    onNavigateToCommittee = onNavigateToCommittee
                )

                DirectoryTab.COUNCILS -> CouncilsTabContent(
                    councils = councils,
                    searchQuery = searchQuery,
                    onNavigateToCouncil = onNavigateToCouncil
                )

                DirectoryTab.BILLS -> BillsTabContent(
                    onNavigateToBill = onNavigateToBill,
                    searchQuery = searchQuery,
                    showInfoCards = showInfoCards,
                    state = billsState,
                    onSearchQueryChange = billsViewModel::setSearchQuery
                )

                DirectoryTab.DIVISIONS -> DivisionsTabContent(
                    onNavigateToDivision = onNavigateToDivision,
                    houseFilter = filterState.houseFilter,
                    searchQuery = searchQuery,
                    showInfoCards = showInfoCards,
                    state = divisionsState,
                    onSearchQueryChange = divisionsViewModel::setSearchQuery,
                    onHouseFilterChange = divisionsViewModel::setHouseFilter,
                    onTagClick = { tag ->
                        viewModel.updateSearchQuery(tag)
                    }
                )

                DirectoryTab.GOVERNMENT -> GovernmentTabContent(
                    searchQuery = searchQuery,
                    sourceTypeFilter = governmentSourceType,
                    publications = governmentPublications,
                    statements = governmentStatements,
                    legislation = governmentLegislation,
                    isLoading = governmentLoading,
                    showInfoCards = showInfoCards,
                    onSourceTypeChange = viewModel::setGovernmentSourceType
                )
            }
        }
    }

    // Filter bottom sheet — tab-aware
    if (showFilterSheet) {
        val currentTabType = when (DirectoryTab.entries[currentPage]) {
            DirectoryTab.OFFICIALS -> FilterTabType.OFFICIALS
            DirectoryTab.DIVISIONS -> FilterTabType.DIVISIONS
            DirectoryTab.GOVERNMENT -> FilterTabType.GOVERNMENT
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
            onDismiss = { showFilterSheet = false },
            allTags = allAnnouncementTags,
            allDepartments = allDepartments,
            allSources = allSources,
            onTagToggle = viewModel::toggleTagFilter,
            onDepartmentToggle = viewModel::toggleDepartmentFilter,
            onSourceToggle = viewModel::toggleSourceFilter,
            onTypeChange = viewModel::setTypeFilter
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
    showInfoCards: Boolean = true,
    postcodeState: PostcodeSearchState = PostcodeSearchState.Idle
) {
    Log.i(
        "GovEye/Directory",
        "OfficialsTabContent compose — searchQuery='$searchQuery' pagingItems=${lazyPagingItems.itemCount} searchResults=${searchResults.size} filteredMps=${filteredMps.size} hasFilters=$hasActiveFilters"
    )
    // Priority: search results > filtered browsing > paged browsing
    if (searchQuery.isNotBlank()) {
        // Show loading state for postcode search
        if (postcodeState is PostcodeSearchState.Loading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Looking up postcode…",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        } else if (searchResults.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                val message = when (postcodeState) {
                    is PostcodeSearchState.NotFound -> "Postcode not found. Check the format (e.g., SW1A 1AA)."
                    else -> "No MPs found for \"$searchQuery\""
                }
                Text(
                    text = message,
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
                // Show postcode result header
                if (postcodeState is PostcodeSearchState.Found) {
                    val result = (postcodeState as PostcodeSearchState.Found).result
                    item(key = "postcode-header") {
                        PostcodeResultCard(
                            postcode = result.postcode,
                            constituency = result.constituencyName,
                            council = result.adminDistrict,
                            region = result.region
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

/**
 * Card showing postcode lookup result — constituency, council, region.
 * Displayed above the MP list when a postcode is searched.
 */
@Composable
private fun PostcodeResultCard(postcode: String, constituency: String?, council: String?, region: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = postcode,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (constituency != null) {
                Text(
                    text = constituency,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (council != null) {
                Text(
                    text = council,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            if (region != null) {
                Text(
                    text = region,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Councils tab — shows a searchable list of UK local authorities.
 * Card-based design similar to the Parties tab.
 */
@Composable
private fun CouncilsTabContent(
    councils: List<com.goveye.app.data.local.dao.CouncilSummary>,
    searchQuery: String,
    onNavigateToCouncil: (Int) -> Unit
) {
    val filtered = remember(councils, searchQuery) {
        if (searchQuery.isBlank()) {
            councils
        } else {
            councils.filter { it.name.contains(searchQuery, ignoreCase = true) }
        }
    }

    if (filtered.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "No councils found for \"$searchQuery\""
                } else {
                    "No councils available"
                },
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filtered, key = { it.id }, contentType = { "council_card" }) { council ->
                CouncilCard(
                    council = council,
                    onClick = { onNavigateToCouncil(council.id) }
                )
            }
        }
    }
}

/**
 * Council card — shows council name, type, and region.
 * Similar visual style to party cards.
 */
@Composable
private fun CouncilCard(council: com.goveye.app.data.local.dao.CouncilSummary, onClick: () -> Unit) {
    val typeLabel = when (council.localAuthorityType) {
        "MD" -> "Metropolitan District"
        "NMD" -> "Non-Metropolitan District"
        "UA" -> "Unitary Authority"
        "LBO" -> "London Borough"
        "CC" -> "County Council"
        "MBC" -> "Metropolitan Borough"
        else -> council.localAuthorityType ?: "Local Authority"
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Council icon — building emoji or first letter
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = council.name.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = council.name,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
