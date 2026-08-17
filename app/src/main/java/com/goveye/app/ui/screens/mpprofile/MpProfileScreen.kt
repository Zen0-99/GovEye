package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text(uiState.mp?.nameDisplayAs ?: "") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                    ),
                    scrollBehavior = scrollBehavior,
                )
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    ProfileTab.entries.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = { Text(tab.title) },
                        )
                    }
                }
            }
        },
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                item { ProfileHeader(mp = uiState.mp!!) }

                when (ProfileTab.entries[selectedTab]) {
                    ProfileTab.PROFILE -> {
                        item { ProfileStatsCard(mp = uiState.mp!!) }
                        item { BioSection(synopsis = uiState.synopsis) }
                        item { ContactSection(contacts = uiState.contacts) }
                        item {
                            RelatedMpsSection(
                                samePartyMps = uiState.samePartyMps,
                                committeePeerMps = uiState.committeePeerMps,
                                onNavigateToProfile = onNavigateToProfile,
                            )
                        }
                    }
                    ProfileTab.CAREER -> {
                        item { CareerTimelineSection(experiences = uiState.experiences) }
                    }
                    ProfileTab.COMMITTEES -> {
                        item { CommitteeChipsSection(committees = uiState.committees) }
                    }
                }
            }
        }
    }
}
