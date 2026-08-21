package com.goveye.app.ui.screens.party

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.theme.padding
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

    // Load party data when the screen is first composed
    androidx.compose.runtime.LaunchedEffect(partyId) {
        viewModel.loadParty(partyId)
    }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { PartyTab.entries.size }
    )
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = modifier.fillMaxSize().padding(top = contentTopPadding)
    ) {
        // Party header
        PartyHeader(
            partyName = uiState.party?.partyName ?: "Loading...",
            abbreviation = uiState.party?.partyAbbreviation ?: "",
            seats = uiState.party?.seats ?: 0,
            backgroundColor = uiState.party?.partyBackgroundColour,
            modifier = Modifier.fillMaxWidth()
        )

        // Tabs
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            edgePadding = 0.dp
        ) {
            PartyTab.entries.forEachIndexed { index, tab ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = { Text(tab.label) }
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
                    val pagingData by pagedMps.collectAsState(initial = androidx.paging.PagingData.empty())
                    PartyMembersTab(
                        pagedMps = pagingData,
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

@Composable
private fun PartyHeader(
    partyName: String,
    abbreviation: String,
    seats: Int,
    backgroundColor: String?,
    modifier: Modifier = Modifier
) {
    val bgColor = backgroundColor?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            null
        }
    } ?: MaterialTheme.colorScheme.primaryContainer

    androidx.compose.material3.Surface(
        color = bgColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.large),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = partyName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (abbreviation.isNotEmpty()) {
                Text(
                    text = abbreviation,
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            Text(
                text = "$seats MPs",
                style = MaterialTheme.typography.bodyMedium,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}
