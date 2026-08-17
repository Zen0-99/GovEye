package com.goveye.app.ui.screens.mpprofile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.goveye.app.ui.theme.LocalPartyAccent
import com.goveye.app.ui.theme.partyAccentColorScheme
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parsePartyColor

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
    VOTES("Votes"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { ProfileTab.entries.size })
    val coroutineScope = rememberCoroutineScope()
    val isCollapsed = scrollBehavior.state.collapsedFraction > 0.5f

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
        if (uiState.isLoading && uiState.mp == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.mp == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text("MP not found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val mp = uiState.mp!!
            val partyColor = remember(mp) { parsePartyColor(mp.party?.backgroundColour) }
            val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
            val partyScheme = partyAccentColorScheme(partyColor, isDark)

            CompositionLocalProvider(LocalPartyAccent provides partyColor) {
                MaterialTheme(colorScheme = partyScheme) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Fixed header — does not scroll
                            ProfileHeader(
                                mp = mp,
                                onBack = onBack,
                            )

                            // Fixed tabs — indicator and selected text use party color
                            TabRow(
                                selectedTabIndex = pagerState.currentPage,
                                containerColor = MaterialTheme.colorScheme.surface,
                            ) {
                                ProfileTab.entries.forEachIndexed { index, tab ->
                                    Tab(
                                        selected = pagerState.currentPage == index,
                                        onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = { Text(tab.title) },
                            )
                        }
                    }

                    // Pager — each page has its own scrollable LazyColumn
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                    ) { page ->
                        when (ProfileTab.entries[page]) {
                            ProfileTab.PROFILE -> ProfileTabContent(
                                mp = mp,
                                synopsis = uiState.synopsis,
                                contacts = uiState.contacts,
                                samePartyMps = uiState.samePartyMps,
                                committeePeerMps = uiState.committeePeerMps,
                                onNavigateToProfile = onNavigateToProfile,
                            )
                            ProfileTab.CAREER -> CareerTabContent(
                                experiences = uiState.experiences,
                            )
                            ProfileTab.COMMITTEES -> CommitteesTabContent(
                                committees = uiState.committees,
                            )
                            ProfileTab.VOTES -> VotesTabContent()
                        }
                    }
                }

                // Animated collapsed header — fades in when scrolled
                AnimatedVisibility(
                    visible = isCollapsed,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 3.dp,
                    ) {
                        TopAppBar(
                            title = { Text(mp.nameDisplayAs) },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ArrowBack,
                                        contentDescription = "Back",
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        }
        }
    }
    }
}

@Composable
private fun ProfileTabContent(
    mp: com.goveye.app.domain.model.Mp,
    synopsis: String?,
    contacts: List<com.goveye.app.domain.model.Contact>,
    samePartyMps: List<com.goveye.app.domain.model.Mp>,
    committeePeerMps: List<com.goveye.app.domain.model.Mp>,
    onNavigateToProfile: (Int) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item {
            ProfileStatsCard(
                mp = mp,
            )
        }
        item { BioSection(synopsis = synopsis) }
        item { ContactSection(contacts = contacts) }
        item {
            RelatedMpsSection(
                samePartyMps = samePartyMps,
                committeePeerMps = committeePeerMps,
                onNavigateToProfile = onNavigateToProfile,
            )
        }
    }
}

@Composable
private fun CareerTabContent(
    experiences: List<com.goveye.app.domain.model.BiographyExperience>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item { CareerTimelineSection(experiences = experiences) }
    }
}

@Composable
private fun CommitteesTabContent(
    committees: List<com.goveye.app.domain.model.Committee>,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item { CommitteeChipsSection(committees = committees) }
    }
}

@Composable
private fun VotesTabContent() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 20.dp),
    ) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Voting data will be available in a future update.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
