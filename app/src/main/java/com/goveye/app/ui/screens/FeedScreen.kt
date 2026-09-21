package com.goveye.app.ui.screens

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.components.SubTab
import com.goveye.app.ui.components.SubTabPager
import com.goveye.app.ui.screens.directory.DirectoryFilterState
import com.goveye.app.ui.screens.directory.FilterBottomSheet
import com.goveye.app.ui.screens.directory.FilterTabType
import com.goveye.app.ui.screens.divisions.MpMicroviewDialog
import com.goveye.app.ui.screens.divisions.MpMicroviewMode
import com.goveye.app.ui.screens.divisions.TagMicroviewDialog
import com.goveye.app.ui.screens.feed.CardType
import com.goveye.app.ui.screens.feed.FeedDateGroup
import com.goveye.app.ui.screens.feed.FeedDateHeader
import com.goveye.app.ui.screens.feed.FeedFinancialCard
import com.goveye.app.ui.screens.feed.FeedItem
import com.goveye.app.ui.screens.feed.FeedMpFinancialComboCard
import com.goveye.app.ui.screens.feed.FeedMpVoteCard
import com.goveye.app.ui.screens.feed.FeedNoActivityEmptyState
import com.goveye.app.ui.screens.feed.FeedNoFollowsEmptyState
import com.goveye.app.ui.screens.feed.FeedRecessEmptyState
import com.goveye.app.ui.screens.feed.FeedSpeechCard
import com.goveye.app.ui.screens.feed.FeedSpeechComboCard
import com.goveye.app.ui.screens.feed.FeedUiState
import com.goveye.app.ui.screens.feed.FeedViewModel
import com.goveye.app.ui.screens.feed.FeedWrittenQuestionCard
import com.goveye.app.ui.screens.feed.UnifiedFeedCard

/**
 * Feed sub-tab definitions.
 *
 * - **MPs** — followed MP activity: speeches, votes, financial (income/expenses).
 *   Already filtered to followed MPs by the ViewModel/data layer.
 * - **Debates** — parliamentary activity: divisions, legislation, statements.
 * - **Government** — executive output: publications.
 *
 * All tabs remain date-sorted — the top of each tab is always "today".
 */
private enum class FeedTab(val title: String, val cardTypes: Set<CardType>) {
    MPS(
        "MPs",
        setOf(
            CardType.SPEECH,
            CardType.SPEECH_COMBO,
            CardType.FINANCIAL,
            CardType.MP_FINANCIAL_COMBO,
            CardType.WRITTEN_QUESTION
        )
    ),
    DEBATES("Debates", setOf(CardType.DIVISION, CardType.STATEMENT)),
    GOVERNMENT("Government", setOf(CardType.PUBLICATION, CardType.LEGISLATION))
}

/**
 * Feed tab — chronological feed with sub-tabs (MPs / Debates / Government),
 * sticky date headers, followed-MP highlighting, filter, and recess-aware
 * empty states.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    onNavigateToDivision: (Int, Int) -> Unit,
    onNavigateToPublicationDetail: (Int) -> Unit,
    onNavigateToStatementDetail: (Int) -> Unit,
    onNavigateToLegislationDetail: (Int) -> Unit,
    onNavigateToTranscript: (Int, String, String) -> Unit,
    onNavigateToProfile: (Int, com.goveye.app.ui.navigation.MpHeaderFallback?, Int) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier,
    viewModel: FeedViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var microviewMemberId by remember { mutableStateOf<Int?>(null) }
    var microviewName by remember { mutableStateOf("") }
    var microviewPartyColor by remember { mutableStateOf<String?>(null) }
    var microviewMode by remember { mutableStateOf(MpMicroviewMode.VOTES) }

    // Save sub-tab index across navigation — same pattern as DirectoryScreen.
    var savedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var currentPage by remember { mutableIntStateOf(savedTabIndex) }

    Log.i(
        "GovEye/Feed",
        "FeedScreen compose — isLoading=${state.isLoading} isEmpty=${state.isEmpty} " +
            "isRecessEmpty=${state.isRecessEmpty} dateGroups=${state.dateGroups.size} " +
            "totalDivisions=${state.totalDivisions} currentPage=$currentPage"
    )

    // Stable lambdas — prevents ConfigureSearchBar's DisposableEffect from
    // firing on every recomposition (which would trigger FloatingSearchBar
    // recompositions at the app shell level during navigation transitions).
    val onQueryChange = remember(viewModel) { viewModel::setSearchQuery }
    val onFilterClick = remember { { showFilterSheet = true } }

    val currentTab = FeedTab.entries[currentPage]
    val searchPlaceholder = when (currentTab) {
        FeedTab.MPS -> "Search followed MP activity…"
        FeedTab.DEBATES -> "Search debates…"
        FeedTab.GOVERNMENT -> "Search publications…"
    }

    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            placeholder = searchPlaceholder,
            query = state.searchQuery,
            onQueryChange = onQueryChange,
            onFilterClick = onFilterClick,
            hasActiveFilters = state.followingOnly || state.houseFilter != 0
        )
    )

    // Pre-compute filtered date groups per tab so adjacent pager pages
    // show correct content during swipe transitions.
    val mpsGroups = remember(state.dateGroups) {
        state.dateGroups.filterGroups(FeedTab.MPS.cardTypes)
    }
    val debatesGroups = remember(state.dateGroups) {
        state.dateGroups.filterGroups(FeedTab.DEBATES.cardTypes)
    }
    val governmentGroups = remember(state.dateGroups) {
        state.dateGroups.filterGroups(FeedTab.GOVERNMENT.cardTypes)
    }

    // Per-tab item counts for badge counts.
    val mpsCount = mpsGroups.sumOf { it.items.size }
    val debatesCount = debatesGroups.sumOf { it.items.size }
    val governmentCount = governmentGroups.sumOf { it.items.size }

    if (state.isLoading) {
        com.goveye.app.ui.components.SkeletonScreen(
            cardType = com.goveye.app.ui.components.SkeletonCardType.FEED,
            itemCount = 4,
            showDateHeader = true
        )
    } else {
        SubTabPager(
            tabs = listOf(
                SubTab(FeedTab.MPS.title, mpsCount.takeIf { it > 0 }),
                SubTab(FeedTab.DEBATES.title, debatesCount.takeIf { it > 0 }),
                SubTab(FeedTab.GOVERNMENT.title, governmentCount.takeIf { it > 0 })
            ),
            initialPage = savedTabIndex,
            scrollable = false,
            edgePadding = 0.dp,
            onPageChange = { page ->
                currentPage = page
                savedTabIndex = page
            },
            modifier = modifier.fillMaxSize()
        ) { page ->
            val tab = FeedTab.entries[page]
            when (tab) {
                FeedTab.MPS -> {
                    if (state.followedMemberIds.isEmpty()) {
                        FeedNoFollowsEmptyState()
                    } else if (mpsGroups.isEmpty() && state.isRecessEmpty) {
                        FeedRecessEmptyState(
                            recessEndDate = state.currentRecess?.endDate ?: "",
                            lastDivisions = state.recentDivisionsForRecess,
                            onDivisionClick = onNavigateToDivision
                        )
                    } else if (mpsGroups.isEmpty()) {
                        FeedNoActivityEmptyState()
                    } else {
                        FeedList(
                            dateGroups = mpsGroups,
                            state = state,
                            onNavigateToDivision = onNavigateToDivision,
                            onNavigateToPublicationDetail = onNavigateToPublicationDetail,
                            onNavigateToStatementDetail = onNavigateToStatementDetail,
                            onNavigateToLegislationDetail = onNavigateToLegislationDetail,
                            onNavigateToTranscript = onNavigateToTranscript,
                            onTagClick = { tag -> selectedTag = tag },
                            onProfileClick = { memberId, name, partyColor, mode ->
                                microviewMemberId = memberId
                                microviewName = name
                                microviewPartyColor = partyColor
                                microviewMode = mode
                            }
                        )
                    }
                }

                FeedTab.DEBATES -> {
                    if (debatesGroups.isEmpty() && state.isRecessEmpty) {
                        FeedRecessEmptyState(
                            recessEndDate = state.currentRecess?.endDate ?: "",
                            lastDivisions = state.recentDivisionsForRecess,
                            onDivisionClick = onNavigateToDivision
                        )
                    } else if (debatesGroups.isEmpty()) {
                        FeedNoActivityEmptyState()
                    } else {
                        FeedList(
                            dateGroups = debatesGroups,
                            state = state,
                            onNavigateToDivision = onNavigateToDivision,
                            onNavigateToPublicationDetail = onNavigateToPublicationDetail,
                            onNavigateToStatementDetail = onNavigateToStatementDetail,
                            onNavigateToLegislationDetail = onNavigateToLegislationDetail,
                            onNavigateToTranscript = onNavigateToTranscript,
                            onTagClick = { tag -> selectedTag = tag },
                            onProfileClick = { memberId, name, partyColor, mode ->
                                microviewMemberId = memberId
                                microviewName = name
                                microviewPartyColor = partyColor
                                microviewMode = mode
                            }
                        )
                    }
                }

                FeedTab.GOVERNMENT -> {
                    if (governmentGroups.isEmpty()) {
                        FeedNoActivityEmptyState()
                    } else {
                        FeedList(
                            dateGroups = governmentGroups,
                            state = state,
                            onNavigateToDivision = onNavigateToDivision,
                            onNavigateToPublicationDetail = onNavigateToPublicationDetail,
                            onNavigateToStatementDetail = onNavigateToStatementDetail,
                            onNavigateToLegislationDetail = onNavigateToLegislationDetail,
                            onNavigateToTranscript = onNavigateToTranscript,
                            onTagClick = { tag -> selectedTag = tag },
                            onProfileClick = { memberId, name, partyColor, mode ->
                                microviewMemberId = memberId
                                microviewName = name
                                microviewPartyColor = partyColor
                                microviewMode = mode
                            }
                        )
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

    // MP microview dialog — opened by clicking the MP avatar on a feed card.
    // Mode depends on the card type: FINANCES for financial cards, VOTES for
    // vote and speech cards. The user can open the full profile from the
    // dialog header.
    microviewMemberId?.let { memberId ->
        MpMicroviewDialog(
            memberId = memberId,
            fallbackName = microviewName,
            fallbackPartyName = null,
            fallbackPartyColour = microviewPartyColor,
            fallbackConstituency = null,
            onNavigateToFullProfile = { id ->
                microviewMemberId = null
                onNavigateToProfile(
                    id,
                    com.goveye.app.ui.navigation.MpHeaderFallback(
                        name = microviewName,
                        partyColor = microviewPartyColor
                    ),
                    if (microviewMode == MpMicroviewMode.FINANCES) 5 else 0
                )
            },
            onDismiss = { microviewMemberId = null },
            mode = microviewMode
        )
    }
}

/**
 * Filter [FeedDateGroup]s to only items whose [FeedItem.cardType] is in
 * [cardTypes]. Groups with no matching items are dropped.
 */
private fun List<FeedDateGroup>.filterGroups(cardTypes: Set<CardType>): List<FeedDateGroup> = mapNotNull { group ->
    val filtered = group.items.filter { it.cardType in cardTypes }
    if (filtered.isEmpty()) null else group.copy(items = filtered)
}

/**
 * Reusable LazyColumn that renders [dateGroups] with sticky date headers
 * and per-item [FeedItemCard] rendering.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FeedList(
    dateGroups: List<FeedDateGroup>,
    state: FeedUiState,
    onNavigateToDivision: (Int, Int) -> Unit,
    onNavigateToPublicationDetail: (Int) -> Unit,
    onNavigateToStatementDetail: (Int) -> Unit,
    onNavigateToLegislationDetail: (Int) -> Unit,
    onNavigateToTranscript: (Int, String, String) -> Unit,
    onTagClick: (String) -> Unit,
    onProfileClick: (Int, String, String?, MpMicroviewMode) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        dateGroups.forEach { group ->
            stickyHeader(key = "header-${group.dateKey}") {
                FeedDateHeader(dateHeader = group.dateHeader)
            }
            items(
                items = group.items,
                key = { item -> "${item.typePrefix}-${item.id}" },
                contentType = { item -> "feed_${item.typePrefix}" }
            ) { item ->
                FeedItemCard(
                    item = item,
                    state = state,
                    onNavigateToDivision = onNavigateToDivision,
                    onNavigateToPublicationDetail = onNavigateToPublicationDetail,
                    onNavigateToStatementDetail = onNavigateToStatementDetail,
                    onNavigateToLegislationDetail = onNavigateToLegislationDetail,
                    onNavigateToTranscript = onNavigateToTranscript,
                    onTagClick = onTagClick,
                    onProfileClick = onProfileClick
                )
            }
        }
    }
}

/**
 * Renders the [UnifiedFeedCard] for any [FeedItem] subtype. The unified card
 * extracts per-type data (image, title, type pill, by-who, division bar,
 * source, date, tags) internally — no per-type branching is needed here.
 */
@Composable
private fun FeedItemCard(
    item: FeedItem,
    state: com.goveye.app.ui.screens.feed.FeedUiState,
    onNavigateToDivision: (Int, Int) -> Unit,
    onNavigateToPublicationDetail: (Int) -> Unit,
    onNavigateToStatementDetail: (Int) -> Unit,
    onNavigateToLegislationDetail: (Int) -> Unit,
    onNavigateToTranscript: (Int, String, String) -> Unit,
    onTagClick: (String) -> Unit,
    onProfileClick: (Int, String, String?, MpMicroviewMode) -> Unit = { _, _, _, _ -> }
) {
    when (item) {
        is FeedItem.FinancialItem -> FeedFinancialCard(
            item = item,
            onClick = { /* navigate to financial detail */ },
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.FINANCES)
            }
        )

        is FeedItem.MpFinancialComboItem -> FeedMpFinancialComboCard(
            item = item,
            onClick = { /* navigate to financial detail */ },
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.FINANCES)
            }
        )

        is FeedItem.SpeechItem -> FeedSpeechCard(
            item = item,
            onClick = { onNavigateToDivision(item.divisionId, 1) },
            onNavigateToTranscript = onNavigateToTranscript,
            onTagClick = onTagClick,
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.VOTES)
            }
        )

        is FeedItem.SpeechComboItem -> FeedSpeechComboCard(
            item = item,
            onClick = { onNavigateToDivision(item.speeches.first().divisionId, 1) },
            onNavigateToTranscript = onNavigateToTranscript,
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.VOTES)
            }
        )

        is FeedItem.MpVoteItem -> FeedMpVoteCard(
            item = item,
            onClick = { onNavigateToDivision(item.divisionId, item.divisionHouse) },
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.VOTES)
            }
        )

        is FeedItem.WrittenQuestionItem -> FeedWrittenQuestionCard(
            item = item,
            onClick = { /* written question detail — future */ },
            onProfileClick = {
                onProfileClick(item.memberId, item.memberName, item.memberPartyColorHex, MpMicroviewMode.VOTES)
            }
        )

        is FeedItem.DivisionItem -> {
            val hasFollowedVotes = item.division.id in state.divisionsWithFollowedVotes
            UnifiedFeedCard(
                item = item,
                hasFollowedVotes = hasFollowedVotes,
                onClick = { onNavigateToDivision(item.division.id, item.division.house) },
                onTagClick = onTagClick
            )
        }

        is FeedItem.PublicationItem -> UnifiedFeedCard(
            item = item,
            hasFollowedVotes = false,
            onClick = { onNavigateToPublicationDetail(item.publication.id) },
            onTagClick = onTagClick
        )

        is FeedItem.StatementItem -> UnifiedFeedCard(
            item = item,
            hasFollowedVotes = false,
            onClick = { onNavigateToStatementDetail(item.statement.id) },
            onTagClick = onTagClick
        )

        is FeedItem.LegislationItem -> UnifiedFeedCard(
            item = item,
            hasFollowedVotes = false,
            onClick = { onNavigateToLegislationDetail(item.legislation.id) },
            onTagClick = onTagClick
        )
    }
}
