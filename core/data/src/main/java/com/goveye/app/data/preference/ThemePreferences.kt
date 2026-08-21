package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.goveye.app.domain.AppTheme
import com.goveye.app.domain.ThemeMode
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed theme preferences (D-05, D-06, D-10).
 *
 * Exposes reactive flows for theme mode (light/dark/system), color scheme
 * (Sky — grayscale + blue/white background), and the AMOLED toggle.
 * Suspend setters persist changes so the theme updates live without an
 * Activity recreate.
 */
@Singleton
class ThemePreferences
@Inject
constructor(@Named("theme") private val dataStore: DataStore<Preferences>) {
    val themeMode: Flow<ThemeMode> =
        dataStore.data.map { preferences ->
            preferences[THEME_MODE_KEY]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: DEFAULT_THEME_MODE
        }

    val appTheme: Flow<AppTheme> =
        dataStore.data.map { preferences ->
            preferences[APP_THEME_KEY]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() }
                ?: DEFAULT_APP_THEME
        }

    val isAmoled: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[AMOLED_KEY] ?: DEFAULT_AMOLED
        }

    val showInfoCards: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[INFO_CARDS_KEY] ?: DEFAULT_INFO_CARDS
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[THEME_MODE_KEY] = mode.name }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        dataStore.edit { it[APP_THEME_KEY] = theme.name }
    }

    suspend fun setAmoled(enabled: Boolean) {
        dataStore.edit { it[AMOLED_KEY] = enabled }
    }

    suspend fun setShowInfoCards(enabled: Boolean) {
        dataStore.edit { it[INFO_CARDS_KEY] = enabled }
    }

    private companion object {
        val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        val APP_THEME_KEY = stringPreferencesKey("app_theme")
        val AMOLED_KEY = booleanPreferencesKey("is_amoled")
        val INFO_CARDS_KEY = booleanPreferencesKey("show_info_cards")

        val DEFAULT_THEME_MODE = ThemeMode.SYSTEM
        val DEFAULT_APP_THEME = AppTheme.SKY
        const val DEFAULT_AMOLED = false
        const val DEFAULT_INFO_CARDS = true
    }
}
