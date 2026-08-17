package com.goveye.app.ui.screens.mpprofile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.theme.padding
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack

private enum class ProfileTab(val title: String) {
    PROFILE("Profile"),
    CAREER("Career"),
    COMMITTEES("Committees"),
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

    Scaffold(modifier = modifier) { innerPadding ->
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    item {
                        ProfileHeader(
                            mp = uiState.mp!!,
                            onBack = onBack,
                        )
                    }
                    item {
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
                    }
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxWidth(),
                        ) { page ->
                            when (ProfileTab.entries[page]) {
                                ProfileTab.PROFILE -> {
                                    Column {
                                        ProfileStatsCard(
                                            mp = uiState.mp!!,
                                            age = uiState.age,
                                            votesRecorded = uiState.votesRecorded,
                                        )
                                        BioSection(synopsis = uiState.synopsis)
                                        ContactSection(contacts = uiState.contacts)
                                        RelatedMpsSection(
                                            samePartyMps = uiState.samePartyMps,
                                            committeePeerMps = uiState.committeePeerMps,
                                            onNavigateToProfile = onNavigateToProfile,
                                        )
                                    }
                                }
                                ProfileTab.CAREER -> CareerTimelineSection(uiState.experiences)
                                ProfileTab.COMMITTEES -> CommitteeChipsSection(uiState.committees)
                            }
                        }
                    }
                }

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
                            title = { Text(uiState.mp?.nameDisplayAs ?: "") },
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
