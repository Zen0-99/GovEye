package com.goveye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.directory.FilterBottomSheet
import com.goveye.app.ui.screens.directory.FilterTabType
import com.goveye.app.ui.screens.following.FollowedMpUi
import com.goveye.app.ui.screens.following.FollowingUiState
import com.goveye.app.ui.screens.following.FollowingViewModel
import com.goveye.app.ui.theme.padding

/**
 * Following tab — FotMob roster style (D-03).
 *
 * Shows a list of followed MPs with their most recent vote. Tapping a card
 * opens the MP's profile. Overflow menu allows unfollow/mute.
 *
 * Filter icon opens the same FilterBottomSheet as the directory (Party,
 * House, Status).
 */
@Composable
fun FollowingScreen(
    onNavigateToProfile: (Int) -> Unit = {},
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

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.followedMps.isEmpty() && uiState.searchQuery.isBlank() && !uiState.filterState.hasActiveFilters -> {
            // Empty state — no followed MPs yet
            Box(
                modifier = modifier.fillMaxSize().padding(MaterialTheme.padding.large),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Following\n\nFollow MPs to track their votes and activity.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        uiState.followedMps.isEmpty() -> {
            // No results (from search or filters)
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
            when (uiState.viewMode) {
                com.goveye.app.data.preference.DirectoryViewMode.LIST -> {
                    LazyColumn(
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = uiState.followedMps,
                            key = { it.memberId }
                        ) { followedMp ->
                            FollowedMpCard(
                                followedMp = followedMp,
                                onClick = { onNavigateToProfile(followedMp.memberId) },
                                onUnfollow = { viewModel.unfollow(followedMp.memberId) }
                            )
                        }
                    }
                }

                com.goveye.app.data.preference.DirectoryViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        gridItems(
                            items = uiState.followedMps,
                            key = { it.memberId }
                        ) { followedMp ->
                            FollowedMpGridCard(
                                followedMp = followedMp,
                                onClick = { onNavigateToProfile(followedMp.memberId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Filter bottom sheet — same as directory
    if (showFilterSheet) {
        FilterBottomSheet(
            distinctParties = uiState.distinctParties,
            filterState = uiState.filterState,
            tabType = FilterTabType.OFFICIALS,
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
private fun FollowedMpCard(
    followedMp: FollowedMpUi,
    onClick: () -> Unit,
    onUnfollow: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Text(
                        text = "···",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Unfollow") },
                        onClick = {
                            showMenu = false
                            onUnfollow()
                        }
                    )
                }
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
            .clickable(onClick = onClick),
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
