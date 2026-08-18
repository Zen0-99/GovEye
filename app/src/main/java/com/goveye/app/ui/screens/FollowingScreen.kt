package com.goveye.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
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
import com.goveye.app.ui.screens.following.FollowedMpUi
import com.goveye.app.ui.screens.following.FollowingUiState
import com.goveye.app.ui.screens.following.FollowingViewModel
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor

/**
 * Following tab — FotMob roster style (D-03).
 *
 * Shows a list of followed MPs with their most recent vote. Tapping a card
 * opens the MP's profile. Long-press or overflow menu allows unfollow/mute.
 */
@Composable
fun FollowingScreen(
    onNavigateToProfile: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: FollowingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Configure the global search bar for filtering followed MPs
    com.goveye.app.ui.components.ConfigureSearchBar(
        config = com.goveye.app.ui.components.SearchBarConfig(
            isVisible = true,
            query = uiState.searchQuery,
            placeholder = "Search followed MPs…",
            onQueryChange = viewModel::updateSearchQuery,
        ),
    )

    when {
        uiState.isLoading -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        uiState.followedMps.isEmpty() && uiState.searchQuery.isBlank() -> {
            // Empty state — no followed MPs yet
            Box(
                modifier = modifier.fillMaxSize().padding(MaterialTheme.padding.large),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Following\n\nFollow MPs to track their votes and activity.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }

        uiState.followedMps.isEmpty() -> {
            // No search results
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No followed MPs match \"${uiState.searchQuery}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        else -> {
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = uiState.followedMps,
                    key = { it.memberId },
                ) { followedMp ->
                    FollowedMpCard(
                        followedMp = followedMp,
                        onClick = { onNavigateToProfile(followedMp.memberId) },
                        onUnfollow = { viewModel.unfollow(followedMp.memberId) },
                        onToggleMute = { viewModel.toggleMute(followedMp.memberId, followedMp.isMuted) },
                    )
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
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val partyColor = parsePartyColor(followedMp.partyBackgroundColour)
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MpAvatar(
                thumbnailUrl = followedMp.thumbnailUrl,
                displayName = followedMp.displayName,
                partyColorHex = followedMp.partyBackgroundColour,
                size = 48.dp,
                borderWidth = 1.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = followedMp.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (followedMp.isMuted) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsOff,
                            contentDescription = "Muted",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
                // Party dot + name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(partyColor),
                    )
                    Text(
                        text = followedMp.partyName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                // Recent vote info
                if (followedMp.recentVoteType != null && followedMp.recentDivisionTitle != null) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        VoteBadge(voteType = followedMp.recentVoteType)
                        Text(
                            text = followedMp.recentDivisionTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(if (followedMp.isMuted) "Unmute notifications" else "Mute notifications") },
                        onClick = {
                            showMenu = false
                            onToggleMute()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Unfollow") },
                        onClick = {
                            showMenu = false
                            onUnfollow()
                        },
                    )
                }
            }
        }
    }
}

/**
 * Rounded square vote badge (36×22dp) — same style as DivisionDetailScreen.
 * Green = Aye, Red = No.
 */
@Composable
private fun VoteBadge(voteType: String) {
    val (color, label) = when (voteType.uppercase()) {
        "AYE", "AYEVOTE" -> MaterialTheme.colorScheme.primary to "Aye"
        "NO", "NOVOTE" -> MaterialTheme.colorScheme.error to "No"
        else -> MaterialTheme.colorScheme.outline to "—"
    }
    Surface(
        color = color,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.size(width = 36.dp, height = 22.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
