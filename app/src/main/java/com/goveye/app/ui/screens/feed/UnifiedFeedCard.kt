package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Gavel
import androidx.compose.material.icons.outlined.HowToVote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.cardClickable
import com.goveye.app.ui.components.cardSurfaceColor

// Theme-aware vote colors — teal for Aye, orange for No
private val AyeColor @Composable get() = VoteColors.aye
private val NoColor @Composable get() = VoteColors.no

/**
 * Formats raw legislation API type strings into human-readable labels.
 * e.g. "UnitedKingdomStatutoryInstrument" → "UK Statutory Instrument"
 */
fun formatLegislationType(type: String): String {
    val map = mapOf(
        "UnitedKingdomStatutoryInstrument" to "UK Statutory Instrument",
        "ScottishStatutoryInstrument" to "Scottish Statutory Instrument",
        "WelshStatutoryInstrument" to "Welsh Statutory Instrument",
        "NorthernIrelandStatutoryRule" to "NI Statutory Rule",
        "NorthernIrelandAct" to "NI Act",
        "UnitedKingdomPublicGeneralAct" to "UK Public General Act",
        "UnitedKingdomLocalAct" to "UK Local Act",
        "UnitedKingdomChurchMeasure" to "Church Measure",
        "UnitedKingdomMinisterialOrder" to "Ministerial Order"
    )
    return map[type] ?: type.replace(Regex("([a-z])([A-Z])"), "$1 $2")
}

/**
 * Formats an ISO date string (e.g. "2026-08-24T...") to DD/MM/YYYY.
 */
fun formatDivisionDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    val cleaned = dateString.split("T").first()
    if (cleaned.matches(Regex("\\d{2}/\\d{2}/\\d{4}"))) return cleaned
    return try {
        val parts = cleaned.split("-")
        "${parts[2]}/${parts[1]}/${parts[0]}"
    } catch (e: Exception) {
        cleaned
    }
}

/**
 * Formats a Parliament link type (e.g. "JointStatement") into a human-readable
 * label for statement cards (e.g. "Joint Statement").
 */
fun formatLinkType(linkType: String): String = when (linkType) {
    "JointStatement" -> "Joint Statement"
    "Response" -> "Response"
    "Correction" -> "Correction"
    "Update" -> "Update"
    else -> linkType.replace(Regex("([a-z])([A-Z])"), "$1 $2")
}

/**
 * Strips parliamentary filler phrases from the start of a written statement's
 * text so the actual content leads the quote instead of formalities.
 *
 * Based on scanning the actual statement database, the dominant pattern (65%+)
 * is: "My [Hon/Right Honourable/Rt Hon/Honourable/rt hon] Friend the [role]
 * ([name]) has [today] made the following [Written Ministerial] Statement[.:]"
 *
 * Instead of guessing individual phrases, we find the "has ... made the
 * following ... statement" marker and return everything after it. This
 * handles all the "My ... Friend ..." variants in one pass. Falls back to
 * stripping other known openings if no marker is found.
 */
fun stripStatementFiller(text: String): String {
    var cleaned = text.trim()

    // Pattern 1 (dominant): "My ... Friend ... has [today] made the following
    // [Written Ministerial] Statement[:.]" — return everything after.
    val markerPattern = Regex(
        """(?is)^My\s+[^.]*?\s+has\s+(?:today\s+)?made\s+the\s+following\s+(?:Written\s+Ministerial\s+)?[Ss]tatement\s*[:\.]\s*"""
    )
    val afterMarker = markerPattern.find(cleaned)
    if (afterMarker != null) {
        cleaned = cleaned.substring(afterMarker.range.last + 1).trim()
        if (cleaned.isNotBlank()) return cleaned
    }

    // Pattern 2: "The [role] has [today] made the following ... Statement[:.]"
    val markerPattern2 = Regex(
        """(?is)^The\s+[^.]*?\s+has\s+(?:today\s+)?made\s+the\s+following\s+(?:Written\s+Ministerial\s+)?[Ss]tatement\s*[:\.]\s*"""
    )
    val afterMarker2 = markerPattern2.find(cleaned)
    if (afterMarker2 != null) {
        cleaned = cleaned.substring(afterMarker2.range.last + 1).trim()
        if (cleaned.isNotBlank()) return cleaned
    }

    // Fallback: strip other known openings
    // "I am writing to inform the House that" / "I would like to inform the House that"
    cleaned = cleaned.replace(
        Regex(
            """(?i)^I\s+(?:am\s+writing\s+to|would\s+like\s+to)\s+inform\s+(?:the\s+House|Members)\s+that\s*""",
            RegexOption.IGNORE_CASE
        ),
        ""
    )

    // "The Government has today published/announced" / "The government has today announced"
    cleaned = cleaned.replace(
        Regex(
            """(?i)^The\s+[Gg]overnment\s+(?:has\s+today\s+(?:published|announced)|is\s+today\s+(?:publishing|announcing))\s*(?:that\s+)?""",
            RegexOption.IGNORE_CASE
        ),
        ""
    )

    // "I am today publishing/announcing" / "I have today published/announced"
    cleaned = cleaned.replace(
        Regex(
            """(?i)^I\s+(?:am\s+today\s+(?:publishing|announcing|informing)|have\s+today\s+(?:published|announced))\s*(?:that\s+)?""",
            RegexOption.IGNORE_CASE
        ),
        ""
    )

    // "I wish to inform the House that" / "I am making this statement on behalf of"
    cleaned = cleaned.replace(
        Regex(
            """(?i)^I\s+(?:wish\s+to\s+inform|am\s+making\s+this\s+statement\s+on\s+behalf\s+of)\s+[^.]*?\.\s*""",
            RegexOption.IGNORE_CASE
        ),
        ""
    )

    return cleaned.trim()
}

/**
 * Unified feed card — each [FeedItem] subtype gets its own card architecture.
 *
 * Iteration 5. The verdict word on divisions is a keeper and survives; where
 * source and date live is now type-specific rather than a fixed footer.
 *
 * - **Division** — tinted verdict band across the top carrying the outcome,
 *   the date, and the split. Content hangs below it.
 * - **Publication** — full-bleed image with a magazine issue-date block
 *   overlaid top-left and the headline bottom-left.
 * - **Statement** — letterhead: department and date sit *above* a rule, with
 *   an italic signature closing the card.
 * - **Legislation** — index row with the date in a fixed left column.
 */
@Composable
fun UnifiedFeedCard(
    item: FeedItem,
    hasFollowedVotes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onTagClick: (String) -> Unit = {}
) {
    // Division cards use a verdict-tinted surface (like income/expense cards)
    // instead of the default CardShell, so the entire card picks up a subtle
    // green (passed) or red (failed) tint.
    if (item is FeedItem.DivisionItem) {
        val division = item.division
        val passed = division.ayeCount > division.noCount
        val verdictColor = if (passed) AyeColor else NoColor
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .cardClickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = cardSurfaceColor(verdictColor)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                DivisionCardBody(item, onTagClick)
            }
        }
        return
    }
    CardShell(hasFollowedVotes = hasFollowedVotes, onClick = onClick, modifier = modifier) {
        when (item) {
            is FeedItem.DivisionItem -> DivisionCardBody(item, onTagClick)

            is FeedItem.PublicationItem -> PublicationCardBody(item, onTagClick)

            is FeedItem.StatementItem -> StatementCardBody(item, onTagClick)

            is FeedItem.LegislationItem -> LegislationCardBody(item)

            // Financial, Speech and MpVote render through their own composables.
            else -> Unit
        }
    }
}

/**
 * Shared card container. Applies **no padding** — each architecture owns its
 * own insets so full-bleed images and edge-to-edge bands are possible.
 *
 * Card color is adjusted to stand out more against the background:
 * slightly brighter in dark mode, slightly darker in light mode.
 */
@Composable
private fun CardShell(
    hasFollowedVotes: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val baseColor = MaterialTheme.colorScheme.surfaceContainer
    // Blend toward white (dark mode) or black (light mode) for more contrast
    val adjustedColor = if (isDark) {
        lerp(baseColor, Color.White, 0.04f)
    } else {
        lerp(baseColor, Color.Black, 0.02f)
    }
    val cardColor = if (hasFollowedVotes) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
    } else {
        adjustedColor
    }
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = cardColor
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

/**
 * Metadata line used where a card still wants source and date together.
 * Kept internal so the MP-vote and speech cards can share it.
 */
@Composable
internal fun CardFooter(
    source: String,
    date: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f, fill = false)
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
            }
            Text(
                text = source,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = date,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

/**
 * **Division — verdict-led, attribution band at bottom.**
 *
 * Mirrors the statement card architecture: the outcome word and vote
 * split lead at the top with tags, then a tinted strip at the bottom
 * carries the division title, house, and date — the same pattern as
 * the statement's attribution bar.
 */
@Composable
private fun DivisionCardBody(item: FeedItem.DivisionItem, onTagClick: (String) -> Unit) {
    val division = item.division
    val passed = division.ayeCount > division.noCount
    val verdictColor = if (passed) AyeColor else NoColor

    Column(modifier = Modifier.fillMaxWidth()) {
        // Top row: verdict (left, fixed), tags (middle, scrollable weight),
        // X-Y split (right, fixed) — numbers have priority, tags scroll.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: icon + verdict (fixed width)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.HowToVote,
                    contentDescription = "Division",
                    tint = verdictColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (passed) "PASSED" else "FAILED",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.6.sp,
                    color = verdictColor
                )
            }
            // Middle: tags — take remaining space, horizontally scrollable
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.width(10.dp))
                FeedTagPillRow(
                    tags = item.tags,
                    onTagClick = onTagClick,
                    maxTags = 2,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(10.dp))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
            // Right: X-Y vote split (fixed width, never compressed)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${division.ayeCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AyeColor
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "–",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${division.noCount}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = NoColor
                )
            }
        }

        // Followed votes (if any)
        if (item.followedVotes.isNotEmpty()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                item.followedVotes.take(10).forEach { vote ->
                    val isAye = vote.vote.equals("Aye", ignoreCase = true)
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = vote.memberName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = if (isAye) "Aye" else "No",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isAye) AyeColor else NoColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Bottom: tinted attribution band — title + house left, date right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(verdictColor.copy(alpha = 0.13f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = division.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (division.house == 2) "Lords" else "Commons",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatDivisionDate(division.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * **Publication — letter card.**
 *
 * The publication title leads as the heading. The publishing ministry or
 * organisation sits underneath as sub-text (the sender). A short preview
 * of the letter body follows to draw the reader in. Tags and date close
 * the card in a bottom row.
 */
@Composable
private fun PublicationCardBody(item: FeedItem.PublicationItem, onTagClick: (String) -> Unit) {
    val publication = item.publication

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        // Title — the letter's headline
        Text(
            text = publication.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )

        // Sender — the ministry/organisation as sub-text
        if (publication.organisation.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = publication.organisation,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Letter preview — first few lines of the body text to interest
        // the viewer into reading more. Falls back to the summary if
        // bodyText is not available.
        val preview = publication.bodyText
            ?.takeIf { it.isNotBlank() }
            ?.trim()
            ?.take(280)
            ?: publication.summary
                .takeIf { it.isNotBlank() }
                ?.trim()
        if (!preview.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = preview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tags + date in one row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (item.tags.isNotEmpty()) {
                FeedTagPillRow(
                    tags = item.tags,
                    onTagClick = onTagClick,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = formatDivisionDate(publication.firstPublishedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * **Statement — pull-quote.**
 *
 * Mirrors the speech card's inverted pull-quote design. A large quote
 * glyph opens the card, the statement text runs in italic, and the
 * title + source arrive underneath as an attribution — the statement
 * title (e.g. "NHS Pension Scheme") where the MP's name would be on a
 * speech card, and the member's role (e.g. "Minister of State for
 * Health") as the sub-line.
 */
@Composable
private fun StatementCardBody(item: FeedItem.StatementItem, onTagClick: (String) -> Unit) {
    val statement = item.statement

    Column(modifier = Modifier.fillMaxWidth()) {
        // Quote glyph + tags row — the quote mark on the left, top 3 tags
        // in the empty space on the right.
        // Quote glyph + linked statement label + tags row — all left-aligned
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 16.dp, top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                modifier = Modifier.size(30.dp)
            )
            if (statement.hasLinkedStatements) {
                val linkTypes = statement.linkedStatements
                    ?.map { it.linkType }
                    ?.distinct()
                    ?: listOf("JointStatement")
                val label = linkTypes.joinToString(", ") { formatLinkType(it) }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }
            if (item.tags.isNotEmpty()) {
                FeedTagPillRow(
                    tags = item.tags,
                    onTagClick = onTagClick,
                    maxTags = 3
                )
            }
        }

        // Statement text as the quote body — first paragraph (up to 3 lines,
        // whichever comes first). The full text is on the detail page.
        // Parliamentary filler phrases are stripped so the actual content
        // leads the quote instead of formalities.
        val quoteText = stripStatementFiller(statement.text)
            .replace(Regex("<[^>]+>"), "")
            .trim()
            .takeIf { it.isNotBlank() }
            ?: statement.title

        // Extract the first paragraph — split on double newline or first
        // sentence boundary. Show the full first paragraph, capped at 3 lines.
        val firstParagraph = quoteText.split(Regex("\\n\\n+")).firstOrNull()?.trim() ?: quoteText

        Text(
            text = firstParagraph,
            style = MaterialTheme.typography.bodyLarge.copy(fontStyle = FontStyle.Italic),
            lineHeight = 25.sp,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 20.dp, end = 18.dp, top = 2.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Attribution bar — tinted strip matching the speech card.
        // Statement title where MP name would be, member role underneath.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = statement.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = statement.memberRole.ifBlank { statement.answeringBodyName },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = formatDivisionDate(statement.dateMade),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * **Legislation — docket row.**
 *
 * The title leads at body weight. Underneath, the legislation type and tags
 * run inline with a leading glyph, and the date closes the card
 * right-aligned. Deliberately the shortest card in the feed.
 */
@Composable
private fun LegislationCardBody(item: FeedItem.LegislationItem) {
    val legislation = item.legislation

    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = legislation.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Gavel,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = formatLegislationType(legislation.type),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (item.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.tags.take(3).joinToString(" · ") { it.tag },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = true)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = formatDivisionDate(legislation.date),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
