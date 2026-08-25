package com.goveye.app.ui.screens.mpprofile

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.domain.model.ActivityEntry
import com.goveye.app.domain.model.ActivityEntryType
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.feed.FeedDateHeader
import com.goveye.app.ui.screens.feed.TagPillRow
import com.goveye.app.ui.screens.feed.UnifiedFinancialCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Mixed chronological activity feed for the MP profile Activity tab (D-01).
 *
 * Shows all MP activity types — votes, written questions, income declarations,
 * expense claims, committee joins/leaves, and career milestones — sorted by
 * date descending and grouped by date headers ([FeedDateHeader]).
 *
 * Each activity type has a distinct card layout (variable height per D-01).
 * Vote cards are simplified per D-02 (no weight score or rebellion indicator —
 * those are on [VotingRecordScreen]).
 *
 * The feed covers the last 6 months of activity (D-09).
 */
@Composable
fun ActivityTabContent(
    activityEntries: List<ActivityEntry>,
    @Suppress("UNUSED_PARAMETER") enabledTypes: Set<ActivityEntryType>,
    totalCount: Int,
    @Suppress("UNUSED_PARAMETER") onFilterClick: () -> Unit,
    onNavigateToDivision: (Int, Int) -> Unit,
    partyColorHex: String? = null,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Result count
        if (totalCount > 0) {
            item {
                Text(
                    text = "$totalCount activit${if (totalCount != 1) "ies" else "y"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        if (activityEntries.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recent activity",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Group entries by date and render with FeedDateHeader
            val grouped = activityEntries.groupBy { it.date.take(10) }
            grouped.forEach { (dateKey, entries) ->
                item(key = "header_$dateKey") {
                    FeedDateHeader(dateHeader = formatDateHeader(dateKey))
                }
                items(entries, key = { it.id }) { entry ->
                    when (entry.entryType) {
                        ActivityEntryType.VOTE -> ActivityVoteCard(
                            entry,
                            onClick = { onNavigateToDivision(entry.divisionId!!, entry.house!!) }
                        )

                        ActivityEntryType.QUESTION -> ActivityQuestionCard(entry)

                        ActivityEntryType.INCOME -> UnifiedFinancialCard(
                            amount = entry.amountPence?.let { formatAmount(it) } ?: "£0",
                            whoOrWhere = entry.summary.take(80),
                            description = "",
                            category = entry.categoryName ?: "",
                            date = formatActivityDate(entry.date),
                            isIncome = true,
                            partyColorHex = partyColorHex,
                            expandableContent = entry.summary,
                            onClick = { /* navigate to income detail */ }
                        )

                        ActivityEntryType.EXPENSE -> UnifiedFinancialCard(
                            amount = entry.totalAmountPence?.let { formatAmount(it) } ?: "£0",
                            whoOrWhere = entry.bucketLabel ?: "",
                            description = "",
                            category = entry.bucketLabel ?: "",
                            date = formatActivityDate(entry.date),
                            isIncome = false,
                            partyColorHex = partyColorHex,
                            onClick = { /* navigate to expense detail */ }
                        )

                        ActivityEntryType.COMMITTEE -> ActivityCommitteeCard(entry)

                        ActivityEntryType.CAREER -> ActivityCareerCard(entry)

                        ActivityEntryType.SPEECH -> ActivitySpeechCard(
                            entry,
                            onClick = { onNavigateToDivision(entry.divisionId!!, 1) }
                        )
                    }
                }
            }
        }
    }
}

// --- Vote card (D-02 — simplified: division title, aye/no badge, date only) ---

@Composable
fun ActivityVoteCard(entry: ActivityEntry, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ayeColor = VoteColors.aye
        val noColor = VoteColors.no
        val result = entry.voteResult ?: "—"
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(
                    when (result) {
                        "Aye" -> ayeColor
                        "No" -> noColor
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = result,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.divisionTitle ?: entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatActivityDate(entry.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Question card (D-03 — truncated question text, answering body, date tabled) ---

@Composable
fun ActivityQuestionCard(entry: ActivityEntry) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable {
                entry.uin?.let { uin ->
                    runCatching {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.theyworkforyou.com/wrans/?id=$uin")
                        )
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }
                }
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.questionText ?: entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entry.answeringBodyName?.let { body ->
                    Text(
                        text = body,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatActivityDate(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Committee card (committee name, joined/left indicator, date) ---

@Composable
fun ActivityCommitteeCard(entry: ActivityEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.committeeName ?: entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (entry.isJoin == true) "Joined" else "Left",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatActivityDate(entry.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Career card (D-04 — role title, context line, date) ---

@Composable
fun ActivityCareerCard(entry: ActivityEntry) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.roleTitle ?: entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            entry.contextLine?.let { context ->
                Text(
                    text = context,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatActivityDate(entry.date),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// --- Speech card (17-07 — 3 lines of speech text + inherited division tags) ---

@Composable
fun ActivitySpeechCard(entry: ActivityEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Speech text — 3 lines, truncated with ellipsis (LOCKED per UI-SPEC)
            Text(
                text = entry.speechText ?: entry.summary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            // Tags inherited from the parent division
            val tags = entry.speechTags
            if (!tags.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                TagPillRow(tags = tags, onTagClick = {})
            }
        }
    }
}

// --- Helpers ---

private fun formatActivityDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}

private fun formatDateHeader(dateKey: String): String = try {
    val date = LocalDate.parse(dateKey)
    date.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.UK))
} catch (e: Exception) {
    dateKey
}

private fun formatAmount(pence: Long): String {
    val pounds = pence / 100.0
    return "£${String.format(Locale.UK, "%,.0f", pounds)}"
}
