package com.goveye.app.ui.screens.mpprofile

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Mp
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.parsePartyColor

/**
 * Shows all MPs who have a given tag, ranked by frequency + recency weighted
 * hitCount. Reachable from the Topics section on an MP's profile.
 */
@Composable
fun MpTagBrowseScreen(
    tag: String,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentTopPadding: Dp = 0.dp,
    viewModel: MpTagBrowseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(tag) {
        viewModel.load(tag)
    }

    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = tag,
            onBack = onBack
        )
    )

    if (uiState.isLoading) {
        DelayedSpinner(modifier = modifier)
    } else if (uiState.mps.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No MPs found for this topic",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            // Description header
            if (uiState.description.isNotBlank()) {
                Text(
                    text = uiState.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }

            Text(
                text = "${uiState.mps.size} MP${if (uiState.mps.size > 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.mps, key = { it.id }) { mp ->
                    MpTagBrowseRow(
                        mp = mp,
                        onClick = { onNavigateToProfile(mp.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MpTagBrowseRow(mp: Mp, onClick: () -> Unit) {
    val partyColor = parsePartyColor(mp.party?.backgroundColour)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MpAvatar(
            thumbnailUrl = mp.thumbnailUrl,
            displayName = mp.nameDisplayAs,
            partyColorHex = mp.party?.backgroundColour,
            size = 48.dp,
            borderWidth = 2.dp
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = mp.nameDisplayAs,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${mp.party?.abbreviation ?: mp.party?.name ?: ""} · ${mp.constituency?.name ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
