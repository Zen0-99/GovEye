package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed onboarding preferences.
 *
 * Tracks whether the user has completed the onboarding flow
 * (Welcome → Government selection → Continue) and which government
 * they selected.
 *
 * Used by [com.goveye.app.MainActivity] to decide whether to show
 * the onboarding screens before the download flow, and to gate the
 * download on a government being selected.
 */
@Singleton
class OnboardingPreferences
@Inject
constructor(@Named("theme") private val dataStore: DataStore<Preferences>) {

    /** When true, the user has completed onboarding and should not see it again. */
    val onboardingCompleted: Flow<Boolean> =
        dataStore.data.map { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] ?: DEFAULT_ONBOARDING_COMPLETED
        }

    /** The government the user selected during onboarding (e.g. "UK"). */
    val selectedGovernment: Flow<String?> =
        dataStore.data.map { preferences ->
            preferences[SELECTED_GOVERNMENT_KEY]
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[ONBOARDING_COMPLETED_KEY] = completed }
    }

    suspend fun setSelectedGovernment(government: String?) {
        dataStore.edit {
            if (government != null) {
                it[SELECTED_GOVERNMENT_KEY] = government
            } else {
                it.remove(SELECTED_GOVERNMENT_KEY)
            }
        }
    }

    // --- Onboarding selections (Phase 14 — 5-step onboarding redesign) ---

    /** Tags selected during onboarding step 2 (Pick your topics). */
    val selectedTags: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[SELECTED_TAGS_KEY] ?: emptySet()
        }

    /** Sources selected during onboarding step 3 (Choose your sources).
     * Each entry is a "{organisationSlug}:{streamType}" pair. */
    val selectedSources: Flow<Set<String>> =
        dataStore.data.map { preferences ->
            preferences[SELECTED_SOURCES_KEY] ?: emptySet()
        }

    /** Parties selected during onboarding step 4 (Follow parties).
     * Stored as a set of partyId strings (DataStore stringSet limitation). */
    val selectedParties: Flow<Set<Int>> =
        dataStore.data.map { preferences ->
            (preferences[SELECTED_PARTIES_KEY] ?: emptySet()).mapNotNull { it.toIntOrNull() }.toSet()
        }

    suspend fun setSelectedTags(tags: Set<String>) {
        dataStore.edit { it[SELECTED_TAGS_KEY] = tags }
    }

    suspend fun setSelectedSources(sources: Set<String>) {
        dataStore.edit { it[SELECTED_SOURCES_KEY] = sources }
    }

    suspend fun setSelectedParties(partyIds: Set<Int>) {
        dataStore.edit { it[SELECTED_PARTIES_KEY] = partyIds.map { it.toString() }.toSet() }
    }

    private companion object {
        val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")
        val SELECTED_GOVERNMENT_KEY = stringPreferencesKey("selected_government")
        val SELECTED_TAGS_KEY = stringSetPreferencesKey("selected_tags")
        val SELECTED_SOURCES_KEY = stringSetPreferencesKey("selected_sources")
        val SELECTED_PARTIES_KEY = stringSetPreferencesKey("selected_parties")
        const val DEFAULT_ONBOARDING_COMPLETED = false
    }
}
