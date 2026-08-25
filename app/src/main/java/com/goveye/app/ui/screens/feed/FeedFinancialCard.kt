package com.goveye.app.ui.screens.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Feed variant of [UnifiedFinancialCard] — wraps it with [showProfileIcon]
 * enabled so the followed MP's avatar (party-colored border) renders in-line
 * before the amount text.
 *
 * The amount, who/where, description, category, and date are all carried on
 * [item] ([FeedItem.FinancialItem]). The date is formatted to DD/MM/YYYY via
 * [formatDivisionDate].
 */
@Composable
fun FeedFinancialCard(item: FeedItem.FinancialItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    UnifiedFinancialCard(
        amount = item.amount,
        whoOrWhere = item.whoOrWhere,
        description = item.description,
        category = item.category,
        date = formatDivisionDate(item.date),
        isIncome = item.isIncome,
        partyColorHex = item.memberPartyColorHex,
        showProfileIcon = true,
        profileImageUrl = item.memberPhotoUrl,
        profileInitials = item.memberName.take(2).uppercase(),
        expandableContent = item.description.takeIf { it.length > 80 },
        onClick = onClick,
        modifier = modifier
    )
}
