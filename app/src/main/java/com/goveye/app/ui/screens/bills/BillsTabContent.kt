package com.goveye.app.ui.screens.bills

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.domain.model.Bill
import com.goveye.app.domain.model.SyncStatus
import com.goveye.app.ui.components.StickyInfoCard
import com.goveye.app.ui.components.SyncStatusBanner
import com.goveye.app.ui.theme.padding

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BillsTabContent(
    onNavigateToBill: (Int) -> Unit,
    searchQuery: String = "",
    showInfoCards: Boolean = true,
    modifier: Modifier = Modifier,
    viewModel: BillBrowseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(searchQuery) {
        viewModel.setSearchQuery(searchQuery)
    }

    // Show search results when searching, otherwise all bills
    val displayBills = if (searchQuery.isNotBlank()) state.searchResults else state.bills

    Column(modifier = modifier.fillMaxSize()) {
        if (state.syncStatus != SyncStatus.FRESH && state.bills.isNotEmpty()) {
            SyncStatusBanner(status = state.syncStatus)
        }

        when {
            state.isLoading && displayBills.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            displayBills.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotBlank()) "No bills match \"$searchQuery\"" else "No bills found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        bottom = MaterialTheme.padding.small
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (showInfoCards) {
                        stickyHeader(key = "tab-info") {
                            StickyInfoCard(
                                title = "Bills",
                                subtitle = "Track legislation as it moves through Parliament."
                            )
                        }
                    }
                    items(displayBills, key = { it.id }, contentType = { "bill_card" }) { bill ->
                        BillCard(
                            bill = bill,
                            onClick = { onNavigateToBill(bill.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillCard(bill: Bill, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = bill.shortTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (bill.isAct) {
                    BillStatusChip(text = "Act", color = MaterialTheme.colorScheme.primaryContainer)
                }
                BillStatusChip(
                    text = bill.currentHouse,
                    color = MaterialTheme.colorScheme.secondaryContainer
                )
                bill.currentStage?.description?.let { stage ->
                    BillStatusChip(
                        text = stage,
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun BillStatusChip(text: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
