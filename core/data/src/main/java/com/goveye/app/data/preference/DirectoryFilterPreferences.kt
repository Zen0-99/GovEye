package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DirectoryFilterPreferences @Inject constructor(@Named("theme") private val dataStore: DataStore<Preferences>) {
    // Party filter — tri-state per party (included / excluded / disabled)
    // Stored as two sets: included parties and excluded parties.
    val includedParties: Flow<Set<String>> =
        dataStore.data.map { it[INCLUDED_PARTIES_KEY] ?: emptySet() }

    val excludedParties: Flow<Set<String>> =
        dataStore.data.map { it[EXCLUDED_PARTIES_KEY] ?: emptySet() }

    // House filter — single-select, stored as Int (0 = all, 1 = Commons, 2 = Lords)
    val houseFilter: Flow<Int> =
        dataStore.data.map { it[HOUSE_KEY] ?: 0 } // default: all

    // Status filter — single-select, stored as Boolean (true = current only)
    val currentOnly: Flow<Boolean> =
        dataStore.data.map { it[CURRENT_ONLY_KEY] ?: false } // default: include former

    suspend fun setIncludedParties(parties: Set<String>) {
        dataStore.edit { it[INCLUDED_PARTIES_KEY] = parties }
    }

    suspend fun setExcludedParties(parties: Set<String>) {
        dataStore.edit { it[EXCLUDED_PARTIES_KEY] = parties }
    }

    suspend fun setHouseFilter(house: Int) {
        dataStore.edit { it[HOUSE_KEY] = house }
    }

    suspend fun setCurrentOnly(currentOnly: Boolean) {
        dataStore.edit { it[CURRENT_ONLY_KEY] = currentOnly }
    }

    suspend fun clearAll() {
        dataStore.edit {
            it.remove(INCLUDED_PARTIES_KEY)
            it.remove(EXCLUDED_PARTIES_KEY)
            it.remove(HOUSE_KEY)
            it.remove(CURRENT_ONLY_KEY)
        }
    }

    private companion object {
        val INCLUDED_PARTIES_KEY = stringSetPreferencesKey("directory_filter_included_parties")
        val EXCLUDED_PARTIES_KEY = stringSetPreferencesKey("directory_filter_excluded_parties")
        val HOUSE_KEY = intPreferencesKey("directory_filter_house")
        val CURRENT_ONLY_KEY = booleanPreferencesKey("directory_filter_current_only")
    }
}
