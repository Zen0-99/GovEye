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
 * Stores per-API DB version keys in DataStore (DATA-03, D-10a).
 *
 * Six per-API version keys track which patch stream version the local
 * BundledDatabase is at:
 * - [mpsVersion] — mps-latest stream
 * - [commonsVotesVersion] — commons-votes-latest stream
 * - [lordsVotesVersion] — lords-votes-latest stream
 * - [billsVersion] — bills-latest stream
 * - [committeesVersion] — committees-latest stream
 * - [recessVersion] — recess-latest stream
 * - [interestsVersion] — interests-latest stream
 *
 * The [seedVersion] key tracks whether the first-launch seed DB download has
 * been completed (null = first launch, not yet downloaded).
 *
 * Used by [com.goveye.app.data.update.DatabaseUpdateManager] to compare local
 * versions against manifest versions on startup.
 */
@Singleton
class DatabasePreferences @Inject constructor(@Named("database") private val dataStore: DataStore<Preferences>) {
    /** Current local mps stream version, or null if never updated. */
    val mpsVersion: Flow<Int?> = dataStore.data.map { it[MPS_VERSION_KEY] }

    /** Current local Commons votes stream version, or null if never updated. */
    val commonsVotesVersion: Flow<Int?> = dataStore.data.map { it[COMMONS_VOTES_VERSION_KEY] }

    /** Current local Lords votes stream version, or null if never updated. */
    val lordsVotesVersion: Flow<Int?> = dataStore.data.map { it[LORDS_VOTES_VERSION_KEY] }

    /** Current local bills stream version, or null if never updated. */
    val billsVersion: Flow<Int?> = dataStore.data.map { it[BILLS_VERSION_KEY] }

    /** Current local committees stream version, or null if never updated. */
    val committeesVersion: Flow<Int?> = dataStore.data.map { it[COMMITTEES_VERSION_KEY] }

    /** Current local recess stream version, or null if never updated. */
    val recessVersion: Flow<Int?> = dataStore.data.map { it[RECESS_VERSION_KEY] }

    /** Current local interests stream version, or null if never updated. */
    val interestsVersion: Flow<Int?> = dataStore.data.map { it[INTERESTS_VERSION_KEY] }

    /** Current local bio-data stream version, or null if never updated. */
    val bioDataVersion: Flow<Int?> = dataStore.data.map { it[BIO_DATA_VERSION_KEY] }

    /** Current local expenses stream version, or null if never updated. */
    val expensesVersion: Flow<Int?> = dataStore.data.map { it[EXPENSES_VERSION_KEY] }

    /** Current local mp-links stream version, or null if never updated. */
    val mpLinksVersion: Flow<Int?> = dataStore.data.map { it[MP_LINKS_VERSION_KEY] }

    /** Current local manifestos stream version, or null if never updated. */
    val manifestosVersion: Flow<Int?> = dataStore.data.map { it[MANIFESTOS_VERSION_KEY] }

    /** Current local party-stats stream version, or null if never updated. */
    val partyStatsVersion: Flow<Int?> = dataStore.data.map { it[PARTY_STATS_VERSION_KEY] }

    /**
     * Seed DB version — null means first launch (seed DB not yet downloaded).
     * Set to 1 after the first-launch download completes.
     */
    val seedVersion: Flow<Int?> = dataStore.data.map { it[SEED_VERSION_KEY] }

    suspend fun setMpsVersion(version: Int) {
        dataStore.edit { it[MPS_VERSION_KEY] = version }
    }

    suspend fun setCommonsVotesVersion(version: Int) {
        dataStore.edit { it[COMMONS_VOTES_VERSION_KEY] = version }
    }

    suspend fun setLordsVotesVersion(version: Int) {
        dataStore.edit { it[LORDS_VOTES_VERSION_KEY] = version }
    }

    suspend fun setBillsVersion(version: Int) {
        dataStore.edit { it[BILLS_VERSION_KEY] = version }
    }

    suspend fun setCommitteesVersion(version: Int) {
        dataStore.edit { it[COMMITTEES_VERSION_KEY] = version }
    }

    suspend fun setRecessVersion(version: Int) {
        dataStore.edit { it[RECESS_VERSION_KEY] = version }
    }

    suspend fun setInterestsVersion(version: Int) {
        dataStore.edit { it[INTERESTS_VERSION_KEY] = version }
    }

    suspend fun setBioDataVersion(version: Int) {
        dataStore.edit { it[BIO_DATA_VERSION_KEY] = version }
    }

    suspend fun setExpensesVersion(version: Int) {
        dataStore.edit { it[EXPENSES_VERSION_KEY] = version }
    }

    suspend fun setMpLinksVersion(version: Int) {
        dataStore.edit { it[MP_LINKS_VERSION_KEY] = version }
    }

    suspend fun setManifestosVersion(version: Int) {
        dataStore.edit { it[MANIFESTOS_VERSION_KEY] = version }
    }

    suspend fun setPartyStatsVersion(version: Int) {
        dataStore.edit { it[PARTY_STATS_VERSION_KEY] = version }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[SEED_VERSION_KEY] = version }
    }

    /**
     * Last notified division ID — tracks the highest division ID for which
     * vote notifications have been dispatched (D-09). Used by VotePollingWorker
     * to detect new divisions after a votes patch.
     */
    val lastNotifiedDivisionId: Flow<Int?> = dataStore.data.map { it[LAST_NOTIFIED_DIVISION_ID_KEY] }

    suspend fun setLastNotifiedDivisionId(id: Int) {
        dataStore.edit { it[LAST_NOTIFIED_DIVISION_ID_KEY] = id }
    }

    /**
     * Last known bill stages — JSON map of billId → currentStageDescription
     * for all followed bills (D-09, BILLS-04). Used by BillPollingWorker to
     * detect stage changes after a bills patch.
     */
    val lastNotifiedBillStages: Flow<String?> = dataStore.data.map { it[LAST_NOTIFIED_BILL_STAGES_KEY] }

    suspend fun setLastNotifiedBillStages(json: String) {
        dataStore.edit { it[LAST_NOTIFIED_BILL_STAGES_KEY] = json }
    }

    private companion object {
        val MPS_VERSION_KEY = intPreferencesKey("mps_version")
        val COMMONS_VOTES_VERSION_KEY = intPreferencesKey("commons_votes_version")
        val LORDS_VOTES_VERSION_KEY = intPreferencesKey("lords_votes_version")
        val BILLS_VERSION_KEY = intPreferencesKey("bills_version")
        val COMMITTEES_VERSION_KEY = intPreferencesKey("committees_version")
        val RECESS_VERSION_KEY = intPreferencesKey("recess_version")
        val INTERESTS_VERSION_KEY = intPreferencesKey("interests_version")
        val BIO_DATA_VERSION_KEY = intPreferencesKey("bio_data_version")
        val EXPENSES_VERSION_KEY = intPreferencesKey("expenses_version")
        val MP_LINKS_VERSION_KEY = intPreferencesKey("mp_links_version")
        val MANIFESTOS_VERSION_KEY = intPreferencesKey("manifestos_version")
        val PARTY_STATS_VERSION_KEY = intPreferencesKey("party_stats_version")
        val SEED_VERSION_KEY = intPreferencesKey("seed_version")
        val LAST_NOTIFIED_DIVISION_ID_KEY = intPreferencesKey("last_notified_division_id")
        val LAST_NOTIFIED_BILL_STAGES_KEY = stringPreferencesKey("last_notified_bill_stages")
    }
}
