package com.goveye.app.ui.navigation

import androidx.navigation3.runtime.NavKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Singleton
class DeepLinkNavigator @Inject constructor() {
    private val _deepLinkEvents = MutableSharedFlow<NavKey>(extraBufferCapacity = 1)
    val deepLinkEvents: SharedFlow<NavKey> = _deepLinkEvents

    suspend fun emit(route: NavKey) {
        _deepLinkEvents.emit(route)
    }
}
