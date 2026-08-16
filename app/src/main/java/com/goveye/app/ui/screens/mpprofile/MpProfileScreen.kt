package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.screens.directory.MpAvatar
import com.goveye.app.ui.theme.padding
import com.goveye.app.ui.theme.parseMutedPartyColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MpProfileScreen(
    memberId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MpProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.observeProfile(memberId).collectAsStateWithLifecycle(MpProfileUiState())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.mp?.nameDisplayAs ?: "MP Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        modifier = modifier,
    ) { innerPadding ->
        if (uiState.isLoading) {
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
            val partyColor = parseMutedPartyColor(mp.party?.backgroundColour)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(MaterialTheme.padding.large),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            ) {
                MpAvatar(
                    thumbnailUrl = mp.thumbnailUrl,
                    displayName = mp.nameDisplayAs,
                    partyColorHex = mp.party?.backgroundColour,
                    size = 96.dp,
                )
                Text(
                    text = mp.nameDisplayAs,
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = mp.party?.name ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = partyColor,
                )
                Text(
                    text = mp.constituency?.name ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
