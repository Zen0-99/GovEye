package com.goveye.app.ui.screens.following

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Process-level cache for the last rendered [FollowingUiState].
 *
 * Same pattern as [com.goveye.app.ui.screens.feed.FeedCache] — the
 * FollowingViewModel is recreated on every tab switch (Nav3 limitation),
 * so without a cache each recreation starts from isLoading=true.
 *
 * The ViewModel uses [cached] as the initial value for its stateIn,
 * showing the cached content immediately while the fresh DB query
 * runs in the background.
 */
object FollowingCache {
    private val _state = MutableStateFlow<FollowingUiState?>(null)

    val cached: FollowingUiState? get() = _state.value

    fun update(state: FollowingUiState) {
        if (!state.isLoading) {
            _state.value = state
        }
    }

    fun clear() {
        _state.value = null
    }
}
