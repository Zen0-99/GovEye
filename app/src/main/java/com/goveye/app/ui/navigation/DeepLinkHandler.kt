package com.goveye.app.ui.navigation

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.navigation3.runtime.NavKey

/**
 * Parses `goveye://` deep link intents into [NavKey] routes (D-21).
 *
 * Supported hosts:
 * - `goveye://mp` → Directory (MP detail screen in later phases)
 * - `goveye://division` → Feed (division detail in later phases)
 * - `goveye://bill` → Feed (bill detail in later phases)
 *
 * Phase 1 stub: returns the appropriate top-level route. Later phases
 * will parse path/query parameters and push detail screens onto the
 * relevant tab's back stack.
 */
object DeepLinkHandler {
    private const val TAG = "DeepLinkHandler"
    private const val SCHEME = "goveye"

    fun parseDeepLink(intent: Intent): NavKey? {
        val data: Uri = intent.data ?: return null
        if (data.scheme != SCHEME) return null

        val route: NavKey? =
            when (data.host) {
                "mp" -> DirectoryRoute
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
