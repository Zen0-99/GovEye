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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel exposing theme preference state to the UI (D-05, D-06, D-10).
 *
 * Created at the Activity level so both [GovEyeApp] (theme application) and
 * [com.goveye.app.ui.screens.SettingsScreen] (theme picker) share the same
 * instance — changes in Settings propagate live to the theme wrapper.
 */
@HiltViewModel
class ThemeViewModel
@Inject
constructor(private val themePreferences: ThemePreferences) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        themePreferences.themeMode
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                ThemeMode.SYSTEM
            )

    val appTheme: StateFlow<AppTheme> =
        themePreferences.appTheme
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                AppTheme.CORAL
            )

    val isAmoled: StateFlow<Boolean> =
        themePreferences.isAmoled
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                false
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
