package com.goveye.app.ui.screens.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.HorizontalRule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.goveye.app.ui.components.MpAvatar
import com.goveye.app.ui.components.VoteColors
import com.goveye.app.ui.components.cardClickable
import java.util.Locale

/**
 * **MP financial combo card — grouped income/expense entries for one MP.**
 *
 * When a followed MP has multiple financial entries on the same date, they
 * are grouped into this combo card. Layout (redesigned to match
 * [UnifiedFinancialCard] styling):
 * - Top row: MP avatar + name (left, labelMedium/SemiBold), total net sum +
 *   trend icon (right, headlineSmall/Bold in trend color)
 * - Middle: individual entries — source on the left, exact amount on the
 *   right (white)
 * - Bottom row: bucket icon + buckets on the left, date on the right
 * - Card surface tint: trendColor.copy(alpha = 0.08f) — same as non-combo cards
 */
@Composable
fun FeedMpFinancialComboCard(
    item: FeedItem.MpFinancialComboItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProfileClick: (() -> Unit)? = null
) {
    val trendColor = if (item.isIncomeNet) VoteColors.aye else VoteColors.no
    val totalFormatted = formatComboTotal(item.totalAmountPence)
    val trendIcon = when {
        item.totalAmountPence == 0L -> Icons.Outlined.HorizontalRule
        item.isIncomeNet -> Icons.AutoMirrored.Outlined.TrendingUp
        else -> Icons.AutoMirrored.Outlined.TrendingDown
    }

    // Collect unique buckets for the bottom-left label
    val buckets = item.entries.mapNotNull { it.bucket?.takeIf { b -> b.isNotBlank() } }.distinct()
    // Use the first bucket's icon, or a generic category icon
    val bucketIcon = bucketIcon(buckets.firstOrNull() ?: item.entries.firstOrNull()?.category)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .cardClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = trendColor.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Top row — MP avatar + name (left), total sum + trend icon (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MP avatar + name (left) — matches UnifiedFinancialCard styling
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = if (onProfileClick != null) {
                        Modifier.clickable { onProfileClick() }
                    } else {
                        Modifier
                    }
                ) {
                    MpAvatar(
                        thumbnailUrl = item.memberPhotoUrl,
                        displayName = item.memberName,
                        partyColorHex = item.memberPartyColorHex,
                        size = 28.dp,
                        borderWidth = 1.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.memberName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Total sum + trend icon (right) — trend color, not white
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = totalFormatted,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = trendColor,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = trendIcon,
                        contentDescription = if (item.isIncomeNet) "Net income" else "Net expense",
                        tint = trendColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Individual entries — source on the left, exact amount on the right (white)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item.entries.take(10).forEach { entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Source on the left — strip any embedded £ amounts to avoid
                        // showing two numbers for the same entry
                        val sourceText = stripAmounts(entry.whoOrWhere.ifBlank { entry.category })
                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        // Exact amount on the right — white
                        val exactAmount = formatExactAmount(entry.amount, entry.isIncome)
                        Text(
                            text = exactAmount,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color.White,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom row — bucket icon + buckets on the left, date on the right
            // Same tint strip as UnifiedFinancialCard: pillColor.copy(alpha = 0.10f)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(trendColor.copy(alpha = 0.10f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = bucketIcon,
                        contentDescription = "Bucket",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (buckets.isNotEmpty()) buckets.joinToString(", ") else "Financial Activity",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = formatComboDate(item.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Strips embedded £ amounts from a source string to avoid showing
 * two numbers for the same entry (e.g. "BBC - £485.80" -> "BBC").
 */
private fun stripAmounts(text: String): String = text.replace(Regex("""\s*[-–—]\s*£[\d,]+(?:\.\d+)?\s*"""), " ")
    .replace(Regex("""£[\d,]+(?:\.\d+)?"""), "")
    .replace(Regex("""\s{2,}"""), " ")
    .trim()
    .trim('-', '–', '—')
    .trim()

/**
 * Formats an exact amount string from the entry's formatted amount.
 * Re-parses the rounded "£485" back to pence and formats with pence shown.
 */
private fun formatExactAmount(roundedAmount: String, isIncome: Boolean): String {
    val cleaned = roundedAmount.replace("£", "").replace(",", "").trim()
    val pounds = cleaned.toDoubleOrNull() ?: 0.0
    val pence = (pounds * 100).toLong()
    val absPence = kotlin.math.abs(pence)
    val poundsPart = absPence / 100
    val pencePart = absPence % 100
    val sign = if (isIncome) "+" else "-"
    val formatted = if (pencePart == 0L) {
        String.format(Locale.UK, "%,.0f", poundsPart.toDouble())
    } else {
        String.format(Locale.UK, "%,.2f", poundsPart + pencePart / 100.0)
    }
    return "$sign£$formatted"
}

private fun formatComboTotal(pence: Long): String {
    val absPence = kotlin.math.abs(pence)
    val pounds = absPence / 100.0
    val sign = if (pence >= 0) "+" else "-"
    val pencePart = absPence % 100
    val formatted = if (pencePart == 0L) {
        String.format(Locale.UK, "%,.0f", pounds)
    } else {
        String.format(Locale.UK, "%,.2f", pounds)
    }
    return "$sign£$formatted"
}

private fun formatComboDate(dateString: String): String = try {
    val parts = dateString.split("T").first().split("-")
    "${parts[2]}/${parts[1]}/${parts[0]}"
} catch (e: Exception) {
    dateString
}
