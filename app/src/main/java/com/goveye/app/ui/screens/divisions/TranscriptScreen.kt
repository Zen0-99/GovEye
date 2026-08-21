package com.goveye.app.ui.screens.divisions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.DebateSpeech
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.SearchBarConfig

@Composable
fun TranscriptScreen(
    divisionId: Int,
    divisionTitle: String,
    onBack: () -> Unit,
    onNavigateToProfile: (Int) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TranscriptViewModel = hiltViewModel()
) {
    LaunchedEffect(divisionId) {
        viewModel.load(divisionId, divisionTitle)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    // Configure the global search bar with back button + title
    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            query = "",
            placeholder = divisionTitle,
            onQueryChange = { },
            onBack = onBack,
            isSearchActive = false,
            onSearchActiveChange = { },
            segments = emptyList()
        )
    )

    if (state.isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (state.speeches.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No transcript available for this division.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 16.dp,
            vertical = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(state.speeches, key = { it.speechGid }) { speech ->
            SpeechCard(
                speech = speech,
                speakerInfo = state.speakerInfo[speech.memberId],
                onNavigateToProfile = onNavigateToProfile
            )
        }
    }
}

@Composable
private fun SpeechCard(
    speech: DebateSpeech,
    speakerInfo: com.goveye.app.data.local.entity.MpEntity?,
    onNavigateToProfile: (Int) -> Unit
) {
    // Procedural blocks (no speaker) — render as a centered, muted note
    if (speech.speakerName.isBlank()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow
        ) {
            Text(
                text = speech.speechText,
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        return
    }

    val isIntervention = speech.isIntervention

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isIntervention) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speaker row — avatar + name + party
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (speech.memberId > 0 && speakerInfo != null) {
                    MpAvatar(
                        thumbnailUrl = speakerInfo.thumbnailUrl,
                        displayName = speakerInfo.nameDisplayAs,
                        partyColorHex = speakerInfo.partyBackgroundColour,
                        size = 36.dp,
                        modifier = Modifier.clickable { onNavigateToProfile(speech.memberId) }
                    )
                } else {
                    // Unmatched speaker — show initials circle
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = speech.speakerName.take(2).uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = speech.speakerName,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (speech.memberId > 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = if (speech.memberId > 0) {
                            Modifier.clickable { onNavigateToProfile(speech.memberId) }
                        } else {
                            Modifier
                        }
                    )
                    if (speakerInfo != null) {
                        Text(
                            text = speakerInfo.partyName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (speech.speakerPosition.isNotBlank()) {
                        Text(
                            text = speech.speakerPosition,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (isIntervention) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = "Intervention",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Speech text
            Text(
                text = speech.speechText,
                style = if (isIntervention) {
                    MaterialTheme.typography.bodySmall
                } else {
                    MaterialTheme.typography.bodyMedium
                },
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
