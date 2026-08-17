package com.goveye.app.ui.screens.mpprofile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MpProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val pagerState = rememberPagerState(pageCount = { ProfileTab.entries.size })
    val coroutineScope = rememberCoroutineScope()

    val scrolledFraction = scrollBehavior.state.collapsedFraction
    val headerAlpha by animateFloatAsState(targetValue = scrolledFraction, label = "headerAlpha")
    val contentColor = lerp(Color.White, MaterialTheme.colorScheme.onSurface, headerAlpha)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (headerAlpha > 0.5f) {
                        Text(uiState.mp?.nameDisplayAs ?: "")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = contentColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = lerp(Color.Transparent, MaterialTheme.colorScheme.surface, headerAlpha),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = contentColor,
                ),
                scrollBehavior = scrollBehavior,
            )
        },
        modifier = modifier,
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                ) {
                    // Profile header — scrolls away, TopAppBar fades in
                    item { ProfileHeader(mp = uiState.mp!!) }

                    // Tabs — pinned below header
                    item {
                        TabRow(
                            selectedTabIndex = pagerState.currentPage,
                            containerColor = MaterialTheme.colorScheme.surface,
                        ) {
                            ProfileTab.entries.forEachIndexed { index, tab ->
                                Tab(
                                    selected = pagerState.currentPage == index,
                                    onClick = {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    },
                                    text = { Text(tab.title) },
                                )
                            }
                        }
                    }

                    // Tab content via HorizontalPager
                    item {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            when (ProfileTab.entries[page]) {
                                ProfileTab.PROFILE -> {
                                    Column {
                                        ProfileStatsCard(mp = uiState.mp!!)
                                        BioSection(synopsis = uiState.synopsis)
                                        ContactSection(contacts = uiState.contacts)
                                        RelatedMpsSection(
                                            samePartyMps = uiState.samePartyMps,
                                            committeePeerMps = uiState.committeePeerMps,
                                            onNavigateToProfile = onNavigateToProfile,
                                        )
                                    }
                                }
                                ProfileTab.CAREER -> {
                                    CareerTimelineSection(experiences = uiState.experiences)
                                }
                                ProfileTab.COMMITTEES -> {
                                    CommitteeChipsSection(committees = uiState.committees)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
