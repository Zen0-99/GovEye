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

    // Government announcement filters (D-14) — tag/source/department/type
    val tagFilter: Flow<Set<String>> =
        dataStore.data.map { it[TAG_FILTER_KEY] ?: emptySet() }

    val sourceFilter: Flow<Set<String>> =
        dataStore.data.map { it[SOURCE_FILTER_KEY] ?: emptySet() }

    val departmentFilter: Flow<Set<String>> =
        dataStore.data.map { it[DEPARTMENT_FILTER_KEY] ?: emptySet() }

    // Type filter — 0 = All, 1 = Publications, 2 = Statements, 3 = Legislation, 4 = Divisions
    val typeFilter: Flow<Int> =
        dataStore.data.map { it[TYPE_FILTER_KEY] ?: 0 }

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

    suspend fun setTagFilter(tags: Set<String>) {
        dataStore.edit { it[TAG_FILTER_KEY] = tags }
    }

    suspend fun setSourceFilter(sources: Set<String>) {
        dataStore.edit { it[SOURCE_FILTER_KEY] = sources }
    }

    suspend fun setDepartmentFilter(departments: Set<String>) {
        dataStore.edit { it[DEPARTMENT_FILTER_KEY] = departments }
    }

    suspend fun setTypeFilter(type: Int) {
        dataStore.edit { it[TYPE_FILTER_KEY] = type }
    }

    suspend fun clearAll() {
        dataStore.edit {
            it.remove(INCLUDED_PARTIES_KEY)
            it.remove(EXCLUDED_PARTIES_KEY)
            it.remove(HOUSE_KEY)
            it.remove(CURRENT_ONLY_KEY)
            it.remove(TAG_FILTER_KEY)
            it.remove(SOURCE_FILTER_KEY)
            it.remove(DEPARTMENT_FILTER_KEY)
            it.remove(TYPE_FILTER_KEY)
        }
    }

    private companion object {
        val INCLUDED_PARTIES_KEY = stringSetPreferencesKey("directory_filter_included_parties")
        val EXCLUDED_PARTIES_KEY = stringSetPreferencesKey("directory_filter_excluded_parties")
        val HOUSE_KEY = intPreferencesKey("directory_filter_house")
        val CURRENT_ONLY_KEY = booleanPreferencesKey("directory_filter_current_only")
        val TAG_FILTER_KEY = stringSetPreferencesKey("directory_filter_tags")
        val SOURCE_FILTER_KEY = stringSetPreferencesKey("directory_filter_sources")
        val DEPARTMENT_FILTER_KEY = stringSetPreferencesKey("directory_filter_departments")
        val TYPE_FILTER_KEY = intPreferencesKey("directory_filter_type")
    }
}
