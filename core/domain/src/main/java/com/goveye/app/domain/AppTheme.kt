package com.goveye.app.domain

/**
 * App color scheme (D-06).
 *
 * SKY is the sole scheme: grayscale accents with a blue background (dark
 * mode) / white background (light mode). Party colors are applied
 * per-profile via a CompositionLocal override, not via the app theme.
 *
 * The enum is retained (single value) so the DataStore preference key
 * and [com.goveye.app.data.preference.ThemePreferences] remain stable
 * for future re-enabling of additional schemes.
 */
enum class AppTheme {
    SKY
}
