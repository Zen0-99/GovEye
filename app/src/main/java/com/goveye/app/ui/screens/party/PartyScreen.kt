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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.DetailTopBarAction
import com.goveye.app.ui.components.DetailTopBarConfig
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.components.SubTab
import com.goveye.app.ui.components.SubTabPager
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.partyColorForId
import com.goveye.app.ui.utils.partyLogoResId

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
    onNavigateToProfile: (Int, com.goveye.app.ui.navigation.MpHeaderFallback?) -> Unit,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: PartyViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(partyId) {
        viewModel.loadParty(partyId)
    }

    // Track the current tab index — needed for search bar config.
    var currentPage by remember { mutableIntStateOf(0) }

    val party = uiState.party
    val partyColor = remember(party) { partyColorForId(party?.partyId, party?.partyBackgroundColour) }
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    val headerIconTint = if (isDark) Color.White else Color(0xFF1A1A1A)

    // Configure the shell's detail top bar — same pattern as ProfileScreen
    ConfigureDetailTopBar(
        config = DetailTopBarConfig(
            title = "",
            onBack = onBack,
            accentColor = if (party != null) partyColor else null,
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

    // Wire the global search bar to manifesto search when on the Manifesto tab.
    val manifestoSearchQuery by viewModel.manifestoSearchQuery.collectAsStateWithLifecycle()
    val isOnManifestoTab = currentPage == PartyTab.entries.indexOf(PartyTab.MANIFESTO)
    ConfigureSearchBar(
        config = SearchBarConfig(
            query = if (isOnManifestoTab) manifestoSearchQuery else "",
            onQueryChange = if (isOnManifestoTab) {
                viewModel::updateManifestoSearchQuery
            } else {
                {}
            },
            placeholder = if (isOnManifestoTab) "Search this manifesto…" else "Search…"
        )
    )

    if (uiState.isLoading && party == null) {
        DelayedSpinner(modifier = modifier)
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
                partyAbbreviation = party.partyAbbreviation,
                seats = party.seats,
                partyColor = partyColor,
                partyId = party.partyId,
                isDark = isDark,
                contentTopPadding = contentTopPadding
            )

            SubTabPager(
                tabs = PartyTab.entries.map { SubTab(label = it.label) },
                onPageChange = { currentPage = it },
                modifier = Modifier.fillMaxWidth()
            ) { page ->
                when (PartyTab.entries[page]) {
                    PartyTab.INFO -> PartyInfoTab(
                        party = uiState.party,
                        stats = uiState.stats,
                        leader = uiState.leader,
                        onNavigateToProfile = onNavigateToProfile
                    )

                    PartyTab.MEMBERS -> {
                        val pagedMps = remember(partyId) {
                            viewModel.getPagedMps(partyId)
                        }
                        PartyMembersTab(
                            pagedMps = pagedMps,
                            partyName = uiState.party?.partyName,
                            partyColor = uiState.party?.partyBackgroundColour,
                            onNavigateToProfile = onNavigateToProfile
                        )
                    }

                    PartyTab.STATS -> PartyStatsTab(
                        party = uiState.party,
                        stats = uiState.stats
                    )

                    PartyTab.MANIFESTO -> {
                        val searchResults by viewModel.manifestoSearchResults.collectAsStateWithLifecycle()
                        PartyManifestoTab(
                            manifesto = uiState.manifesto,
                            searchQuery = manifestoSearchQuery,
                            searchResults = searchResults,
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
    partyAbbreviation: String,
    seats: Int,
    partyColor: Color,
    partyId: Int,
    isDark: Boolean,
    contentTopPadding: Dp,
    modifier: Modifier = Modifier
) {
    val headerTextColor = if (isDark) Color.White else Color(0xFF1A1A1A)

    // Gradient is rendered at the shell level (GovEyeApp) so it fades
    // in/out smoothly across navigation transitions. This Box is
    // transparent — the shell-level gradient shows through.
    Box(
        modifier = modifier
            .fillMaxWidth()
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
                    text = if (partyAbbreviation.isNotBlank()) {
                        "$partyName ($partyAbbreviation)"
                    } else {
                        partyName
                    },
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
