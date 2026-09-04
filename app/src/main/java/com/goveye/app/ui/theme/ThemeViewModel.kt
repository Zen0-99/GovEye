package com.goveye.app.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.goveye.app.data.preference.ThemePreferences
import com.goveye.app.domain.AppTheme
import com.goveye.app.domain.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel exposing theme preference state to the UI (D-05, D-06, D-10).
 *
 * Created at the Activity level so both [GovEyeApp] (theme application) and
 * [com.goveye.app.ui.screens.SettingsScreen] (theme picker) share the same
 * instance — changes in Settings propagate live to the theme wrapper.
 *
 * Initial values are read synchronously via [runBlocking] so the correct
 * theme is applied on the very first frame after app launch — no flash of
 * the default system theme before DataStore loads the saved value.
 */
@HiltViewModel
class ThemeViewModel
@Inject
constructor(private val themePreferences: ThemePreferences) : ViewModel() {

    // Read saved values synchronously so the first frame uses the correct theme.
    // This is a one-time blocking read — subsequent updates flow reactively.
    private val initialThemeMode: ThemeMode = runBlocking {
        themePreferences.themeMode.first()
    }
    private val initialAppTheme: AppTheme = runBlocking {
        themePreferences.appTheme.first()
    }
    private val initialAmoled: Boolean = runBlocking {
        themePreferences.isAmoled.first()
    }

    val themeMode: StateFlow<ThemeMode> =
        themePreferences.themeMode
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialThemeMode
            )

    val appTheme: StateFlow<AppTheme> =
        themePreferences.appTheme
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialAppTheme
            )

    val isAmoled: StateFlow<Boolean> =
        themePreferences.isAmoled
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                initialAmoled
            )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch { themePreferences.setAppTheme(theme) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { themePreferences.setAmoled(enabled) }
    }
}
