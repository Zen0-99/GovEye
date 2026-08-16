package com.goveye.app.ui.theme.colorscheme

import android.content.Context
import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme

/**
 * Sky color scheme — dynamic wallpaper-based colors (D-06).
 *
 * Uses system [dynamicLightColorScheme] / [dynamicDarkColorScheme] on API 31+
 * (Android S). On older APIs (minSdk 26), falls back to [CoralColorScheme]
 * (the default). No material-color-utilities dependency — just the platform
 * API + fallback.
 */
internal class SkyColorScheme(context: Context) : BaseColorScheme() {
    private val delegate: BaseColorScheme =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SkySystemColorScheme(context)
        } else {
            CoralColorScheme
        }

    override val darkScheme: ColorScheme
        get() = delegate.darkScheme

    override val lightScheme: ColorScheme
        get() = delegate.lightScheme
}

private class SkySystemColorScheme(context: Context) : BaseColorScheme() {
    override val lightScheme: ColorScheme = dynamicLightColorScheme(context)

    override val darkScheme: ColorScheme = dynamicDarkColorScheme(context)
}
