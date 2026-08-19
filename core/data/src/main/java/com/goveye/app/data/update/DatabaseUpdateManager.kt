package com.goveye.app.data.update

import android.content.Context
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.preference.DatabasePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Core orchestrator for the bundled DB update flow (D-04, D-05, DATA-01, DATA-03).
 *
 * On startup, [checkForUpdates] fetches manifest.json from the goveye-data
 * repo's `database-latest` release and compares the version against the local
 * DB version stored in [DatabasePreferences].
 *
 * - First launch (no local DB) → [DatabaseUpdateState.NeedsFullDownload]
 * - Same version → [DatabaseUpdateState.UpToDate]
 * - Exactly 1 behind → [DatabaseUpdateState.NeedsPatch] (patch applied via Room transaction)
 * - Multiple behind → [DatabaseUpdateState.NeedsFullDownload] (full DB swap)
 *
 * Patch application ([applyPatch]) and full DB download/swap
 * ([downloadFullDb], [swapDbFile]) are implemented in Task 2.
 */
@Singleton
class DatabaseUpdateManager @Inject constructor(
    private val updateApi: DatabaseUpdateApi,
    private val preferences: DatabasePreferences,
    private val json: Json,
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    /**
     * Checks whether this is the first app launch (no local DB file exists).
     *
     * Uses [Context.getDatabasePath] to check Room's expected DB location.
     */
    fun isFirstLaunch(): Boolean = !context.getDatabasePath(GovEyeDatabase.DATABASE_NAME).exists()

    /**
     * Fetches manifest.json and compares the version against the local DB version.
     *
     * Returns the appropriate [DatabaseUpdateState]:
     * - null local version → NeedsFullDownload(null) (first launch)
     * - same version → UpToDate
     * - manifest.previousVersion == localVersion → NeedsPatch
     * - else → NeedsFullDownload (multiple versions behind)
     *
     * Wraps network errors in [DatabaseUpdateState.Failed].
     */
    suspend fun checkForUpdates(): DatabaseUpdateState {
        return try {
            val localVersion = preferences.dbVersion.first()

            // First launch — no DB has been downloaded yet
            if (localVersion == null) {
                return DatabaseUpdateState.NeedsFullDownload(null)
            }

            // Fetch the database-latest release from goveye-data (D-06)
            val release = updateApi.getLatestRelease()
            val manifestAsset = release.assets.find { it.name == MANIFEST_ASSET_NAME }
                ?: return DatabaseUpdateState.Failed("manifest.json asset not found in release")

            // Download and parse manifest.json (~200B)
            val manifestJson = downloadAssetText(manifestAsset.browserDownloadUrl)
            val manifest = json.decodeFromString<DatabaseManifest>(manifestJson)

            when {
                manifest.version == localVersion ->
                    DatabaseUpdateState.UpToDate
                manifest.previousVersion == localVersion ->
                    DatabaseUpdateState.NeedsPatch(manifest)
                else ->
                    DatabaseUpdateState.NeedsFullDownload(manifest)
            }
        } catch (e: IOException) {
            DatabaseUpdateState.Failed(e.message ?: "Network error")
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Downloads a small text asset (manifest.json or patch.json) via OkHttpClient.
     */
    private fun downloadAssetText(url: String): String {
        val request = Request.Builder().url(url).build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching asset: $url")
            }
            return response.body.string()
                .ifEmpty { throw IOException("Empty response body for asset: $url") }
        }
    }

    companion object {
        internal const val MANIFEST_ASSET_NAME = "manifest.json"
        internal const val PATCH_ASSET_NAME = "patch.json"
        internal const val DB_ASSET_NAME = "goveye.db"
    }
}
