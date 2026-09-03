package com.goveye.app.ui.screens

import android.util.Log
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.components.StickyInfoCard
import com.goveye.app.ui.screens.directory.DirectoryFilterState
import com.goveye.app.ui.screens.directory.FilterBottomSheet
import com.goveye.app.ui.screens.directory.FilterTabType
import com.goveye.app.ui.screens.divisions.TagMicroviewDialog
import com.goveye.app.ui.screens.feed.FeedDateHeader
import com.goveye.app.ui.screens.feed.FeedFinancialCard
import com.goveye.app.ui.screens.feed.FeedItem
import com.goveye.app.ui.screens.feed.FeedMpVoteCard
import com.goveye.app.ui.screens.feed.FeedNoActivityEmptyState
import com.goveye.app.ui.screens.feed.FeedNoFollowsEmptyState
import com.goveye.app.ui.screens.feed.FeedRecessEmptyState
import com.goveye.app.ui.screens.feed.FeedSpeechCard
import com.goveye.app.ui.screens.feed.FeedViewModel
import com.goveye.app.ui.screens.feed.UnifiedFeedCard

/**
 * Feed tab — chronological mixed feed with sticky date headers,
 * showing divisions, publications, statements, and legislation.
 * Followed-MP highlighting, filter, and recess-aware empty states.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FeedScreen(
    onNavigateToDivision: (Int, Int) -> Unit,
    onNavigateToPublicationDetail: (Int) -> Unit,
    onNavigateToStatementDetail: (Int) -> Unit,
    onNavigateToLegislationDetail: (Int) -> Unit,
    onNavigateToTranscript: (Int, String, String) -> Unit,
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
                FeedSkeletonScreen()
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
    onTagClick: (String) -> Unit
) {
    when (item) {
        is FeedItem.FinancialItem -> FeedFinancialCard(
            item = item,
            onClick = { /* navigate to financial detail */ }
        )

        is FeedItem.SpeechItem -> FeedSpeechCard(
            item = item,
            onClick = { onNavigateToDivision(item.divisionId, 1) },
            onNavigateToTranscript = onNavigateToTranscript,
            onTagClick = onTagClick
        )

        is FeedItem.MpVoteItem -> FeedMpVoteCard(
            item = item,
            onClick = { onNavigateToDivision(item.divisionId, item.divisionHouse) }
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

/**
 * Skeleton loading screen for the feed — shows animated placeholder cards
 * that mimic the feed card structure (image area + title + subtitle lines).
 *
 * Replaces the centered [CircularProgressIndicator] with a structural
 * placeholder that communicates "content is loading here" rather than
 * "the app is thinking." The shimmer animation uses a simple alpha pulse
 * — no shimmer gradient mask (keeps it lightweight for a 1-2s load).
 */
@Composable
private fun FeedSkeletonScreen() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )
    val placeholderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
    val cardShape = RoundedCornerShape(16.dp)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date header placeholder
        item(key = "skeleton-header") {
            SkeletonLine(
                width = 120.dp,
                height = 16.dp,
                color = placeholderColor,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        // 4 card placeholders — each mimics a feed card: image area + title + subtitle
        items(count = 4, key = { "skeleton-card-$it" }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(cardShape)
                    .background(placeholderColor.copy(alpha = 0.1f))
                    .padding(0.dp)
            ) {
                // Image placeholder (16:9 aspect ratio area)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(placeholderColor.copy(alpha = 0.15f))
                )
                // Content area
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title line
                    SkeletonLine(
                        width = 240.dp,
                        height = 18.dp,
                        color = placeholderColor
                    )
                    // Subtitle line (shorter)
                    SkeletonLine(
                        width = 160.dp,
                        height = 14.dp,
                        color = placeholderColor
                    )
                    // Bottom row: source + date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonLine(
                            width = 80.dp,
                            height = 12.dp,
                            color = placeholderColor
                        )
                        SkeletonLine(
                            width = 60.dp,
                            height = 12.dp,
                            color = placeholderColor
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single skeleton placeholder line — a rounded rectangle with the given
 * dimensions and color. Used by [FeedSkeletonScreen] to build card placeholders.
 */
@Composable
private fun SkeletonLine(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(color)
    )
}
