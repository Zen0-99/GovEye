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
import com.goveye.app.ui.screens.feed.FeedItem
import com.goveye.app.ui.screens.feed.FeedMpVoteCard
import com.goveye.app.ui.screens.feed.FeedSpeechCard
import com.goveye.app.ui.screens.feed.FeedWrittenQuestionCard
import com.goveye.app.ui.screens.feed.FinancialDetailField
import com.goveye.app.ui.screens.feed.TagPillRow
import com.goveye.app.ui.screens.feed.UnifiedFinancialCard
import com.goveye.app.ui.screens.feed.formatInterestStructuredFields
import com.goveye.app.ui.screens.feed.interestDescriptionLine
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
    @Suppress("UNUSED_PARAMETER") totalCount: Int,
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
                        // Reuses the feed's vote card. showMember = false drops
                        // the MP avatar/name — this page is already about them.
                        ActivityEntryType.VOTE -> FeedMpVoteCard(
                            item = FeedItem.MpVoteItem(
                                memberId = 0,
                                memberName = "",
                                memberPartyColorHex = null,
                                memberPhotoUrl = null,
                                vote = when (entry.voteResult) {
                                    "Aye" -> "AYE"
                                    "No" -> "NO"
                                    else -> "NO VOTE RECORDED"
                                },
                                divisionId = entry.divisionId ?: 0,
                                divisionTitle = entry.divisionTitle ?: entry.summary,
                                divisionHouse = entry.house ?: 1,
                                ayeCount = 0,
                                noCount = 0,
                                date = entry.date
                            ),
                            onClick = { onNavigateToDivision(entry.divisionId!!, entry.house!!) },
                            showMember = false
                        )

                        // Reuses the feed's written-question card, passing the
                        // answer fields the feed itself does not carry.
                        ActivityEntryType.QUESTION -> FeedWrittenQuestionCard(
                            item = FeedItem.WrittenQuestionItem(
                                memberId = 0,
                                memberName = "",
                                memberPartyColorHex = null,
                                memberPhotoUrl = null,
                                questionText = entry.questionText ?: entry.summary,
                                heading = entry.heading ?: "",
                                answeringBodyName = entry.answeringBodyName ?: "",
                                uin = entry.uin ?: "",
                                questionId = entry.id.hashCode(),
                                date = entry.date
                            ),
                            onClick = {},
                            showMember = false,
                            answerText = entry.answerText,
                            answerIsHolding = entry.answerIsHolding == true,
                            dateAnswered = entry.dateAnswered,
                            isWithdrawn = entry.isWithdrawn == true
                        )

                        ActivityEntryType.INCOME -> {
                            val whoOrWhere = entry.donorName?.takeIf { it.isNotBlank() }
                                ?: entry.summary.lineSequence().firstOrNull()?.take(80) ?: ""
                            val descLine = interestDescriptionLine(
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
                                descriptionLine = descLine.takeIf { it.isNotBlank() }
                            )
                            UnifiedFinancialCard(
                                amount = entry.amountPence?.let { formatAmount(it) } ?: "Unpaid",
                                whoOrWhere = whoOrWhere,
                                description = descLine,
                                category = entry.categoryName ?: "",
                                date = formatActivityDate(entry.date),
                                isIncome = true,
                                isUnpaid = entry.amountPence == null,
                                partyColorHex = partyColorHex,
                                expandableFields = structuredFields.takeIf { it.isNotEmpty() },
                                expandableContent = if (structuredFields.isEmpty()) {
                                    entry.summary.takeIf { it.length > 80 }
                                } else {
                                    null
                                },
                                bucket = entry.bucket,
                                onClick = { /* navigate to income detail */ }
                            )
                        }

                        ActivityEntryType.EXPENSE -> UnifiedFinancialCard(
                            amount = entry.totalAmountPence?.let { formatAmount(it) } ?: "£0",
                            whoOrWhere = entry.bucketLabel ?: "",
                            description = "",
                            category = entry.bucketLabel ?: "",
                            date = formatActivityDate(entry.date),
                            isIncome = false,
                            partyColorHex = partyColorHex,
                            bucket = entry.bucketLabel,
                            onClick = { /* navigate to expense detail */ }
                        )

                        ActivityEntryType.COMMITTEE -> ActivityCommitteeCard(entry)

                        ActivityEntryType.CAREER -> ActivityCareerCard(entry)

                        // Reuses the feed's speech pull-quote card.
                        ActivityEntryType.SPEECH -> FeedSpeechCard(
                            item = FeedItem.SpeechItem(
                                memberId = 0,
                                memberName = "",
                                memberPartyColorHex = null,
                                memberPhotoUrl = null,
                                speechText = entry.speechText ?: entry.summary,
                                divisionId = entry.divisionId ?: 0,
                                divisionTitle = entry.divisionTitle ?: entry.summary,
                                date = entry.date
                            ),
                            onClick = { onNavigateToDivision(entry.divisionId!!, 1) },
                            showMember = false
                        )
                    }
                }
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
            .background(com.goveye.app.ui.components.cardSurfaceColor())
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
            .background(com.goveye.app.ui.components.cardSurfaceColor())
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
