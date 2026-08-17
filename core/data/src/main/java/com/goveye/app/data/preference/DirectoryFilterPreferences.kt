package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DirectoryFilterPreferences @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    // Party filter — multi-select, stored as Set<String>
    val selectedParties: Flow<Set<String>> =
        dataStore.data.map { it[PARTIES_KEY] ?: emptySet() }

    // House filter — single-select, stored as Int (0 = all, 1 = Commons, 2 = Lords)
    val houseFilter: Flow<Int> =
        dataStore.data.map { it[HOUSE_KEY] ?: 1 }  // default: Commons

    // Status filter — single-select, stored as Boolean (true = current only)
    val currentOnly: Flow<Boolean> =
        dataStore.data.map { it[CURRENT_ONLY_KEY] ?: true }  // default: current only

    suspend fun setSelectedParties(parties: Set<String>) {
        dataStore.edit { it[PARTIES_KEY] = parties }
    }

    suspend fun setHouseFilter(house: Int) {
        dataStore.edit { it[HOUSE_KEY] = house }
    }

    suspend fun setCurrentOnly(currentOnly: Boolean) {
        dataStore.edit { it[CURRENT_ONLY_KEY] = currentOnly }
    }

    suspend fun clearAll() {
        dataStore.edit {
            it.remove(PARTIES_KEY)
            it.remove(HOUSE_KEY)
            it.remove(CURRENT_ONLY_KEY)
        }
    }

    private companion object {
        val PARTIES_KEY = stringSetPreferencesKey("directory_filter_parties")
        val HOUSE_KEY = intPreferencesKey("directory_filter_house")
        val CURRENT_ONLY_KEY = booleanPreferencesKey("directory_filter_current_only")
    }
}
