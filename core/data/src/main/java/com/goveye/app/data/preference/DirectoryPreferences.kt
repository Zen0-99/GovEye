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

enum class DirectoryViewMode { LIST, GRID }

@Singleton
class DirectoryPreferences @Inject constructor(
    @Named("theme") private val dataStore: DataStore<Preferences>,
) {
    val viewMode: Flow<DirectoryViewMode> =
        dataStore.data.map { preferences ->
            if (preferences[VIEW_MODE_KEY] == true) DirectoryViewMode.GRID
            else DirectoryViewMode.LIST
        }

    suspend fun setViewMode(mode: DirectoryViewMode) {
        dataStore.edit { it[VIEW_MODE_KEY] = (mode == DirectoryViewMode.GRID) }
    }

    private companion object {
        val VIEW_MODE_KEY = booleanPreferencesKey("directory_view_mode_is_grid")
    }
}
