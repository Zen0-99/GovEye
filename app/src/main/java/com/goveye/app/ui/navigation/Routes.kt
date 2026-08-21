package com.goveye.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for the 4-tab bottom nav (D-14, D-19).
 *
 * Each route is a `@Serializable data object` implementing [NavKey] so
 * Nav3 can save/restore the back stack across process death.
 */
@Serializable
data object FeedRoute : NavKey

@Serializable
data object DirectoryRoute : NavKey

@Serializable
data object FollowingRoute : NavKey

@Serializable
data object SettingsRoute : NavKey

@Serializable
data class ProfileRoute(val memberId: Int) : NavKey

@Serializable
data class DivisionDetailRoute(val divisionId: Int, val house: Int = 1) : NavKey

@Serializable
data class BillDetailRoute(val billId: Int) : NavKey

@Serializable
data class InterestBucketDetailRoute(val memberId: Int, val bucketLabel: String) : NavKey

@Serializable
data class TranscriptRoute(val divisionId: Int, val divisionTitle: String) : NavKey

@Serializable
data class PartyRoute(val partyId: Int) : NavKey
