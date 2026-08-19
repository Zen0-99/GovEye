package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stores the current bundled DB version and hash in DataStore (DATA-03).
 *
 * Used by [com.goveye.app.data.update.DatabaseUpdateManager] to compare the
 * local version against the manifest version on startup.
 *
 * Follows the [DirectoryPreferences] pattern.
 */
@Singleton
class DatabasePreferences @Inject constructor(
    @Named("database") private val dataStore: DataStore<Preferences>,
) {
    /** Current local DB version, or null if the DB has never been downloaded (first launch). */
    val dbVersion: Flow<Int?> =
        dataStore.data.map { it[DB_VERSION_KEY] }

    /** SHA-256 hash of the current local DB file, or null if not yet set. */
    val dbHash: Flow<String?> =
        dataStore.data.map { it[DB_HASH_KEY] }

    suspend fun setDbVersion(version: Int) {
        dataStore.edit { it[DB_VERSION_KEY] = version }
    }

    suspend fun setDbHash(hash: String) {
        dataStore.edit { it[DB_HASH_KEY] = hash }
    }

    private companion object {
        val DB_VERSION_KEY = intPreferencesKey("db_version")
        val DB_HASH_KEY = stringPreferencesKey("db_hash")
    }
}
