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
data class MpProfileRoute(val memberId: Int) : NavKey
