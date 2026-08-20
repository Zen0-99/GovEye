package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.goveye.app.ui.theme.padding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestBucketDetailScreen(
    memberId: Int,
    bucketLabel: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

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

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(bucketLabel) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (bucketInterests.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No interests in this category",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(
                    horizontal = MaterialTheme.padding.medium,
                    vertical = MaterialTheme.padding.medium,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            ) {
                groupedByCategory.forEach { (categoryName, entries) ->
                    item {
                        CategoryGroupHeader(
                            categoryName = categoryName,
                            entryCount = entries.size,
                            totalPence = entries.sumOf { it.parsedAmountPence ?: 0L },
                        )
                    }
                    items(entries, key = { it.id }) { interest ->
                        InterestEntryRow(interest = interest)
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupHeader(
    categoryName: String,
    entryCount: Int,
    totalPence: Long,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "$entryCount entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (totalPence > 0) {
                Text(
                    text = formatPencePublic(totalPence),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun InterestEntryRow(
    interest: Interest,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = interest.summary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            interest.publishedDate?.let { date ->
                Text(
                    text = formatDateShort(date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            interest.parsedAmountPence?.let { pence ->
                Text(
                    text = formatPencePublic(pence),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

private fun formatDateShort(dateString: String): String =
    runCatching {
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
