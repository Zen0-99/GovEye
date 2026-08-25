package com.goveye.app.ui.screens.feed

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors

/**
 * A label-value pair for the expandable detail section of a financial card.
 * The [label] is rendered in bold, the [value] in normal weight.
 */
data class FinancialDetailField(val label: String, val value: String)

/**
 * Unified financial card — renders income and expense entries with a
 * consistent layout across the activity feed, main feed, and detail views.
 *
 * Layout (top to bottom):
 * 1. Amount (bodyLarge/Bold) + arrow pill (top-right, Aye green for income
 *    / No red for expense — no text, just the arrow icon)
 * 2. "by X" / "for X" subtext — "by "/"for " in normal weight, name in bold
 * 3. Short description if available (bodySmall, 2 lines, truncated)
 * 4. Category icon (gray, varies per category) + category text (left) + Date (right)
 * 5. [Expandable] Extra detail section — collapsed by default. Uses
 *    [animateContentSize] for smooth height transitions (no jump).
 *    Detail fields rendered as bold-label: value pairs.
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val arrowColor = if (isIncome) VoteColors.aye else VoteColors.no
    // Use bucket for icon lookup (maps to distinct icons per category);
    // fall back to category text, then generic icon
    val categoryIcon = bucketIcon(bucket ?: category)
    val hasExpandable = !expandableContent.isNullOrBlank() || !expandableFields.isNullOrEmpty()
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioNoBouncy,
                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                )
            )
            .clickable(onClick = {
                if (hasExpandable) expanded = !expanded
                onClick()
            }),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Amount + arrow pill row
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
                // Arrow pill — income = ArrowUpward (green), expense = ArrowDownward (red)
                Surface(
                    shape = RoundedCornerShape(50),
                    color = arrowColor.copy(alpha = 0.12f)
                ) {
                    Icon(
                        imageVector = if (isIncome) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                        contentDescription = if (isIncome) "Income" else "Expense",
                        tint = arrowColor,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(18.dp)
                    )
                }
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

            // 3. Short description (2 lines, truncated)
            if (description.isNotBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 4. Category icon + text (left) + Date (right)
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

            // 5. Expandable detail — structured fields with bold labels
            if (expanded && hasExpandable) {
                if (!expandableFields.isNullOrEmpty()) {
                    expandableFields.forEach { field ->
                        Text(
                            text = buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                                    append("${field.label}: ")
                                }
                                append(field.value)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                } else if (!expandableContent.isNullOrBlank()) {
                    // Fallback: plain text (for entries without structured fields)
                    Text(
                        text = expandableContent,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
