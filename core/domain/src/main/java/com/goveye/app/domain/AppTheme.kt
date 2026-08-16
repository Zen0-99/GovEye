package com.goveye.app.domain

/**
 * Selectable color schemes (D-06).
 *
 * Names are neutral — no political references (OPEN-03).
 * CORAL is the default (D-07).
 *
 * SKY is the dynamic color scheme (wallpaper-based on API 31+,
 * Coral fallback on older APIs).
 */
enum class AppTheme {
    FOREST,
    SKY,
    EMBER,
    CORAL
}
