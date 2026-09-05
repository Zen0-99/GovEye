package com.goveye.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonRemove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.SubTab
import com.goveye.app.ui.components.SubTabPager
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.screens.directory.FilterBottomSheet
import com.goveye.app.ui.screens.directory.FilterTabType
import com.goveye.app.ui.screens.directory.PartyCardShared
import com.goveye.app.ui.screens.following.FollowedMpUi
import com.goveye.app.ui.screens.following.FollowedPartyUi
import com.goveye.app.ui.screens.following.FollowingUiState
import com.goveye.app.ui.screens.following.FollowingViewModel
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.utils.partyLogoResId

/**
 * Following tab — central hub for all followed entities (D-14).
 *
 * Shows followed entities grouped by type: Officials (MPs), Parties, Sources,
 * and Tags. Each section lists followed entities with an unfollow action.
 * Tapping a card opens the entity's profile.
 *
 * Filter icon opens the FilterBottomSheet with FOLLOWING_HUB tab type.
 */
@Composable
fun FollowingScreen(
    onNavigateToProfile: (Int) -> Unit = {},
    onNavigateToParty: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilterSheet by remember { mutableStateOf(false) }

    // Configure the global search bar for filtering followed MPs
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = uiState.searchQuery,
            placeholder = "Search followed MPs…",
            onQueryChange = viewModel::updateSearchQuery,
            onFilterClick = { showFilterSheet = true },
            hasActiveFilters = uiState.filterState.hasActiveFilters
        )
    )

    // Sub-tab pager — Officials, Parties, Sources, Tags (issue #10)
    // Always shown so tabs remain visible during loading. The skeleton
    // appears inside the content area, not replacing the entire screen.
    SubTabPager(
        tabs = listOf(
            SubTab("Officials", uiState.followedMps.size.takeIf { it > 0 }),
            SubTab("Parties", uiState.followedParties.size.takeIf { it > 0 }),
            SubTab("Sources", null),
            SubTab("Tags", null)
        ),
        modifier = modifier,
        scrollable = true,
        edgePadding = 16.dp
    ) { page ->
        when {
            uiState.isLoading -> {
                com.goveye.app.ui.components.SkeletonScreen(
                    cardType = com.goveye.app.ui.components.SkeletonCardType.MP_ROW,
                    itemCount = 6,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            page == 0 -> OfficialsTabContent(
                uiState = uiState,
                onNavigateToProfile = onNavigateToProfile,
                onUnfollow = viewModel::unfollow
            )

            page == 1 -> PartiesTabContent(
                parties = uiState.followedParties,
                onNavigateToParty = onNavigateToParty,
                onUnfollow = viewModel::unfollowParty
            )

            page == 2 -> TabEmptyHint("No followed sources yet")

            page == 3 -> TabEmptyHint("No followed tags yet")
        }
    }

    // Filter bottom sheet — Following hub (D-14)
    if (showFilterSheet) {
        FilterBottomSheet(
            distinctParties = uiState.distinctParties,
            filterState = uiState.filterState,
            tabType = FilterTabType.FOLLOWING_HUB,
            viewMode = uiState.viewMode,
            onPartyToggle = viewModel::togglePartyFilter,
            onHouseChange = viewModel::setHouseFilter,
            onCurrentOnlyChange = viewModel::setCurrentOnly,
            onViewModeChange = viewModel::setViewMode,
            onClearFilters = viewModel::clearFilters,
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
private fun OfficialsTabContent(
    uiState: FollowingUiState,
    onNavigateToProfile: (Int) -> Unit,
    onUnfollow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    when {
        // Global empty state — no follows at all
        uiState.followedMps.isEmpty() && uiState.searchQuery.isBlank() && !uiState.filterState.hasActiveFilters -> {
            Box(
                modifier = modifier.fillMaxSize().padding(MaterialTheme.padding.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Following\n\nFollow MPs, parties, sources, and tags to track " +
                        "their votes and activity in your feed.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // No results from search or filters
        uiState.followedMps.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No followed MPs match your filters",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.followedMps,
                    key = { "official-${it.memberId}" }
                ) { followedMp ->
                    FollowedMpCard(
                        followedMp = followedMp,
                        onClick = { onNavigateToProfile(followedMp.memberId) },
                        onUnfollow = { onUnfollow(followedMp.memberId) }
                    )
                }
            }
        }
    }
}

/**
 * Empty hint shown on framework tabs (Parties, Sources, Tags) that have no
 * followed entities yet.
 */
@Composable
private fun TabEmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PartiesTabContent(
    parties: List<FollowedPartyUi>,
    onNavigateToParty: (Int) -> Unit,
    onUnfollow: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (parties.isEmpty()) {
        TabEmptyHint("No followed parties yet", modifier)
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.medium
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            gridItems(
                items = parties,
                key = { "party-${it.partyId}" }
            ) { party ->
                Box {
                    PartyCardShared(
                        partyId = party.partyId,
                        partyName = party.partyName,
                        partyBackgroundColour = party.partyBackgroundColour,
                        seats = party.seats,
                        selected = false,
                        onClick = { onNavigateToParty(party.partyId) }
                    )
                    // Unfollow icon overlay — top-end, circular background
                    // for visibility against the party-colored card.
                    IconButton(
                        onClick = { onUnfollow(party.partyId) },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PersonRemove,
                            contentDescription = "Unfollow ${party.partyName}",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FollowedMpCard(
    followedMp: FollowedMpUi,
    onClick: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MpAvatar(
                thumbnailUrl = followedMp.thumbnailUrl,
                displayName = followedMp.displayName,
                partyColorHex = followedMp.partyBackgroundColour,
                size = 48.dp,
                borderWidth = 1.dp
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = followedMp.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Party abbreviation · Constituency (directory style)
                Text(
                    text = "${followedMp.partyAbbreviation} · ${followedMp.constituencyName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // Recent vote info
                if (followedMp.recentVoteType != null && followedMp.recentDivisionTitle != null) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        VoteBadge(voteType = followedMp.recentVoteType)
                        Text(
                            text = followedMp.recentDivisionTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Unfollow icon button — replaces swipe-to-dismiss (issue #11).
            IconButton(
                onClick = onUnfollow,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonRemove,
                    contentDescription = "Unfollow ${followedMp.displayName}",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * Rounded-square vote badge — same style as DivisionDetailScreen.
 * Green (Aye) / Red (No) using VoteColors for theme-awareness.
 */
@Composable
private fun VoteBadge(voteType: String) {
    val (label, color) = when (voteType.uppercase()) {
        "AYE", "AYEVOTE" -> "Aye" to VoteColors.aye
        "NO", "NOVOTE" -> "No" to VoteColors.no
        else -> "—" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(color),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Grid card for followed MPs — compact version without overflow menu.
 */
@Composable
private fun FollowedMpGridCard(followedMp: FollowedMpUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val partyColor = com.goveye.app.ui.theme.parsePartyColor(followedMp.partyBackgroundColour)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = partyColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MpAvatar(
                    thumbnailUrl = followedMp.thumbnailUrl,
                    displayName = followedMp.displayName,
                    partyColorHex = followedMp.partyBackgroundColour,
                    size = 40.dp,
                    borderWidth = 1.dp
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = followedMp.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${followedMp.partyAbbreviation} · ${followedMp.constituencyName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            // Recent vote
            if (followedMp.recentVoteType != null && followedMp.recentDivisionTitle != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    VoteBadge(voteType = followedMp.recentVoteType)
                    Text(
                        text = followedMp.recentDivisionTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
