package com.goveye.app.ui.screens.party

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.data.local.dao.ManifestoSearchResult
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.ui.theme.padding

@Composable
fun PartyManifestoTab(
    manifesto: PartyManifestoEntity?,
    searchQuery: String,
    searchResults: List<ManifestoSearchResult>,
    onSearchQueryChange: (String) -> Unit,
    fullManifestoText: String?,
    modifier: Modifier = Modifier
) {
    if (manifesto == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No manifesto available",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // Parse the manifesto text into blocks once
    val blocks = remember(manifesto.manifestoText) {
        ManifestoParser.parse(manifesto.manifestoText)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        )
    ) {
        // Search field
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Search this manifesto…") },
                modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.padding.medium),
                singleLine = true
            )
        }

        if (searchQuery.isBlank()) {
            // Title
            item {
                Text(
                    text = "${manifesto.manifestoYear} Manifesto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
                )
                Text(
                    text = "${manifesto.wordCount} words",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.padding.medium)
                )
            }

            // Render parsed blocks
            items(blocks.size) { index ->
                ManifestoBlockRenderer(blocks[index])
            }
        } else if (searchResults.isEmpty()) {
            // Empty state
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = MaterialTheme.padding.large),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No results for '$searchQuery' in this manifesto",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Results list
            item {
                Text(
                    text = "${searchResults.size} results",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
                )
            }
            items(searchResults, key = { it.partyId * 100000 + it.snippetText.hashCode() }) { result ->
                var isExpanded by remember { mutableStateOf(false) }
                ManifestoSearchResultItem(
                    result = result,
                    query = searchQuery,
                    isExpanded = isExpanded,
                    onToggleExpand = { isExpanded = !isExpanded },
                    fullText = fullManifestoText
                )
            }
        }
    }
}

@Composable
private fun ManifestoBlockRenderer(block: ManifestoBlock) {
    when (block) {
        is ManifestoBlock.Heading -> {
            Text(
                text = block.text,
                style = if (block.level == 1) {
                    MaterialTheme.typography.titleLarge
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    top = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small
                )
            )
        }

        is ManifestoBlock.Paragraph -> {
            Text(
                text = block.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = MaterialTheme.padding.small)
            )
        }

        is ManifestoBlock.Bullet -> {
            Text(
                text = "•  ${block.text}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = MaterialTheme.padding.medium,
                    bottom = MaterialTheme.padding.small
                )
            )
        }

        is ManifestoBlock.PageBreak -> {
            HorizontalDivider(
                modifier = Modifier.padding(
                    vertical = MaterialTheme.padding.small
                ),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
