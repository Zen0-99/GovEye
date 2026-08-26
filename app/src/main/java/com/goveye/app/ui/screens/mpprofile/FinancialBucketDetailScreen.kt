package com.goveye.app.ui.screens.mpprofile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
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
import com.goveye.app.ui.components.DelayedLoadingContainer
import com.goveye.app.ui.components.SearchBarConfig
import com.goveye.app.ui.screens.feed.FinancialDetailField
import com.goveye.app.ui.screens.feed.UnifiedFinancialCard
import com.goveye.app.ui.screens.feed.formatDivisionDate
import com.goveye.app.ui.screens.feed.formatInterestStructuredFields
import com.goveye.app.ui.screens.feed.interestDescriptionLine
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

    // Track whether data has been loaded at least once (avoids flash of
    // "no entries" before the Flow emits its first value)
    var dataLoaded by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.interests, uiState.expenses) {
        if (uiState.interests.isNotEmpty() || uiState.expenses.isNotEmpty() || !uiState.isLoading) {
            dataLoaded = true
        }
    }

    if (!dataLoaded) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (bucketEntries.isEmpty()) {
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
                        FinancialEntryCard(entry = entry)
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
    bucket = bucket ?: "",
    donorName = donorName,
    paymentType = paymentType,
    paymentDescription = paymentDescription,
    donorStatus = donorStatus,
    donorAddress = donorAddress,
    donorCompanyIdentifier = donorCompanyIdentifier,
    destination = destination,
    visitPurpose = visitPurpose,
    organisationName = organisationName,
    organisationDescription = organisationDescription,
    propertyLocation = propertyLocation,
    propertyType = propertyType,
    hoursWorked = hoursWorked,
    familyMemberName = familyMemberName,
    familyMemberRelationship = familyMemberRelationship,
    familyMemberRole = familyMemberRole
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

/**
 * Renders a financial entry using the unified card design with expand/collapse.
 * Income entries: "by X" shows the donor/payer (extracted from summary), full
 * summary text is in the expandable section.
 * Expense entries: "for X" shows the bucket/category, journey and payment
 * details are in the expandable section.
 */
@Composable
private fun FinancialEntryCard(entry: FinancialEntry) {
    val isIncome = entry.entryType == FinancialEntryType.INCOME
    val amount = entry.amountPence?.let { formatPencePublic(it) }
        ?: if (isIncome) "Unpaid" else ""
    val date = entry.date?.let { formatDivisionDate(it) } ?: ""
    val category = entry.categoryName ?: entry.bucket ?: ""

    val whoOrWhere: String
    val description: String
    val expandableContent: String?
    val expandableFields: List<FinancialDetailField>?

    if (isIncome) {
        whoOrWhere = entry.donorName?.takeIf { it.isNotBlank() }
            ?: entry.summary.lineSequence().firstOrNull()?.take(80) ?: ""
        description = interestDescriptionLine(
            entry.paymentDescription,
            entry.visitPurpose,
            entry.organisationDescription
        ) ?: ""
        val structuredFields = formatInterestStructuredFields(
            entry.donorName, entry.paymentType, entry.paymentDescription,
            entry.donorStatus, entry.donorAddress, entry.donorCompanyIdentifier,
            entry.destination, entry.visitPurpose, entry.organisationName,
            entry.organisationDescription, entry.propertyLocation, entry.propertyType,
            entry.hoursWorked, entry.familyMemberName,
            entry.familyMemberRelationship, entry.familyMemberRole,
            descriptionLine = description.takeIf { it.isNotBlank() }
        )
        expandableFields = structuredFields.takeIf { it.isNotEmpty() }
        expandableContent = if (expandableFields == null) entry.summary.takeIf { it.length > 80 } else null
    } else {
        whoOrWhere = entry.categoryName ?: entry.bucket ?: ""
        description = ""
        expandableContent = buildExpenseDetail(entry).takeIf { it.isNotBlank() }
        expandableFields = null
    }

    UnifiedFinancialCard(
        amount = amount,
        whoOrWhere = whoOrWhere,
        description = description,
        category = category,
        date = date,
        isIncome = isIncome,
        isUnpaid = isIncome && entry.amountPence == null,
        partyColorHex = null,
        expandableContent = expandableContent,
        expandableFields = expandableFields,
        bucket = entry.bucket,
        onClick = {}
    )
}

/**
 * Builds the expandable detail text for expense entries — journey info,
 * payment breakdown, claim status.
 */
private fun buildExpenseDetail(entry: FinancialEntry): String {
    val parts = mutableListOf<String>()

    // Summary text
    if (entry.summary.isNotBlank()) {
        parts.add(entry.summary)
    }

    // Journey details
    entry.journeyFrom?.let { from ->
        entry.journeyTo?.let { to ->
            if (from.isNotBlank() && to.isNotBlank()) {
                val journey = buildString {
                    append("Journey: $from \u2192 $to")
                    entry.journeyType?.let { if (it.isNotBlank()) append(" ($it)") }
                    entry.mileage?.let { if (it.isNotBlank()) append(" \u00b7 $it miles") }
                }
                parts.add(journey)
            }
        }
    }

    // Payment breakdown
    val paymentParts = mutableListOf<String>()
    entry.amountPaidPence?.let { if (it > 0) paymentParts.add("Paid: ${formatPencePublic(it)}") }
    entry.amountNotPaidPence?.let { if (it > 0) paymentParts.add("Not paid: ${formatPencePublic(it)}") }
    entry.amountRepaidPence?.let { if (it > 0) paymentParts.add("Repaid: ${formatPencePublic(it)}") }
    if (paymentParts.isNotEmpty()) parts.add(paymentParts.joinToString(" \u00b7 "))

    // Reason if not paid
    entry.reasonIfNotPaid?.let { reason ->
        if (reason.isNotBlank()) parts.add("Not paid: $reason")
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
    if (metaText.isNotBlank()) parts.add(metaText)

    return parts.joinToString("\n")
}

private fun formatPencePublic(pence: Long): String {
    val pounds = pence / 100.0
    return if (pence % 100 == 0L) {
        "\u00a3${"%,.0f".format(pounds)}"
    } else {
        "\u00a3${"%,.2f".format(pounds)}"
    }
}
