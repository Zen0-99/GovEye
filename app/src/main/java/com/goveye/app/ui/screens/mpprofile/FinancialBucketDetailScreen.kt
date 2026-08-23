package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.domain.model.FinancialEntry
import com.goveye.app.domain.model.FinancialEntryType
import com.goveye.app.domain.model.Interest
import com.goveye.app.ui.components.ConfigureDetailTopBar
import com.goveye.app.ui.components.ConfigureSearchBar
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.theme.padding
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Unified financial bucket detail screen — handles both income (registered
 * interests) and expenses (IPSA claims).
 *
 * Replaces the former InterestBucketDetailScreen. Both entry types share
 * the same rendering logic: date grouping, search filtering, and entry rows.
 * Editing this screen changes both the income and expense detail views.
 *
 * @param memberId The MP's ID
 * @param bucketLabel The bucket to filter by (e.g. "Travel", "Shareholdings")
 * @param entryType INCOME for registered interests, EXPENSE for IPSA claims
 * @param onBack Navigation back callback
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FinancialBucketDetailScreen(
    memberId: Int,
    bucketLabel: String,
    entryType: FinancialEntryType = FinancialEntryType.INCOME,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(memberId) {
        viewModel.loadProfile(memberId)
    }

    var searchQuery by remember { mutableStateOf("") }

    ConfigureDetailTopBar(
        config = com.goveye.app.ui.components.DetailTopBarConfig(
            title = bucketLabel,
            onBack = onBack
        )
    )

    ConfigureSearchBar(
        config = SearchBarConfig(
            query = searchQuery,
            placeholder = "Search entries\u2026",
            onQueryChange = { searchQuery = it }
        )
    )

    // Convert either interests or expenses to FinancialEntry list, filtered by bucket.
    // Dates are normalized to YYYY-MM-DD so grouping works for both formats.
    val bucketEntries = remember(uiState.interests, uiState.expenses, bucketLabel, entryType) {
        when (entryType) {
            FinancialEntryType.INCOME ->
                uiState.interests
                    .filter { it.bucket == bucketLabel }
                    .map { it.toFinancialEntry() }

            FinancialEntryType.EXPENSE ->
                uiState.expenses
                    .filter { it.bucket == bucketLabel }
                    .map { it.toFinancialEntry() }
        }
    }

    val filteredEntries = remember(bucketEntries, searchQuery) {
        if (searchQuery.isBlank()) {
            bucketEntries
        } else {
            val query = searchQuery.lowercase().trim()
            bucketEntries.filter { entry ->
                entry.summary.lowercase().contains(query) ||
                    entry.categoryName?.lowercase()?.contains(query) == true
            }
        }
    }

    // Group by full date (YYYY-MM-DD). No sub-grouping by category —
    // the category is shown as a label on each row, not as a separate header.
    // Entries within each group are sorted by category number (interests) then
    // category name, so the order is deterministic.
    val groupedByDate = remember(filteredEntries) {
        filteredEntries
            .groupBy { it.date?.let { d -> normalizeToIsoDate(d) } ?: "Unknown" }
            .mapValues { (_, entries) ->
                entries.sortedWith(
                    compareBy(
                        { it.categoryNumber ?: "99" },
                        { it.categoryName ?: "" }
                    )
                )
            }
            .toList()
            .sortedByDescending { (dateKey, _) -> dateKey }
    }

    if (bucketEntries.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No entries in this category",
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
            if (filteredEntries.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No matching entries",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                groupedByDate.forEach { (dateKey, entries) ->
                    // Sticky date header — stays pinned while scrolling through entries
                    stickyHeader(key = "header_$dateKey") {
                        StickyDateHeader(
                            dateKey = dateKey,
                            entryCount = entries.size
                        )
                    }
                    // Entries are pre-sorted within each group (see groupedByDate above).
                    // No sub-grouping — each row shows its own category label.
                    items(entries, key = { "${it.entryType}_${it.id}" }) { entry ->
                        FinancialEntryRow(entry = entry)
                    }
                }
            }
        }
    }
}

// ── Conversion helpers ──────────────────────────────────────────────

private fun Interest.toFinancialEntry(): FinancialEntry = FinancialEntry(
    entryType = FinancialEntryType.INCOME,
    id = id,
    summary = summary,
    categoryName = categoryName,
    categoryNumber = categoryNumber,
    date = publishedDate, // already YYYY-MM-DD
    amountPence = parsedAmountPence,
    bucket = bucket ?: ""
)

private fun ExpenseEntity.toFinancialEntry(): FinancialEntry {
    val summaryParts = listOfNotNull(shortDescription, details)
    val summary = if (summaryParts.isNotEmpty()) summaryParts.joinToString(" \u2014 ") else category
    return FinancialEntry(
        entryType = FinancialEntryType.EXPENSE,
        id = id,
        summary = summary,
        categoryName = category,
        categoryNumber = null,
        date = claimDate?.let { normalizeToIsoDate(it) }, // DD/MM/YYYY -> YYYY-MM-DD
        amountPence = amountPence,
        bucket = bucket,
        claimNumber = claimNumber,
        journeyType = journeyType,
        journeyFrom = journeyFrom,
        journeyTo = journeyTo,
        travel = travel,
        nights = nights,
        mileage = mileage,
        amountPaidPence = amountPaidPence,
        amountNotPaidPence = amountNotPaidPence,
        amountRepaidPence = amountRepaidPence,
        reasonIfNotPaid = reasonIfNotPaid,
        supplyMonth = supplyMonth,
        supplyPeriod = supplyPeriod,
        status = status
    )
}

// ── Date normalization ──────────────────────────────────────────────

/**
 * Normalize any date string to YYYY-MM-DD (ISO format).
 * Handles both YYYY-MM-DD (interests) and DD/MM/YYYY (expenses).
 * Returns the original string if it can't be parsed.
 */
private fun normalizeToIsoDate(dateString: String): String {
    // Already ISO format?
    if (dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) return dateString
    // DD/MM/YYYY format (IPSA)
    return runCatching {
        val parts = dateString.split("/")
        if (parts.size == 3) {
            val day = parts[0].padStart(2, '0')
            val month = parts[1].padStart(2, '0')
            val year = parts[2]
            "$year-$month-$day"
        } else {
            dateString
        }
    }.getOrDefault(dateString)
}

/**
 * Format a date for display in the row (e.g. "2nd March 2026").
 * Handles both ISO and DD/MM/YYYY formats.
 */
private fun formatDateDisplay(dateString: String?): String? {
    if (dateString.isNullOrBlank()) return null
    return runCatching {
        val isoDate = normalizeToIsoDate(dateString)
        val date = LocalDate.parse(isoDate.substring(0, 10))
        formatFullDate(date)
    }.getOrNull() ?: dateString
}

// ── Relative date formatting ────────────────────────────────────────

/**
 * Format a YYYY-MM-DD date key as a relative date label for the sticky header.
 *
 * Rules:
 * - Same day: "X minutes ago" or "X hours ago"
 * - 1 day ago: "Yesterday"
 * - 2-6 days: "X days ago"
 * - 7-13 days: "Last week"
 * - Older: full date like "2nd March 2026"
 */
private fun formatRelativeDate(dateKey: String): String {
    if (dateKey == "Unknown") return "Undated"
    return runCatching {
        val date = LocalDate.parse(dateKey)
        val now = LocalDateTime.now()
        val minutesBetween = ChronoUnit.MINUTES.between(date.atStartOfDay(), now)
        val daysBetween = ChronoUnit.DAYS.between(date, now.toLocalDate())

        when {
            // Same day — show minutes/hours ago
            daysBetween == 0L -> when {
                minutesBetween < 1 -> "Just now"

                minutesBetween < 60 -> "$minutesBetween minute${if (minutesBetween == 1L) "" else "s"} ago"

                minutesBetween < 1440 -> {
                    val hours = minutesBetween / 60
                    "$hours hour${if (hours == 1L) "" else "s"} ago"
                }

                else -> "Today"
            }

            // 1 day ago
            daysBetween == 1L -> "Yesterday"

            // 2-6 days
            daysBetween in 2..6 -> "$daysBetween days ago"

            // 7-13 days
            daysBetween in 7..13 -> "Last week"

            // Older — full date
            else -> formatFullDate(date)
        }
    }.getOrDefault(dateKey)
}

/**
 * Format a LocalDate as "2nd March 2026" with ordinal day.
 */
private fun formatFullDate(date: LocalDate): String {
    val day = date.dayOfMonth
    val ordinal = when {
        day in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    val monthYear = date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    return "${day}$ordinal $monthYear"
}

// ── UI components ───────────────────────────────────────────────────

@Composable
private fun StickyDateHeader(dateKey: String, entryCount: Int) {
    val displayText = formatRelativeDate(dateKey)
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = displayText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$entryCount ${if (entryCount == 1) "entry" else "entries"}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FinancialEntryRow(entry: FinancialEntry) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Category label — shown once per row, not as a separate sub-group header
        entry.categoryName?.let { name ->
            if (name.isNotBlank()) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
        // Summary (description)
        if (entry.summary.isNotBlank() && entry.summary != entry.categoryName) {
            Text(
                text = entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Journey details (expenses only)
        if (entry.entryType == FinancialEntryType.EXPENSE) {
            entry.journeyFrom?.let { from ->
                entry.journeyTo?.let { to ->
                    if (from.isNotBlank() && to.isNotBlank()) {
                        Text(
                            text = buildString {
                                append("Journey: $from \u2192 $to")
                                entry.journeyType?.let { if (it.isNotBlank()) append(" ($it)") }
                                entry.mileage?.let { if (it.isNotBlank()) append(" \u00b7 $it miles") }
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Payment breakdown
            val paymentText = buildPaymentBreakdown(entry)
            if (paymentText.isNotBlank()) {
                Text(
                    text = paymentText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Reason if not paid
            entry.reasonIfNotPaid?.let { reason ->
                if (reason.isNotBlank()) {
                    Text(
                        text = "Not paid: $reason",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            // Claim number + status
            val metaText = buildString {
                entry.claimNumber?.let { if (it.isNotBlank()) append("Claim #$it") }
                entry.status?.let {
                    if (it.isNotBlank()) {
                        if (isNotEmpty()) append(" \u00b7 ")
                        append(it)
                    }
                }
            }
            if (metaText.isNotBlank()) {
                Text(
                    text = metaText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        // Date + amount row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            formatDateDisplay(entry.date)?.let { date ->
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            entry.amountPence?.let { pence ->
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

private fun buildPaymentBreakdown(entry: FinancialEntry): String {
    val parts = mutableListOf<String>()
    entry.amountPaidPence?.let { if (it > 0) parts.add("Paid: ${formatPencePublic(it)}") }
    entry.amountNotPaidPence?.let { if (it > 0) parts.add("Not paid: ${formatPencePublic(it)}") }
    entry.amountRepaidPence?.let { if (it > 0) parts.add("Repaid: ${formatPencePublic(it)}") }
    return parts.joinToString(" \u00b7 ")
}

private fun formatPencePublic(pence: Long): String {
    val pounds = pence / 100.0
    return if (pence % 100 == 0L) {
        "\u00a3${"%,.0f".format(pounds)}"
    } else {
        "\u00a3${"%,.2f".format(pounds)}"
    }
}
