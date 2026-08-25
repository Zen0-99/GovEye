package com.goveye.app.ui.screens.feed

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Flight
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Paid
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RealEstateAgent
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * The 10 interest buckets, matching the Register of Members' Financial
 * Interests categories 1–10. Must match the `bucket` column values
 * written by `build_interests.py`.
 */
val BUCKET_ORDER = listOf(
    "Employment/Earnings",
    "Financial Support",
    "Gifts",
    "Overseas Visits",
    "Overseas Gifts",
    "Land/Property",
    "Shareholdings",
    "Miscellaneous",
    "Family Employed",
    "Family Lobbying"
)

val BUCKET_ICONS: Map<String, ImageVector> = mapOf(
    "Employment/Earnings" to Icons.Outlined.Paid,
    "Financial Support" to Icons.Outlined.AccountBalance,
    "Gifts" to Icons.Outlined.CardGiftcard,
    "Overseas Visits" to Icons.Outlined.Flight,
    "Overseas Gifts" to Icons.Outlined.Public,
    "Land/Property" to Icons.Outlined.RealEstateAgent,
    "Shareholdings" to Icons.AutoMirrored.Outlined.TrendingUp,
    "Miscellaneous" to Icons.Outlined.Category,
    "Family Employed" to Icons.Outlined.Groups,
    "Family Lobbying" to Icons.Outlined.Business
)

/**
 * Returns the icon for a given bucket label, falling back to a generic
 * category icon if the bucket is not recognized.
 */
fun bucketIcon(bucketLabel: String?): ImageVector = BUCKET_ICONS[bucketLabel] ?: Icons.Outlined.Category
