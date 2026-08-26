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
import coil3.compose.AsyncImage
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.DelayedSpinner
import com.goveye.app.ui.components.SearchBarConfig

/**
 * Shared section card for announcement detail screens — a titled surface
 * card with body text. Used by [PublicationDetailScreen] and
 * [StatementDetailScreen].
 */
@Composable
internal fun DetailSectionCard(title: String, body: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Detail screen for a government publication (GOV.UK Content API).
 *
 * Displays the publication title, organisation, first-published date,
 * summary, image (if available), tags, and a link to the full document
 * on GOV.UK. Follows the [BillDetailScreen] pattern — uses the shared
 * [AnnouncementDetailViewModel] to load by ID.
 */
@Composable
fun PublicationDetailScreen(
    publicationId: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AnnouncementDetailViewModel = hiltViewModel()
) {
    LaunchedEffect(publicationId) {
        viewModel.loadPublication(publicationId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    ConfigureSearchBar(
        config = SearchBarConfig(
            isVisible = true,
            placeholder = "Publication detail",
            onBack = onBack
        )
    )

    if (state.isLoading && state.publication == null) {
        DelayedSpinner(modifier = modifier)
    } else if (state.publication == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Publication not found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        val publication = state.publication!!
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Image (if available)
            if (!publication.imageUrl.isNullOrBlank()) {
                item {
                    AsyncImage(
                        model = publication.imageUrl,
                        contentDescription = publication.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 4.dp)
                    )
                }
            }

            // Title + organisation + date + globe icon
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
                            text = publication.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(publication.url))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Public,
                                contentDescription = "View on GOV.UK",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        text = publication.organisation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Publication",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "·",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDivisionDate(publication.firstPublishedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Tags — below title, no heading
            if (state.tags.isNotEmpty()) {
                item {
                    TagPillRow(tags = state.tags, onTagClick = {})
                }
            }

            // Summary
            if (publication.summary.isNotBlank()) {
                item {
                    DetailSectionCard(title = "Summary", body = publication.summary)
                }
            }

            // Full body text (HTML-stripped plain text from build_gov_publications.py)
            val bodyText = publication.bodyText
            if (!bodyText.isNullOrBlank()) {
                item {
                    DetailSectionCard(title = "Full text", body = bodyText)
                }
            }

            // Tags — below title, no heading (moved to right after title block)
            // Tags shown after summary/full text
        }
    }
}
