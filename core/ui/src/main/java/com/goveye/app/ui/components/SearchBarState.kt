package com.goveye.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A segment in a [SearchBarConfig.segments] segmented control (Miko-style
 * double pill). The selected segment gets a filled pill background.
 */
data class SearchSegment(val label: String, val isSelected: Boolean, val onClick: () -> Unit)

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
    /** Optional back button shown to the left of the search bar. */
    val onBack: (() -> Unit)? = null,
    /** Optional segmented control (double pill) shown below the search bar. */
    val segments: List<SearchSegment> = emptyList(),
    /** Whether the search bar is actively focused (pills expanded). */
    val isSearchActive: Boolean = false,
    /** Called when the search bar gains/loses focus. */
    val onSearchActiveChange: (Boolean) -> Unit = {}
)

/**
 * An action icon shown in the [DetailTopBarConfig].
 */
data class DetailTopBarAction(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit,
    val tint: androidx.compose.ui.graphics.Color? = null
)

/**
 * Configuration for the detail top bar — the Miko-style shared toolbar
 * that replaces the search bar on detail screens (profile, bill, etc.).
 *
 * Like [SearchBarConfig], this is rendered at the app shell level.
 * Detail screens configure it via [ConfigureDetailTopBar].
 */
data class DetailTopBarConfig(
    val title: String = "",
    val onBack: () -> Unit = {},
    val actions: List<DetailTopBarAction> = emptyList(),
    /** Optional party color for a subtle gradient accent (profile screens). */
    val accentColor: androidx.compose.ui.graphics.Color? = null,
    /** Icon tint for back button and actions (use white over gradients). */
    val iconTint: androidx.compose.ui.graphics.Color? = null
)

/**
 * Mutable holder for the global top bar state.
 * Provided via [LocalSearchBarState] at the app shell level.
 *
 * Holds both search bar config and detail top bar config. The shell
 * decides which to render based on the current route.
 */
class SearchBarStateHolder {
    val config: MutableState<SearchBarConfig> = mutableStateOf(SearchBarConfig())
    val detailConfig: MutableState<DetailTopBarConfig> = mutableStateOf(DetailTopBarConfig())

    fun update(config: SearchBarConfig) {
        this.config.value = config
    }

    fun updateDetail(config: DetailTopBarConfig) {
        this.detailConfig.value = config
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
fun ConfigureSearchBar(config: SearchBarConfig) {
    val holder = LocalSearchBarState.current
    DisposableEffect(config) {
        holder.update(config)
        onDispose {
            // Keep the last config — don't clear. This prevents janky
            // element-by-element destruction when navigating to detail screens.
        }
    }
}

/**
 * Configures the detail top bar for a detail screen.
 * Like [ConfigureSearchBar], does NOT clear on dispose so the last config
 * persists when navigating away.
 */
@Composable
fun ConfigureDetailTopBar(config: DetailTopBarConfig) {
    val holder = LocalSearchBarState.current
    DisposableEffect(config) {
        holder.updateDetail(config)
        onDispose { }
    }
}
