package com.goveye.app.ui.screens.directory

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.screens.feed.FeedItem
import com.goveye.app.ui.screens.feed.UnifiedFeedCard

/**
 * Source type filter for the Government tab.
 */
enum class GovernmentSourceType(val label: String) {
    ALL("All"),
    PUBLICATIONS("Publications"),
    STATEMENTS("Statements"),
    LEGISLATION("Legislation")
}

/**
 * Government tab content — browse announcements by source type.
 *
 * Shows a source type filter (All / Publications / Statements / Legislation)
 * at the top, followed by a LazyColumn of announcement cards reusing the
 * Feed card components. Search via the global ConfigureSearchBar.
 *
 * Per UI-SPEC lines 329-338.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun GovernmentTabContent(
    searchQuery: String,
    sourceTypeFilter: GovernmentSourceType,
    publications: List<com.goveye.app.domain.model.GovernmentPublication>,
    statements: List<com.goveye.app.domain.model.WrittenStatement>,
    legislation: List<com.goveye.app.domain.model.Legislation>,
    isLoading: Boolean,
    onSourceTypeChange: (GovernmentSourceType) -> Unit,
    onPublicationClick: (Int) -> Unit = {},
    onStatementClick: (Int) -> Unit = {},
    onLegislationClick: (Int) -> Unit = {},
    onTagClick: (String) -> Unit = {}
) {
    Log.i(
        "GovEye/GovernmentTab",
        "GovernmentTabContent compose — searchQuery='$searchQuery' " +
            "sourceType=$sourceTypeFilter publications=${publications.size} " +
            "statements=${statements.size} legislation=${legislation.size} isLoading=$isLoading"
    )

    when {
        isLoading -> {
            com.goveye.app.ui.components.SkeletonScreen(
                cardType = com.goveye.app.ui.components.SkeletonCardType.FEED,
                itemCount = 4
            )
        }

        else -> {
            // Build filtered items based on source type filter + search query
            val filteredPublications = publications.filter { pub ->
                (
                    sourceTypeFilter == GovernmentSourceType.ALL ||
                        sourceTypeFilter == GovernmentSourceType.PUBLICATIONS
                    ) &&
                    (searchQuery.isBlank() || pub.title.contains(searchQuery, ignoreCase = true))
            }
            val filteredStatements = statements.filter { stmt ->
                (
                    sourceTypeFilter == GovernmentSourceType.ALL ||
                        sourceTypeFilter == GovernmentSourceType.STATEMENTS
                    ) &&
                    (searchQuery.isBlank() || stmt.title.contains(searchQuery, ignoreCase = true))
            }
            val filteredLegislation = legislation.filter { leg ->
                (
                    sourceTypeFilter == GovernmentSourceType.ALL ||
                        sourceTypeFilter == GovernmentSourceType.LEGISLATION
                    ) &&
                    (searchQuery.isBlank() || leg.title.contains(searchQuery, ignoreCase = true))
            }

            // Determine available source types for de-emphasis of empty types
            val hasPublications = publications.isNotEmpty()
            val hasStatements = statements.isNotEmpty()
            val hasLegislation = legislation.isNotEmpty()

            val totalItems = filteredPublications.size + filteredStatements.size + filteredLegislation.size

            if (totalItems == 0 && searchQuery.isBlank() && sourceTypeFilter == GovernmentSourceType.ALL) {
                // Empty state — no data at all
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No government announcements",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Government publications, statements, and legislation " +
                                "will appear here once the database is updated.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                        )
                    }
                }
            } else if (totalItems == 0 && searchQuery.isNotBlank()) {
                // No search results
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No announcements found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (totalItems == 0) {
                // No filter results
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No announcements match these filters",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Source type filter pills
                    item(key = "source-type-filter") {
                        SourceTypeFilterPills(
                            selected = sourceTypeFilter,
                            hasPublications = hasPublications,
                            hasStatements = hasStatements,
                            hasLegislation = hasLegislation,
                            onSourceTypeChange = onSourceTypeChange
                        )
                    }

                    // Publication cards
                    items(
                        items = filteredPublications,
                        key = { "publication-${it.id}" },
                        contentType = { "feed_publication" }
                    ) { publication ->
                        UnifiedFeedCard(
                            item = FeedItem.PublicationItem(publication = publication),
                            hasFollowedVotes = false,
                            onClick = { onPublicationClick(publication.id) },
                            onTagClick = onTagClick
                        )
                    }

                    // Statement cards
                    items(
                        items = filteredStatements,
                        key = { "statement-${it.id}" },
                        contentType = { "feed_statement" }
                    ) { statement ->
                        UnifiedFeedCard(
                            item = FeedItem.StatementItem(statement = statement),
                            hasFollowedVotes = false,
                            onClick = { onStatementClick(statement.id) },
                            onTagClick = onTagClick
                        )
                    }

                    // Legislation cards
                    items(
                        items = filteredLegislation,
                        key = { "legislation-${it.id}" },
                        contentType = { "feed_legislation" }
                    ) { legislation ->
                        UnifiedFeedCard(
                            item = FeedItem.LegislationItem(legislation = legislation),
                            hasFollowedVotes = false,
                            onClick = { onLegislationClick(legislation.id) },
                            onTagClick = onTagClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Source type filter pills — SegmentedPill style inline filter.
 * Filter pills for absent types are visually de-emphasized (onSurfaceVariant at 0.4 alpha).
 */
@Composable
private fun SourceTypeFilterPills(
    selected: GovernmentSourceType,
    hasPublications: Boolean,
    hasStatements: Boolean,
    hasLegislation: Boolean,
    onSourceTypeChange: (GovernmentSourceType) -> Unit,
    modifier: Modifier = Modifier
) {
    val containerShape = RoundedCornerShape(999.dp)
    val segmentShape = RoundedCornerShape(999.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(containerShape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        GovernmentSourceType.entries.forEach { sourceType ->
            val isSelected = selected == sourceType
            val isAvailable = when (sourceType) {
                GovernmentSourceType.ALL -> true
                GovernmentSourceType.PUBLICATIONS -> hasPublications
                GovernmentSourceType.STATEMENTS -> hasStatements
                GovernmentSourceType.LEGISLATION -> hasLegislation
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(segmentShape)
                    .then(
                        if (isSelected) {
                            Modifier.background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        } else {
                            Modifier
                        }
                    )
                    .clickable { onSourceTypeChange(sourceType) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = sourceType.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else if (!isAvailable) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}
