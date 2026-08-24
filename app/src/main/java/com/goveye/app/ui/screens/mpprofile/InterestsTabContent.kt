package com.goveye.app.ui.screens.mpprofile

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.domain.model.Interest
import com.goveye.app.ui.screens.feed.UnifiedFinancialCard
import com.goveye.app.ui.theme.padding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * The 10 interest buckets, matching the Register of Members' Financial
 * Interests categories 1–10. Must match the `bucket` column values
 * written by `build_interests.py`.
 */
private val BUCKET_ORDER = listOf(
    "Employment/Earnings",
    "Financial Support",
    "Gifts",
    "Overseas Visits",
    "Overseas Gifts",
    "Land/Property",
    "Shareholdings",
    "Miscellaneous",
    "Family Employed",
    "Family Lobbying"
)

private val BUCKET_ICONS: Map<String, ImageVector> = mapOf(
    "Employment/Earnings" to Icons.Outlined.Paid,
    "Financial Support" to Icons.Outlined.AccountBalance,
    "Gifts" to Icons.Outlined.CardGiftcard,
    "Overseas Visits" to Icons.Outlined.Flight,
    "Overseas Gifts" to Icons.Outlined.Public,
    "Land/Property" to Icons.Outlined.RealEstateAgent,
    "Shareholdings" to Icons.AutoMirrored.Outlined.TrendingUp,
    "Miscellaneous" to Icons.Outlined.Category,
    "Family Employed" to Icons.Outlined.Groups,
    "Family Lobbying" to Icons.Outlined.Business
)

@Composable
fun InterestsTabContent(
    memberId: Int,
    interests: List<Interest>,
    expenseBucketTotals: List<ExpenseBucketTotal> = emptyList(),
    onNavigateToBucketDetail: (String) -> Unit,
    onNavigateToExpenseBucket: (String) -> Unit = {},
    showFilterSheet: Boolean = false,
    onFilterSheetDismiss: () -> Unit = {},
    fromDate: String? = null,
    toDate: String? = null,
    onFromDateChange: (String?) -> Unit = {},
    onToDateChange: (String?) -> Unit = {},
    partyColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    // --- Date filter state is now lifted to the screen level so the
    // global search bar's filter icon can trigger the sheet and report
    // hasActiveFilters. The values are passed in from MpProfileScreen.

    // --- Monthly navigation state (D-07) ---
    // Default to the most recent month that has interests, or current month if empty
    val monthsWithData = remember(interests) { extractMonths(interests) }
    var selectedMonthIndex by remember(interests) {
        mutableStateOf(if (monthsWithData.isNotEmpty()) monthsWithData.lastIndex else 0)
    }

    // Apply date filter to the interests list
    val filteredInterests = remember(interests, fromDate, toDate) {
        val from = fromDate
        val to = toDate
        if (from == null && to == null) {
            interests
        } else {
            interests.filter { interest ->
                val date = interest.publishedDate
                date != null &&
                    (from == null || date >= from) &&
                    (to == null || date <= to)
            }
        }
    }

    if (interests.isEmpty() && expenseBucketTotals.isEmpty()) {
        // --- Empty state (R2) ---
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No registered financial interests",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    // If interests is empty but expenses exist, show only the expenses section
    if (interests.isEmpty()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.medium
            ),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
        ) {
            item(span = { GridItemSpan(2) }) {
                ExpenseSectionHeader()
            }
            items(expenseBucketTotals, key = { it.bucket }) { total ->
                UnifiedFinancialCard(
                    amount = formatPenceToGbp(total.totalPence),
                    whoOrWhere = total.bucket,
                    description = "Monthly total",
                    category = total.bucket,
                    date = "",
                    isIncome = false,
                    partyColorHex = partyColorHex,
                    onClick = { onNavigateToExpenseBucket(total.bucket) }
                )
            }
        }
        return
    }

    // If date filter is active and yields no results, show empty state
    if (filteredInterests.isEmpty()) {
        Column(
            modifier = modifier.fillMaxSize().padding(MaterialTheme.padding.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "No entries match the selected date range",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (fromDate != null || toDate != null) {
                TextButton(onClick = {
                    onFromDateChange(null)
                    onToDateChange(null)
                }) {
                    Text("Clear filter")
                }
            }
        }
        return
    }

    // --- Compute dashboard data ---
    val totalPence = remember(filteredInterests) {
        filteredInterests.sumOf { it.parsedAmountPence ?: 0L }
    }

    // Monthly data: interests in the selected month + previous month for trend
    val selectedMonth = if (monthsWithData.isNotEmpty()) monthsWithData[selectedMonthIndex] else YearMonth.now()
    val previousMonth = selectedMonth.minusMonths(1)
    val currentMonthPence = sumPenceForMonth(filteredInterests, selectedMonth)
    val previousMonthPence = sumPenceForMonth(filteredInterests, previousMonth)
    val percentChange = computePercentChange(currentMonthPence, previousMonthPence)

    // Bucket summaries (from filtered interests)
    val bucketSummaries = remember(filteredInterests) {
        computeBucketSummaries(filteredInterests)
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = MaterialTheme.padding.medium,
            vertical = MaterialTheme.padding.medium
        ),
        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)
    ) {
        // --- Header: total + monthly navigation ---
        item(span = { GridItemSpan(2) }) {
            InterestsDashboardHeader(
                totalPence = totalPence,
                totalEntryCount = filteredInterests.size,
                selectedMonth = selectedMonth,
                currentMonthPence = currentMonthPence,
                percentChange = percentChange,
                hasPreviousMonth = selectedMonthIndex > 0,
                hasNextMonth = selectedMonthIndex < monthsWithData.lastIndex,
                onPreviousMonth = { if (selectedMonthIndex > 0) selectedMonthIndex-- },
                onNextMonth = { if (selectedMonthIndex < monthsWithData.lastIndex) selectedMonthIndex++ }
            )
        }

        // --- Income section (registered interests) ---
        item(span = { GridItemSpan(2) }) {
            IncomeSectionHeader()
        }
        items(bucketSummaries, key = { it.bucketLabel }) { summary ->
            BucketSummaryCard(
                summary = summary,
                onClick = { onNavigateToBucketDetail(summary.bucketLabel) }
            )
        }

        // --- Expenses section (IPSA) ---
        if (expenseBucketTotals.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                ExpenseSectionHeader()
            }
            items(expenseBucketTotals, key = { "expense_${it.bucket}" }) { total ->
                UnifiedFinancialCard(
                    amount = formatPenceToGbp(total.totalPence),
                    whoOrWhere = total.bucket,
                    description = "Monthly total",
                    category = total.bucket,
                    date = "",
                    isIncome = false,
                    partyColorHex = partyColorHex,
                    onClick = { onNavigateToExpenseBucket(total.bucket) }
                )
            }
        }
    }

    // --- Date filter bottom sheet (triggered from the global search bar's filter icon) ---
    if (showFilterSheet) {
        InterestDateFilterBottomSheet(
            fromDate = fromDate,
            toDate = toDate,
            onFromDateChange = onFromDateChange,
            onToDateChange = onToDateChange,
            onClear = {
                onFromDateChange(null)
                onToDateChange(null)
            },
            onDismiss = onFilterSheetDismiss
        )
    }
}

@Composable
private fun InterestsDashboardHeader(
    totalPence: Long,
    totalEntryCount: Int,
    selectedMonth: YearMonth,
    currentMonthPence: Long,
    percentChange: Float?,
    hasPreviousMonth: Boolean,
    hasNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Total sum
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Declared",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatPence(totalPence),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "$totalEntryCount entries",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Monthly navigation: < March 2025 >
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    enabled = hasPreviousMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                }
                Text(
                    text = selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy")),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                IconButton(
                    onClick = onNextMonth,
                    enabled = hasNextMonth
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                }
            }

            // Monthly trend: GBP amount + percentage change vs previous month
            if (currentMonthPence > 0 || percentChange != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatPence(currentMonthPence),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (percentChange != null) {
                        val arrow = if (percentChange >= 0) "▲" else "▼"
                        val color = if (percentChange >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                        Text(
                            text = " $arrow ${"%.0f".format(
                                kotlin.math.abs(percentChange)
                            )}% vs ${selectedMonth.minusMonths(1).format(DateTimeFormatter.ofPattern("MMM"))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = color,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BucketSummaryCard(summary: BucketSummary, onClick: () -> Unit) {
    val icon = BUCKET_ICONS[summary.bucketLabel] ?: Icons.Outlined.Category
    val formattedAmount = formatPence(summary.totalPence)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = summary.bucketLabel,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Text(
                text = formattedAmount,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = summary.bucketLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Data classes and helper functions ---

private data class BucketSummary(val bucketLabel: String, val totalPence: Long, val entryCount: Int)

/**
 * Groups interests by their `bucket` field and computes per-bucket totals.
 * Buckets with no entries are omitted from the result.
 */
private fun computeBucketSummaries(interests: List<Interest>): List<BucketSummary> =
    BUCKET_ORDER.mapNotNull { bucketLabel ->
        val bucketInterests = interests.filter { it.bucket == bucketLabel }
        if (bucketInterests.isEmpty()) return@mapNotNull null
        BucketSummary(
            bucketLabel = bucketLabel,
            totalPence = bucketInterests.sumOf { it.parsedAmountPence ?: 0L },
            entryCount = bucketInterests.size
        )
    }

/**
 * Extracts the sorted list of YearMonth values that have at least one interest,
 * based on `publishedDate`.
 */
private fun extractMonths(interests: List<Interest>): List<YearMonth> = interests.mapNotNull { it.publishedDate }
    .mapNotNull { runCatching { YearMonth.from(LocalDate.parse(it.substring(0, 10))) }.getOrNull() }
    .distinct()
    .sorted()

/**
 * Sums `parsedAmountPence` for interests whose `publishedDate` falls in the given month.
 */
private fun sumPenceForMonth(interests: List<Interest>, month: YearMonth): Long = interests.filter { interest ->
    val date = interest.publishedDate
    date != null && runCatching {
        YearMonth.from(LocalDate.parse(date.substring(0, 10)))
    }.getOrNull() == month
}.sumOf { it.parsedAmountPence ?: 0L }

/**
 * Computes the percentage change from [previous] to [current].
 * Returns null if previous is 0 (can't compute % of zero) or both are 0.
 */
private fun computePercentChange(current: Long, previous: Long): Float? {
    if (previous == 0L) return null
    return ((current - previous).toFloat() / previous.toFloat()) * 100f
}

// --- IPSA Expense section ---

@Composable
private fun IncomeSectionHeader() {
    Text(
        text = "Income",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            top = MaterialTheme.padding.large,
            bottom = MaterialTheme.padding.small
        )
    )
}

@Composable
private fun ExpenseSectionHeader() {
    Text(
        text = "Expenses",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(
            top = MaterialTheme.padding.large,
            bottom = MaterialTheme.padding.small
        )
    )
}

private fun formatPenceToGbp(pence: Long): String {
    val pounds = pence / 100.0
    return if (pounds >= 1000) {
        "£${String.format("%,.0f", pounds)}"
    } else {
        "£${String.format("%,.2f", pounds)}"
    }
}

/**
 * Formats pence as a GBP string: 500000 -> "£5,000", 1234 -> "£12.34".
 */
private fun formatPence(pence: Long): String {
    val pounds = pence / 100.0
    return if (pence % 100 == 0L) {
        "£${"%,.0f".format(pounds)}"
    } else {
        "£${"%,.2f".format(pounds)}"
    }
}
