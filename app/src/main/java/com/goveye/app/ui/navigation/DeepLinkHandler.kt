package com.goveye.app.ui.navigation

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.navigation3.runtime.NavKey

/**
 * Parses `goveye://` deep link intents into [NavKey] routes (D-21, D-11).
 *
 * Supported patterns:
 * - `goveye://mp` → Directory tab
 * - `goveye://mp/{memberId}` → MP profile screen for the given member
 * - `goveye://division` → Feed (division detail in later phases)
 * - `goveye://bill` → Feed (bill detail in later phases)
 */
object DeepLinkHandler {
    private const val TAG = "DeepLinkHandler"
    private const val SCHEME = "goveye"

    fun parseDeepLink(intent: Intent): NavKey? {
        val data: Uri = intent.data ?: return null
        if (data.scheme != SCHEME) return null

        val route: NavKey? =
            when (data.host) {
                "mp" -> {
                    val pathSegments = data.pathSegments
                    if (pathSegments.isNotEmpty()) {
                        val memberId = pathSegments.firstOrNull()?.toIntOrNull()
                        if (memberId != null) {
                            ProfileRoute(memberId)
                        } else {
                            DirectoryRoute
                        }
                    } else {
                        DirectoryRoute
                    }
                }

                "division" -> FeedRoute

                "bill" -> FeedRoute

                else -> null
            }

        if (route == null) {
            Log.w(TAG, "Unknown deep link host: ${data.host}")
        } else {
            Log.d(TAG, "Deep link parsed: $data → $route")
        }

        return route
    }
}
