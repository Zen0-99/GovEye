package com.goveye.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.preference.DownloadPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel exposing download preference state to the Settings screen.
 *
 * Created at the Activity level so [com.goveye.app.ui.screens.SettingsScreen]
 * can read and toggle the WiFi-only setting. When the setting changes, the
 * caller is responsible for re-scheduling WorkManager workers with the new
 * network constraint.
 *
 * Initial values are read synchronously via [runBlocking] so the correct
 * toggle state is shown on the very first frame.
 */
@HiltViewModel
class DownloadSettingsViewModel
@Inject
constructor(private val downloadPreferences: DownloadPreferences) :
    ViewModel() {

    private val initialWifiOnly: Boolean = runBlocking {
        downloadPreferences.wifiOnly.first()
    }

    val wifiOnly: StateFlow<Boolean> =
        downloadPreferences.wifiOnly
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialWifiOnly
            )

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { downloadPreferences.setWifiOnly(enabled) }
    }
}
