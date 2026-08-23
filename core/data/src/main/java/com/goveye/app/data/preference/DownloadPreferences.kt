package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed download preferences for controlling how and when
 * GovEye downloads database updates.
 *
 * Currently exposes:
 * - [wifiOnly]: When true, all database update workers use an
 *   `UNMETERED` network constraint and the seed download refuses to
 *   proceed on metered connections.
 *
 * Designed to be extended with additional download settings in the
 * future (e.g. auto-update toggle, download frequency, etc.).
 */
@Singleton
class DownloadPreferences
@Inject
constructor(@Named("database") private val dataStore: DataStore<Preferences>) {

    /** When true, database updates only happen over WiFi (unmetered). */
    val wifiOnly: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[WIFI_ONLY_KEY] ?: DEFAULT_WIFI_ONLY
        }

    suspend fun setWifiOnly(enabled: Boolean) {
        dataStore.edit { it[WIFI_ONLY_KEY] = enabled }
    }

    private companion object {
        val WIFI_ONLY_KEY = booleanPreferencesKey("wifi_only")
        const val DEFAULT_WIFI_ONLY = false
    }
}
