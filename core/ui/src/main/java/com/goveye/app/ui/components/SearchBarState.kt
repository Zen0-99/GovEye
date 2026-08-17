package com.goveye.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Global search bar state. Screens configure this via [LocalSearchBarState]
 * to show/hide the global search bar and wire up search behavior.
 *
 * The search bar itself is rendered at the app shell level (GovEyeApp),
 * not per-screen. Each screen just sets its configuration.
 */
data class SearchBarConfig(
    val isVisible: Boolean = false,
    val query: String = "",
    val placeholder: String = "Search…",
    val onQueryChange: (String) -> Unit = {},
    val onFilterClick: (() -> Unit)? = null,
    val hasActiveFilters: Boolean = false,
    val filterChips: List<SearchFilterChip> = emptyList(),
)

/**
 * Mutable holder for the global search bar state.
 * Provided via [LocalSearchBarState] at the app shell level.
 */
class SearchBarStateHolder {
    val config: MutableState<SearchBarConfig> = mutableStateOf(SearchBarConfig())

    fun update(config: SearchBarConfig) {
        this.config.value = config
    }

    fun clear() {
        this.config.value = SearchBarConfig()
    }
}

val LocalSearchBarState = staticCompositionLocalOf<SearchBarStateHolder> {
    error("SearchBarStateHolder not provided")
}

/**
 * Configures the global search bar for a screen.
 * Does NOT clear on dispose — the last config persists so the search bar
 * retains its query/placeholder when returning to a tab screen.
 * Visibility is controlled route-level in GovEyeApp, not via config.isVisible.
 */
@Composable
fun ConfigureSearchBar(
    config: SearchBarConfig,
) {
    val holder = LocalSearchBarState.current
    DisposableEffect(config) {
        holder.update(config)
        onDispose {
            // Keep the last config — don't clear. This prevents janky
            // element-by-element destruction when navigating to detail screens.
        }
    }
}
