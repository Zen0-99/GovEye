package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * DataStore-backed notification preferences (D-04).
 *
 * Per-type toggles for notification categories. Per-MP mute is stored in
 * the FollowEntity (isMuted column), not here.
 *
 * - votes_enabled: master toggle for vote notifications (default true)
 * - speeches_enabled: master toggle for speech notifications (default false,
 *   disabled in v1 per D-05 — spoke notifications deferred to Phase 8)
 */
@Singleton
class NotificationPreferences @Inject constructor(
    @Named("notification") private val dataStore: DataStore<Preferences>,
) {
    val votesEnabled: Flow<Boolean> =
        dataStore.data.map { it[VOTES_ENABLED_KEY] ?: DEFAULT_VOTES_ENABLED }

    val speechesEnabled: Flow<Boolean> =
        dataStore.data.map { it[SPEECHES_ENABLED_KEY] ?: DEFAULT_SPEECHES_ENABLED }

    suspend fun setVotesEnabled(enabled: Boolean) {
        dataStore.edit { it[VOTES_ENABLED_KEY] = enabled }
    }

    suspend fun setSpeechesEnabled(enabled: Boolean) {
        dataStore.edit { it[SPEECHES_ENABLED_KEY] = enabled }
    }

    suspend fun getVotesEnabled(): Boolean =
        dataStore.data.map { it[VOTES_ENABLED_KEY] ?: DEFAULT_VOTES_ENABLED }.first()

    private companion object {
        val VOTES_ENABLED_KEY = booleanPreferencesKey("votes_enabled")
        val SPEECHES_ENABLED_KEY = booleanPreferencesKey("speeches_enabled")

        const val DEFAULT_VOTES_ENABLED = true
        const val DEFAULT_SPEECHES_ENABLED = false
    }
}
