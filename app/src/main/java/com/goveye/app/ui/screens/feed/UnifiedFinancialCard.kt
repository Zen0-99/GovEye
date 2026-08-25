package com.goveye.app.ui.screens.feed

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors

/**
 * A label-value pair for the expandable detail section of a financial card.
 * The [label] is rendered in bold, the [value] in normal weight.
 * The optional [group] renders as a styled sub-heading above the field.
 */
data class FinancialDetailField(val label: String, val value: String, val group: String? = null)

/**
 * Unified financial card — renders income and expense entries with a
 * consistent layout across the activity feed, main feed, and detail views.
 *
 * Layout (top to bottom):
 * 1. Amount (bodyLarge/Bold) + trend pill (top-right, green TrendingUp for
 *    income / red TrendingDown for expense — no text, just the stock icon)
 * 2. "by X" / "for X" subtext — "by "/"for " in normal weight, name in bold
 * 3. Short description if available (bodySmall, 2 lines, truncated, italic)
 * 4. [Expandable] Extra detail section — collapsed by default. Uses
 *    AnimatedVisibility with pure expandVertically/shrinkVertically (no fade)
 *    per SyncStone convention for smooth height morph.
 *    Shows structured fields with bold labels grouped by category
 *    (Payment, Donor, Visit, etc.) with thin dividers between groups,
 *    or fallback plain text.
 * 5. Category icon (gray, varies per category) + short category text (left) + Date (right)
 *    — stays at the bottom, expanding with the card.
 *
 * When [showProfileIcon] is true (feed variant), an [MpAvatar] with a
 * party-colored border renders in-line before the amount text.
 */
@Composable
fun UnifiedFinancialCard(
    amount: String,
    whoOrWhere: String,
    description: String,
    category: String,
    date: String,
    isIncome: Boolean,
    partyColorHex: String?,
    showProfileIcon: Boolean = false,
    profileImageUrl: String? = null,
    profileInitials: String = "",
    expandableContent: String? = null,
    expandableFields: List<FinancialDetailField>? = null,
    bucket: String? = null,
    isUnpaid: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val trendColor = if (isIncome) VoteColors.aye else VoteColors.no
    val pillColor = if (isUnpaid) MaterialTheme.colorScheme.onSurfaceVariant else trendColor
    val categoryIcon = bucketIcon(bucket ?: category)
    val hasExpandable = !expandableContent.isNullOrBlank() || !expandableFields.isNullOrEmpty()
    var expanded by remember { mutableStateOf(false) }
    // Low-ripple interaction source for subtler press feedback
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ),
                onClick = {
                    if (hasExpandable) expanded = !expanded
                    onClick()
                }
            ),
        shape = RoundedCornerShape(16.dp),
        color = pillColor.copy(alpha = 0.08f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Amount + trend pill row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (showProfileIcon) {
                        MpAvatar(
                            thumbnailUrl = profileImageUrl,
                            displayName = profileInitials,
                            partyColorHex = partyColorHex,
                            size = 32.dp,
                            borderWidth = 1.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = amount,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Trend icon — income = TrendingUp (green), expense = TrendingDown (red),
                // unpaid = gray HorizontalRule (dash). No circle — the whole card
                // is tinted with the trend color instead.
                val trendIcon = when {
                    isUnpaid -> Icons.Outlined.HorizontalRule
                    isIncome -> Icons.AutoMirrored.Outlined.TrendingUp
                    else -> Icons.AutoMirrored.Outlined.TrendingDown
                }
                Icon(
                    imageVector = trendIcon,
                    contentDescription = when {
                        isUnpaid -> "Unpaid"
                        isIncome -> "Income"
                        else -> "Expense"
                    },
                    tint = pillColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // 2. "by X" / "for X" subtext — prefix normal, name bold
            if (whoOrWhere.isNotBlank()) {
                val prefix = if (isIncome) "by " else "for "
                // Strip trailing " - £amount" if present (amount already shown in title)
                val cleanName = whoOrWhere.replace(Regex("\\s*[-\u2013]\\s*\u00a3[\\d,.]+\\s*$"), "").trim()
                if (cleanName.isNotBlank()) {
                    Text(
                        text = buildAnnotatedString {
                            append(prefix)
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                append(cleanName)
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 3. Short description (2 lines, truncated, italic for context)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. [Expandable] Extra detail — pure height morph (no fade) per SyncStone
            // convention. Placed BETWEEN description and category/date row so the
            // category/date stays at the bottom and expands with the card.
            AnimatedVisibility(
                visible = expanded && hasExpandable,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (!expandableFields.isNullOrEmpty()) {
                        // Render fields with group sub-headings.
                        // Sub-headings use onSurface (darker) color, semibold,
                        // with a thin divider above (except the first group).
                        var currentGroup: String? = null
                        var groupIndex = 0
                        expandableFields.forEach { field ->
                            if (field.group != null && field.group != currentGroup) {
                                if (groupIndex > 0) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(top = 2.dp),
                                        thickness = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                                currentGroup = field.group
                                groupIndex++
                                Text(
                                    text = field.group,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                        append("${field.label}: ")
                                    }
                                    append(field.value)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (!expandableContent.isNullOrBlank()) {
                        // Fallback: split plain text into paragraphs for readability.
                        // The full summary is shown here — the card's description
                        // line is a truncated preview.
                        expandableContent.split("\n").filter { it.isNotBlank() }.forEach { line ->
                            Text(
                                text = line.trim(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 5. Category icon + short text (left) + Date (right)
            // Stays at the bottom, expanding with the card.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = categoryIcon,
                        contentDescription = category,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                if (date.isNotBlank()) {
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
