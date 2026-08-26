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
    // Interest buckets (categories 1-10)
    "Employment/Earnings" to Icons.Outlined.Paid,
    "Financial Support" to Icons.Outlined.AccountBalance,
    "Gifts" to Icons.Outlined.CardGiftcard,
    "Overseas Visits" to Icons.Outlined.Flight,
    "Overseas Gifts" to Icons.Outlined.Public,
    "Land/Property" to Icons.Outlined.RealEstateAgent,
    "Shareholdings" to Icons.AutoMirrored.Outlined.TrendingUp,
    "Miscellaneous" to Icons.Outlined.Category,
    "Other" to Icons.Outlined.Category,
    "Family Employed" to Icons.Outlined.Groups,
    "Family Lobbying" to Icons.Outlined.Business,
    // Expense buckets (IPSA categories)
    "Staffing" to Icons.Outlined.Groups,
    "Office" to Icons.Outlined.Business,
    "Travel" to Icons.Outlined.Flight
)

/**
 * Maps short category names (stored in the DB) to icons — used when the
 * bucket label is null but the short category name is available.
 */
val CATEGORY_ICONS: Map<String, ImageVector> = mapOf(
    "Employment" to Icons.Outlined.Paid,
    "Employment (Ad hoc)" to Icons.Outlined.Paid,
    "Employment (Ongoing)" to Icons.Outlined.Paid,
    "Donations" to Icons.Outlined.AccountBalance,
    "Gifts (UK)" to Icons.Outlined.CardGiftcard,
    "Gifts (Non-UK)" to Icons.Outlined.Public,
    "Overseas Visits" to Icons.Outlined.Flight,
    "Property" to Icons.Outlined.RealEstateAgent,
    "Shareholdings" to Icons.AutoMirrored.Outlined.TrendingUp,
    "Miscellaneous" to Icons.Outlined.Category,
    "Family (Employed)" to Icons.Outlined.Groups,
    "Family (Business)" to Icons.Outlined.Business
)

/**
 * Returns the icon for a given bucket label, falling back to the short
 * category name mapping, then to a generic category icon.
 */
fun bucketIcon(bucketLabel: String?): ImageVector =
    BUCKET_ICONS[bucketLabel] ?: CATEGORY_ICONS[bucketLabel] ?: Icons.Outlined.Category

/**
 * Maps short category names (stored in the DB) back to their full
 * Parliament API names, for display in the expanded card view.
 */
val FULL_CATEGORY_NAMES: Map<String, String> = mapOf(
    "Employment" to "Employment and earnings",
    "Employment (Ad hoc)" to "Employment and earnings - Ad hoc payments",
    "Employment (Ongoing)" to "Employment and earnings - Ongoing paid employment",
    "Donations" to "Donations and other support (including loans) for activities as an MP",
    "Gifts (UK)" to "Gifts, benefits and hospitality from UK sources",
    "Gifts (Non-UK)" to "Gifts and benefits from sources outside the UK",
    "Overseas Visits" to "Visits outside the UK",
    "Property" to "Land and property (within or outside the UK)",
    "Shareholdings" to "Shareholdings",
    "Miscellaneous" to "Miscellaneous",
    "Family (Employed)" to "Family members employed and receiving benefit from the Exchequer",
    "Family (Business)" to "Family business interests"
)

/**
 * Returns the full category name for a short name, or the input if no mapping exists.
 */
fun fullCategoryName(shortName: String?): String = shortName?.let { FULL_CATEGORY_NAMES[it] } ?: shortName ?: ""
