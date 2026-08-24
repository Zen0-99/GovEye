package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ActivityFilterPreferences @Inject constructor(@Named("theme") private val dataStore: DataStore<Preferences>) {
    val enabledActivityTypes: Flow<Set<String>> =
        dataStore.data.map { it[ENABLED_TYPES_KEY] ?: DEFAULT_ENABLED_TYPES }

    suspend fun setEnabledActivityTypes(types: Set<String>) {
        dataStore.edit { it[ENABLED_TYPES_KEY] = types }
    }

    suspend fun clearFilter() {
        dataStore.edit { it.remove(ENABLED_TYPES_KEY) }
    }

    private companion object {
        val ENABLED_TYPES_KEY = stringSetPreferencesKey("activity_filter_enabled_types")
        val DEFAULT_ENABLED_TYPES = setOf("VOTE", "QUESTION", "INCOME", "EXPENSE", "COMMITTEE", "CAREER")
    }
}
