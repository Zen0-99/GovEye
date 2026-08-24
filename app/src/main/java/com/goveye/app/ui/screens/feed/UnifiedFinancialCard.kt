package com.goveye.app.ui.screens.feed

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.theme.parsePartyColor

/**
 * Unified financial card — renders income and expense entries with the
 * UI-SPEC Section 2 LOCKED layout.
 *
 * Layout (top to bottom):
 * 1. Amount (bodyLarge/Bold, weight(1f)) + "Income"/"Expense" label + icon
 *    (top-right, tinted with the official's party color). When
 *    [showProfileIcon] is true (feed variant), an [MpAvatar] with a
 *    party-colored border renders in-line before the amount text.
 * 2. Who/where subtext (bodySmall, onSurfaceVariant, single line)
 * 3. Short description (bodySmall, onSurfaceVariant, 2 lines, truncated)
 * 4. Category (left) + Date (right, DD/MM/YYYY) — Row, SpaceBetween
 *
 * Reused in: MP activity tab, income view, expense view, and the feed
 * (single composable with the [showProfileIcon] parameter).
 *
 * Replaces: ActivityIncomeCard, ActivityExpenseCard, ExpenseBucketCard.
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val partyColor = partyColorHex?.let { parsePartyColor(it) }
        ?: MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
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
            // 1. Amount + label/icon row
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
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isIncome) "Income" else "Expense",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = partyColor
                    )
                    Icon(
                        imageVector = if (isIncome) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                        contentDescription = if (isIncome) "Income" else "Expense",
                        tint = partyColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 2. Who/where subtext
            Text(
                text = whoOrWhere,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // 3. Short description (2 lines, truncated)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            // 4. Category (left) + Date (right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
