package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.SyncStatusBanner
import com.goveye.app.ui.theme.padding

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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.mp?.nameDisplayAs ?: "MP Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
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
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = innerPadding,
            ) {
                item { ProfileHeader(mp = uiState.mp!!) }
                item { SyncStatusBanner(status = uiState.syncStatus) }
                item { FollowButtonPlaceholder() }
                item { BioSection(synopsis = uiState.synopsis) }
                item { ContactSection(contacts = uiState.contacts) }
                item { CommitteeChipsSection(committees = uiState.committees) }
                item { CareerTimelineSection(experiences = uiState.experiences) }
                item {
                    RelatedMpsSection(
                        samePartyMps = uiState.samePartyMps,
                        committeePeerMps = uiState.committeePeerMps,
                        onNavigateToProfile = onNavigateToProfile,
                    )
                }
            }
        }
    }
}

@Composable
private fun FollowButtonPlaceholder() {
    OutlinedButton(
        onClick = { /* Phase 6 */ },
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.padding.large, vertical = MaterialTheme.padding.small),
    ) {
        Text("Follow (coming soon)")
    }
}
