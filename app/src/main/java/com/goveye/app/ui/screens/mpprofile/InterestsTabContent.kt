package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.domain.model.Interest
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.feed.BUCKET_ICONS
import com.goveye.app.ui.screens.feed.BUCKET_ORDER
import com.goveye.app.ui.screens.feed.bucketIcon
import com.goveye.app.ui.theme.padding
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/*

Bucket icons and order are now shared from FinancialBucketIcons.kt
 */

@Composable
fun InterestsTabContent(
    memberId: Int,
    interests: List<Interest>,
    expenseBucketTotals: List<ExpenseBucketTotal> = emptyList(),
    expenses: List<ExpenseEntity> = emptyList(),
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

    // --- Period selection ---
    // Months are drawn from BOTH income (registrationDate) and expenses
    // (claimDate) so switching month moves income and expenses together.
    val monthsWithData = remember(interests, expenses) {
        val incomeMonths = extractMonths(interests)
        val expenseMonths = expenses.mapNotNull { e ->
            e.claimDate?.takeIf { it.length >= 7 }?.let {
                runCatching { YearMonth.parse(it.take(7)) }.getOrNull()
            }
        }
        (incomeMonths + expenseMonths).distinct().sorted()
    }
    var selectedMonthIndex by remember(interests, expenses) {
        mutableStateOf(if (monthsWithData.isNotEmpty()) monthsWithData.lastIndex else 0)
    }
    // All time is the default view — the headline figures describe the whole
    // record, and the user opts into a single month.
    var allTime by remember(interests, expenses) { mutableStateOf(true) }

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

    // Track whether data has been loaded at least once — avoids flash of
    // "no entries" before the Flow emits its first value
    var dataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(interests, expenseBucketTotals) {
        // Mark as loaded once we get any data (even empty, which means the
        // Flow has emitted and there genuinely are no entries)
        dataLoaded = true
    }

    if (!dataLoaded) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (interests.isEmpty() && expenseBucketTotals.isEmpty() && expenses.isEmpty()) {
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
                ExpenseBucketSummaryCard(
                    bucketLabel = total.bucket,
                    totalPence = total.totalPence,
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
    // Deduplicate interests: the Parliament API re-registers the same interest
    // on amendment (same donor + amount + category). Keep only the latest
    // registration per unique (donorName, parsedAmountPence, categoryNumber) triple.
    val dedupedInterests = remember(filteredInterests) {
        deduplicateInterests(filteredInterests)
    }

    val selectedMonth = if (monthsWithData.isNotEmpty()) monthsWithData[selectedMonthIndex] else YearMonth.now()

    // --- Period-scoped income and expenses ---
    // When [allTime] is true every entry counts; otherwise only the selected
    // month. Income uses registrationDate (when it was actually declared),
    // expenses use claimDate.
    val periodInterests = remember(dedupedInterests, allTime, selectedMonth) {
        if (allTime) {
            dedupedInterests
        } else {
            dedupedInterests.filter { interestMonth(it) == selectedMonth }
        }
    }
    val periodExpenses = remember(expenses, allTime, selectedMonth) {
        if (allTime) {
            expenses
        } else {
            expenses.filter { e ->
                e.claimDate?.takeIf { it.length >= 7 }?.let {
                    runCatching { YearMonth.parse(it.take(7)) }.getOrNull()
                } == selectedMonth
            }
        }
    }

    val incomePence = remember(periodInterests) { periodInterests.sumOf { it.parsedAmountPence ?: 0L } }
    val expensesPence = remember(periodExpenses) { periodExpenses.sumOf { it.amountPence } }
    val netPence = incomePence - expensesPence

    // Bucket summaries scoped to the selected period
    val bucketSummaries = remember(periodInterests) {
        computeBucketSummaries(periodInterests)
    }

    // Expense bucket totals — recomputed from the period's expenses so the
    // cards track the month selector. Falls back to the pre-aggregated
    // totals when no dated expense rows are available (e.g. microview).
    val periodExpenseBuckets = remember(periodExpenses, expenseBucketTotals, allTime) {
        if (expenses.isEmpty()) {
            expenseBucketTotals
        } else {
            periodExpenses
                .groupBy { it.bucket }
                .map { (bucket, rows) -> ExpenseBucketTotal(bucket, rows.sumOf { it.amountPence }) }
                .sortedByDescending { it.totalPence }
        }
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
        // --- Header: net position + income/expenses + period selector ---
        item(span = { GridItemSpan(2) }) {
            FinancesSummaryHeader(
                netPence = netPence,
                incomePence = incomePence,
                expensesPence = expensesPence,
                allTime = allTime,
                selectedMonth = selectedMonth,
                hasMonths = monthsWithData.isNotEmpty(),
                hasPreviousMonth = selectedMonthIndex > 0,
                hasNextMonth = selectedMonthIndex < monthsWithData.lastIndex,
                onPreviousMonth = { if (selectedMonthIndex > 0) selectedMonthIndex-- },
                onNextMonth = { if (selectedMonthIndex < monthsWithData.lastIndex) selectedMonthIndex++ },
                onAllTimeChange = { allTime = it }
            )
        }

        // --- Income section (registered interests) ---
        if (bucketSummaries.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                IncomeSectionHeader()
            }
            items(bucketSummaries, key = { it.bucketLabel }) { summary ->
                BucketSummaryCard(
                    summary = summary,
                    onClick = { onNavigateToBucketDetail(summary.bucketLabel) }
                )
            }
        }

        // --- Expenses section (IPSA) — centered cards matching income style ---
        if (periodExpenseBuckets.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) {
                ExpenseSectionHeader()
            }
            items(periodExpenseBuckets, key = { "expense_${it.bucket}" }) { total ->
                ExpenseBucketSummaryCard(
                    bucketLabel = total.bucket,
                    totalPence = total.totalPence,
                    onClick = { onNavigateToExpenseBucket(total.bucket) }
                )
            }
        }

        // Nothing declared in the chosen month
        if (bucketSummaries.isEmpty() && periodExpenseBuckets.isEmpty()) {
            item(span = { GridItemSpan(2) }) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Nothing declared in ${
                            selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
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

/**
 * Finances hero card.
 *
 * Net position leads — income minus expenses — because that is the figure
 * that actually answers "did this MP take more than they spent". Income and
 * expenses then sit side by side underneath as the two halves that make it
 * up, colour-coded with the same teal/orange pair the vote cards use.
 *
 * The period selector governs all three figures *and* the bucket cards below,
 * so "All time" and a single month are the same reading at two zoom levels.
 */
@Composable
private fun FinancesSummaryHeader(
    netPence: Long,
    incomePence: Long,
    expensesPence: Long,
    allTime: Boolean,
    selectedMonth: YearMonth,
    hasMonths: Boolean,
    hasPreviousMonth: Boolean,
    hasNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onAllTimeChange: (Boolean) -> Unit
) {
    val incomeColor = VoteColors.aye
    val expenseColor = VoteColors.no
    val netColor = when {
        netPence > 0 -> incomeColor
        netPence < 0 -> expenseColor
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = com.goveye.app.ui.components.cardSurfaceColor(netColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Net position",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatSignedPence(netPence),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = netColor
                )
                Text(
                    text = if (allTime) {
                        "Declared income minus expenses, all time"
                    } else {
                        "Declared income minus expenses, " +
                            selectedMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Income / expenses split — the two halves of the net figure
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FinanceStatTile(
                    label = "Income",
                    amount = formatPence(incomePence),
                    modifier = Modifier.weight(1f)
                )
                FinanceStatTile(
                    label = "Expenses",
                    amount = formatPence(expensesPence),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Period selector — All time vs a single month, in the tinted
            // strip the feed cards use for their attribution bar.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PeriodChip(
                    label = "All time",
                    selected = allTime,
                    onClick = { onAllTimeChange(true) }
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (hasMonths) {
                    PeriodChip(
                        label = "By month",
                        selected = !allTime,
                        onClick = { onAllTimeChange(false) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                // Month steppers — only meaningful in by-month mode
                if (!allTime && hasMonths) {
                    IconButton(
                        onClick = onPreviousMonth,
                        enabled = hasPreviousMonth,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Previous month",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy")),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    IconButton(
                        onClick = onNextMonth,
                        enabled = hasNextMonth,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Next month",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/** One half of the income/expenses split under the net figure.
 * Neutral surface — no accent tint. Only the Net position headline
 * carries the surplus/shortfall colour. */
@Composable
private fun FinanceStatTile(label: String, amount: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

/** Selectable pill used by the period selector. */
@Composable
private fun PeriodChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun BucketSummaryCard(summary: BucketSummary, onClick: () -> Unit) {
    val icon = BUCKET_ICONS[summary.bucketLabel] ?: Icons.Outlined.Category
    val formattedAmount = formatPence(summary.totalPence)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = com.goveye.app.ui.components.cardSurfaceColor()
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

/**
 * Centered expense bucket card — matches the income BucketSummaryCard style
 * (icon, amount, label all centered). Used in the Finances tab grid.
 */
@Composable
private fun ExpenseBucketSummaryCard(bucketLabel: String, totalPence: Long, onClick: () -> Unit) {
    val icon = bucketIcon(bucketLabel)
    val formattedAmount = formatPence(totalPence)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = com.goveye.app.ui.components.cardSurfaceColor()
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.medium),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = bucketLabel,
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
                text = bucketLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Data classes and helper functions ---

private data class BucketSummary(val bucketLabel: String, val totalPence: Long, val entryCount: Int)

/**
 * Deduplicates interests that the Parliament API re-registers on amendment.
 * Same donor + amount + category = same interest; keep only the latest
 * (by publishedDate, falling back to registrationDate).
 */
private fun deduplicateInterests(interests: List<Interest>): List<Interest> = interests
    .groupBy { Triple(it.donorName ?: it.summary, it.parsedAmountPence, it.categoryNumber) }
    .mapValues { (_, group) ->
        group.maxByOrNull { it.publishedDate ?: it.registrationDate ?: "" } ?: group.first()
    }
    .values
    .sortedByDescending { it.publishedDate ?: it.registrationDate ?: "" }

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
 * based on `registrationDate` (when the interest was actually declared),
 * NOT `publishedDate` (which can be much later due to re-publication).
 */
private fun extractMonths(interests: List<Interest>): List<YearMonth> = interests.mapNotNull { it.registrationDate }
    .mapNotNull { runCatching { YearMonth.from(LocalDate.parse(it.substring(0, 10))) }.getOrNull() }
    .distinct()
    .sorted()

/**
 * The month an interest was declared in, from `registrationDate`
 * (actual declaration date) not `publishedDate`.
 */
private fun interestMonth(interest: Interest): YearMonth? = interest.registrationDate?.let { date ->
    runCatching { YearMonth.from(LocalDate.parse(date.substring(0, 10))) }.getOrNull()
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
 * Formats a net figure with an explicit sign so a surplus and a shortfall are
 * never mistaken for each other: 500000 -> "+£5,000", -500000 -> "-£5,000".
 */
private fun formatSignedPence(pence: Long): String = when {
    pence > 0 -> "+${formatPence(pence)}"
    else -> formatPence(pence)
}

/**
 * Formats pence as a GBP string: 500000 -> "£5,000", 1234 -> "£12.34".
 */
private fun formatPence(pence: Long): String {
    val pounds = pence / 100.0
    val prefix = if (pence < 0) "-£" else "£"
    val absPounds = kotlin.math.abs(pounds)
    return if (pence % 100 == 0L) {
        "$prefix${"%,.0f".format(absPounds)}"
    } else {
        "$prefix${"%,.2f".format(absPounds)}"
    }
}
