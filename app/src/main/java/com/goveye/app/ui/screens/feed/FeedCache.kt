package com.goveye.app.ui.screens.feed

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-level cache for the last rendered [FeedUiState].
 *
 * The FeedViewModel is recreated on every tab switch (Nav3's
 * rememberViewModelStoreNavEntryDecorator doesn't preserve the
 * ViewModelStore across tab changes). Without this cache, each
 * recreation starts from [FeedUiState] with isLoading=true, causing
 * a loading spinner/skeleton on every navigation — even though the
 * data hasn't changed.
 *
 * This object holds the last non-loading state in a [MutableStateFlow].
 * The ViewModel uses [cached] as the initial value for its stateIn,
 * so when it's recreated, it immediately shows the cached content
 * while the fresh DB query runs in the background. Once the fresh
 * query emits, it updates the cache.
 *
 * The cache is cleared when [clear] is called (e.g., on app process
 * death or when the user changes their government selection).
 */
object FeedCache {
    private val _state = MutableStateFlow<FeedUiState?>(null)

    /** The last non-loading FeedUiState, or null if no state has been cached. */
    val cached: FeedUiState? get() = _state.value

    /** Update the cache with a new state. Only caches non-loading states. */
    fun update(state: FeedUiState) {
        if (!state.isLoading) {
            _state.value = state
        }
    }

    /** Clear the cache — used on app process death or government change. */
    fun clear() {
        _state.value = null
    }
}
