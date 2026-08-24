package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.goveye.app.domain.model.Division
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.screens.divisions.DivisionResultBar

// Theme-aware vote colors — teal for Aye, orange for No
private val AyeColor @Composable get() = VoteColors.aye
private val NoColor @Composable get() = VoteColors.no

/**
 * Extracted per-type card data used by [UnifiedFeedCard]. Each [FeedItem]
 * subtype maps to a [CardTypeData] instance via [getCardTypeData].
 */
private data class CardTypeData(
    val imageUrl: String?,
    val title: String,
    val typeLabel: String,
    val byWho: String,
    val source: String,
    val date: String,
    val tags: List<String>,
    val divisionData: DivisionData?
)

private data class DivisionData(val ayeCount: Int, val noCount: Int)

/**
 * Unified feed card — renders all [FeedItem] subtypes (Division, Publication,
 * Statement, Legislation) with a single consistent layout per the UI-SPEC
 * Section 1 LOCKED contract.
 *
 * Layout (top to bottom):
 * 1. Image (16:9, only if imageUrl is non-blank)
 * 2. Title (left, weight(1f)) + Type pill (right) — Row, SpaceBetween
 * 3. "By who" line (bodySmall, onSurfaceVariant)
 * 4. Division bar (4dp, only for DivisionItem) + vote counts
 * 5. Source (left) + Date (right, DD/MM/YYYY) — Row, SpaceBetween
 * 6. Tags (TagPillRow, only if non-empty)
 *
 * When [hasFollowedVotes] is true (division only), the card background uses
 * primary at 0.05 alpha and a 4dp left-edge strip signals the highlight.
 *
 * The old [CardIconBadge] (32dp circle at TopEnd) is removed — the [TypeBadge]
 * replaces it in the top-right position. The title has weight(1f) + maxLines=2
 * + Ellipsis so it shrinks before reaching the type pill (no overlap).
 */
@Composable
fun UnifiedFeedCard(
    item: FeedItem,
    hasFollowedVotes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {}
) {
    val data = getCardTypeData(item)
    val cardColor = if (hasFollowedVotes) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Left edge strip for followed-MP highlight (division only)
            if (hasFollowedVotes) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(72.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                )
            }
            Column(modifier = Modifier.fillMaxWidth()) {
                // 1. Image on top if available (16:9 aspect, top corners rounded)
                if (!data.imageUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = data.imageUrl,
                        contentDescription = data.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = 0.dp,
                                    bottomEnd = 0.dp
                                )
                            ),
                        contentScale = ContentScale.Crop,
                        loading = {
                            // Placeholder rectangle while loading
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            )
                        },
                        error = {
                            // On error, collapse — render nothing (zero height).
                            // The content below fills the card naturally.
                        }
                    )
                }

                // Content below image (or full content if no image)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 2. Title + Type pill row — title weight(1f) shrinks before pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = data.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        TypeBadge(text = data.typeLabel)
                    }

                    // 3. "By who" line
                    Text(
                        text = data.byWho,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // 4. Division bar (4dp, only for DivisionItem) + vote counts
                    if (data.divisionData != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            DivisionResultBar(
                                ayeCount = data.divisionData.ayeCount,
                                noCount = data.divisionData.noCount,
                                barHeight = 4.dp,
                                modifier = Modifier.weight(1f)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${data.divisionData.ayeCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = AyeColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "-",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${data.divisionData.noCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = NoColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // 5. Source (left) + Date (right) row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = data.source,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = data.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // 6. Tags
                    if (data.tags.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        TagPillRow(
                            tags = data.tags,
                            onTagClick = onTagClick
                        )
                    }
                }
            }
        }
    }
}

/**
 * Type badge — labelSmall text in a RoundedCornerShape(4.dp) chip with
 * primaryContainer background and onPrimaryContainer text.
 *
 * Moved from FeedDivisionCard.kt — shared by all card types via
 * [UnifiedFeedCard]. Replaces the old [CardIconBadge] in the top-right
 * position per the UI-SPEC LOCKED decision.
 */
@Composable
fun TypeBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

/**
 * Formats an ISO date string (e.g. "2026-08-24T...") to DD/MM/YYYY.
 *
 * Moved from FeedDivisionCard.kt — shared by all card types via
 * [UnifiedFeedCard].
 */
fun formatDivisionDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}

/**
 * Extracts the per-type card data (image, title, type label, by-who, source,
 * date, tags, division data) from a [FeedItem] subtype.
 */
private fun getCardTypeData(item: FeedItem): CardTypeData = when (item) {
    is FeedItem.DivisionItem -> {
        val division: Division = item.division
        CardTypeData(
            imageUrl = null,
            title = division.title,
            typeLabel = "Division",
            byWho = if (division.house == 2) "Lords" else "Commons",
            source = if (division.house == 2) "Lords" else "Commons",
            date = formatDivisionDate(division.date),
            tags = item.tags,
            divisionData = DivisionData(ayeCount = division.ayeCount, noCount = division.noCount)
        )
    }

    is FeedItem.PublicationItem -> {
        val publication = item.publication
        CardTypeData(
            imageUrl = publication.imageUrl,
            title = publication.title,
            typeLabel = "Publication",
            byWho = publication.organisation,
            source = publication.organisation,
            date = formatDivisionDate(publication.firstPublishedAt),
            tags = item.tags,
            divisionData = null
        )
    }

    is FeedItem.StatementItem -> {
        val statement = item.statement
        CardTypeData(
            imageUrl = null,
            title = statement.title,
            typeLabel = "Statement",
            byWho = "by ${statement.memberRole}",
            source = statement.answeringBodyName,
            date = formatDivisionDate(statement.dateMade),
            tags = item.tags,
            divisionData = null
        )
    }

    is FeedItem.LegislationItem -> {
        val legislation = item.legislation
        CardTypeData(
            imageUrl = null,
            title = legislation.title,
            typeLabel = "Legislation",
            byWho = legislation.type,
            source = legislation.type,
            date = formatDivisionDate(legislation.date),
            tags = item.tags,
            divisionData = null
        )
    }

    // Financial and Speech items are rendered by their own card composables
    // (FeedFinancialCard / FeedSpeechCard), never via UnifiedFeedCard. These
    // branches exist only to satisfy the exhaustive `when` over FeedItem.
    is FeedItem.FinancialItem -> CardTypeData(
        imageUrl = null,
        title = item.amount,
        typeLabel = if (item.isIncome) "Income" else "Expense",
        byWho = item.whoOrWhere,
        source = item.category,
        date = formatDivisionDate(item.date),
        tags = item.tags,
        divisionData = null
    )

    is FeedItem.SpeechItem -> CardTypeData(
        imageUrl = null,
        title = item.speechText,
        typeLabel = "Speech",
        byWho = item.memberName,
        source = item.divisionTitle,
        date = formatDivisionDate(item.date),
        tags = item.tags,
        divisionData = null
    )
}
