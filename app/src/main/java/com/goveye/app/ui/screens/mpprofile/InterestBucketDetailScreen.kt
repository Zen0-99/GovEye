package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Interest
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.theme.padding

@Composable
fun InterestBucketDetailScreen(
    memberId: Int,
    bucketLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    // Configure the shell's detail top bar with bucket label as title
    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = bucketLabel,
            onBack = onBack
        )
    )

    // Filter interests to the selected bucket
    val bucketInterests = remember(uiState.interests, bucketLabel) {
        uiState.interests.filter { it.bucket == bucketLabel }
    }

    // Group by API sub-category (categoryName), preserving categoryNumber for ordering
    val groupedByCategory = remember(bucketInterests) {
        bucketInterests
            .groupBy { it.categoryName }
            .toList()
            .sortedBy { (name, _) ->
                bucketInterests.firstOrNull { it.categoryName == name }?.categoryNumber ?: "99"
            }
    }

    if (bucketInterests.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No interests in this category",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.medium
            ),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            groupedByCategory.forEach { (categoryName, entries) ->
                // Inline heading — same pattern as feed date headings.
                // Not a card, just a text label that separates groups.
                item(key = "header_$categoryName") {
                    CategoryInlineHeader(
                        categoryName = categoryName,
                        entryCount = entries.size
                    )
                }
                items(entries, key = { it.id }) { interest ->
                    InterestEntryRow(interest = interest)
                }
            }
        }
    }
}

/**
 * Inline category heading — like the feed's date headers.
 * Simple text on surface color, not a card.
 */
@Composable
private fun CategoryInlineHeader(categoryName: String, entryCount: Int) {
    Text(
        text = "$categoryName  ·  $entryCount ${if (entryCount == 1) "entry" else "entries"}",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun InterestEntryRow(interest: Interest) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = interest.summary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            interest.publishedDate?.let { date ->
                Text(
                    text = formatDateShort(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            interest.parsedAmountPence?.let { pence ->
                Text(
                    text = formatPencePublic(pence),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatDateShort(dateString: String): String = runCatching {
    val parts = dateString.substring(0, 10).split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
}.getOrNull() ?: dateString

private fun formatPencePublic(pence: Long): String {
    val pounds = pence / 100.0
    return if (pence % 100 == 0L) {
        "£${"%,.0f".format(pounds)}"
    } else {
        "£${"%,.2f".format(pounds)}"
    }
}
