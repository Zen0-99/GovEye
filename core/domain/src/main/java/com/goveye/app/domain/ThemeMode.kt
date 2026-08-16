package com.goveye.app.domain

/**
 * App-wide light/dark mode preference (D-10).
 *
 * Defined in :core:domain so both :core:data (DataStore preferences) and
 * :core:ui (theme resolution) can reference it without a circular dependency.
 */
enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}
