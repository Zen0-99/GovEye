package com.goveye.app.ui.screens.feed

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.SearchBarConfig

/**
 * Detail screen for a piece of legislation (SI or Act).
 *
 * Displays the legislation title, type, year, number, date, and a link
 * to the full text on legislation.gov.uk. Follows the [BillDetailScreen]
 * pattern — uses the shared [AnnouncementDetailViewModel] to load by ID.
 */
@Composable
fun LegislationDetailScreen(
    legislationId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnnouncementDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(legislationId) {
        viewModel.loadLegislation(legislationId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            placeholder = "Legislation detail",
            onBack = onBack
        )
    )

    if (state.isLoading && state.legislation == null) {
        DelayedSpinner(modifier = modifier)
    } else if (state.legislation == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Legislation not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val legislation = state.legislation!!
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Title + type + year + number + date + globe icon
            item {
                val context = LocalContext.current
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = legislation.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(legislation.url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = "View on legislation.gov.uk",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatLegislationType(legislation.type),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${legislation.year} No. ${legislation.number}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (legislation.date.isNotBlank()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Legislation",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatDivisionDate(legislation.date),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tags — below title, no heading
            if (state.tags.isNotEmpty()) {
                item {
                    TagPillRow(tags = state.tags, onTagClick = {})
                }
            }

            // Full body text (scraped from legislation.gov.uk XML)
            val bodyText = legislation.bodyText
            if (!bodyText.isNullOrBlank()) {
                item {
                    DetailSectionCard(title = "Full text", body = bodyText)
                }
            }
        }
    }
}
