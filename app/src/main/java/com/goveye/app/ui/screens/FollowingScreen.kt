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
import androidx.compose.ui.text.style.TextAlign
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
            // Empty state — no followed entities yet
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
            // Sectioned list — Officials, Parties, Sources, Tags (D-14)
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- Officials section (existing followed MPs) ---
                item(key = "section-officials-header") {
                    SectionHeader("Officials")
                }
                if (uiState.followedMps.isEmpty()) {
                    item(key = "section-officials-empty") {
                        SectionEmptyHint("No followed MPs yet")
                    }
                } else {
                    items(
                        items = uiState.followedMps,
                        key = { "official-${it.memberId}" }
                    ) { followedMp ->
                        FollowedMpCard(
                            followedMp = followedMp,
                            onClick = { onNavigateToProfile(followedMp.memberId) },
                            onUnfollow = { viewModel.unfollow(followedMp.memberId) }
                        )
                    }
                }

                // --- Parties section (framework — follow data layer not yet implemented) ---
                item(key = "section-parties-header") {
                    SectionHeader("Parties")
                }
                item(key = "section-parties-empty") {
                    SectionEmptyHint("No followed parties yet")
                }

                // --- Sources section (framework — follow data layer not yet implemented) ---
                item(key = "section-sources-header") {
                    SectionHeader("Sources")
                }
                item(key = "section-sources-empty") {
                    SectionEmptyHint("No followed sources yet")
                }

                // --- Tags section (framework — follow data layer not yet implemented) ---
                item(key = "section-tags-header") {
                    SectionHeader("Tags")
                }
                item(key = "section-tags-empty") {
                    SectionEmptyHint("No followed tags yet")
                }
            }
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

/**
 * Section header for grouped followed entities (D-14).
 */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

/**
 * Empty hint for a followed entity section with no followed items.
 */
@Composable
private fun SectionEmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}
