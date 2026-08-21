package com.goveye.app.ui.screens.party

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.PersonRemove
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DetailTopBarAction
import com.goveye.app.ui.components.DetailTopBarConfig
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor
import com.goveye.app.ui.utils.partyLogoResId
import kotlinx.coroutines.launch

enum class PartyTab(val label: String) {
    INFO("Info"),
    MEMBERS("Members"),
    STATS("Stats"),
    MANIFESTO("Manifesto")
}

@Composable
fun PartyScreen(
    partyId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: PartyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(partyId) {
        viewModel.loadParty(partyId)
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { PartyTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope()

    val party = uiState.party
    val partyColor = remember(party) { parsePartyColor(party?.partyBackgroundColour) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)

    // Configure the shell's detail top bar — same pattern as ProfileScreen
    ConfigureDetailTopBar(
        config = DetailTopBarConfig(
            title = "",
            onBack = onBack,
            iconTint = if (party != null) headerIconTint else null,
            actions = if (party != null) {
                listOf(
                    DetailTopBarAction(
                        icon = if (uiState.notificationsEnabled) {
                            Icons.Outlined.NotificationsActive
                        } else {
                            Icons.Outlined.Notifications
                        },
                        contentDescription = "Notification settings",
                        onClick = { /* TODO: party notification settings */ },
                        tint = headerIconTint
                    ),
                    DetailTopBarAction(
                        icon = if (uiState.isFollowing) {
                            Icons.Outlined.PersonRemove
                        } else {
                            Icons.Outlined.PersonAdd
                        },
                        contentDescription = if (uiState.isFollowing) "Unfollow" else "Follow",
                        onClick = { viewModel.toggleFollow(partyId) },
                        tint = headerIconTint
                    )
                )
            } else {
                emptyList()
            }
        )
    )

    if (uiState.isLoading && party == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.CircularProgressIndicator()
        }
    } else if (party == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Party not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Gradient header — same pattern as ProfileContentHeader
            PartyContentHeader(
                partyName = party.partyName,
                seats = party.seats,
                partyColor = partyColor,
                partyId = party.partyId,
                isDark = isDark,
                contentTopPadding = contentTopPadding
            )

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 16.dp
            ) {
                PartyTab.entries.forEachIndexed { index, tab ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch { pagerState.animateScrollToPage(index) }
                        },
                        text = {
                            Text(
                                text = tab.label,
                                color = if (pagerState.currentPage == index) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    )
                }
            }

            // Pager content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { page ->
                when (PartyTab.entries[page]) {
                    PartyTab.INFO -> PartyInfoTab(
                        party = uiState.party,
                        stats = uiState.stats
                    )

                    PartyTab.MEMBERS -> {
                        val pagedMps = remember(partyId) {
                            viewModel.getPagedMps(partyId)
                        }
                        PartyMembersTab(
                            pagedMps = pagedMps,
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }

                    PartyTab.STATS -> PartyStatsTab(
                        party = uiState.party,
                        stats = uiState.stats
                    )

                    PartyTab.MANIFESTO -> {
                        val searchQuery by viewModel.manifestoSearchQuery.collectAsStateWithLifecycle()
                        val searchResults by viewModel.manifestoSearchResults.collectAsStateWithLifecycle()
                        PartyManifestoTab(
                            manifesto = uiState.manifesto,
                            searchQuery = searchQuery,
                            searchResults = searchResults,
                            onSearchQueryChange = viewModel::updateManifestoSearchQuery,
                            fullManifestoText = uiState.manifesto?.manifestoText
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PartyContentHeader(
    partyName: String,
    seats: Int,
    partyColor: Color,
    partyId: Int,
    isDark: Boolean,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        partyColor.copy(alpha = if (isDark) 0.85f else 0.7f),
                        partyColor.copy(alpha = if (isDark) 0.3f else 0.2f),
                        Color.Transparent
                    )
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = MaterialTheme.padding.medium)
                .padding(top = contentTopPadding + MaterialTheme.padding.small, bottom = MaterialTheme.padding.small),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium)
        ) {
            // Party logo
            partyLogoResId(partyId)?.let { resId ->
                androidx.compose.foundation.Image(
                    painter = painterResource(resId),
                    contentDescription = partyName,
                    modifier = Modifier.size(60.dp)
                )
            } ?: Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = partyColor.copy(alpha = if (isDark) 0.9f else 0.85f),
                    modifier = Modifier.size(60.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = partyName.take(2).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = headerTextColor
                        )
                    }
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = partyName,
                    style = MaterialTheme.typography.titleLarge,
                    color = headerTextColor,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$seats MP${if (seats != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = headerTextColor.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
