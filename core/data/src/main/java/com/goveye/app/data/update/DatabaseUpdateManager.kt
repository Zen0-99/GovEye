package com.goveye.app.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillFollowEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.local.entity.RemoteKeyEntity
import com.goveye.app.data.preference.DatabasePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
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
 * Patch application ([applyPatch]) downloads patch.json (5-50KB), decodes
 * per-table upsert/delete arrays, and applies them via [DatabaseUpdateDao]
 * inside a single Room transaction (D-05). Full DB download
 * ([downloadFullDb]) streams the ~160MB DB to a temp file, verifies SHA-256,
 * and swaps it in via [swapDbFile] (Pitfall 3).
 */
@Singleton
class DatabaseUpdateManager @Inject constructor(
    private val updateApi: DatabaseUpdateApi,
    private val preferences: DatabasePreferences,
    private val json: Json,
    @ApplicationContext private val context: Context,
    private val database: GovEyeDatabase,
    private val updateDao: DatabaseUpdateDao,
    @Named("dbDownloadClient") private val dbDownloadClient: OkHttpClient,
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
     * Downloads patch.json and applies per-table upsert/delete arrays via a
     * Room transaction (D-05).
     *
     * The transaction ensures atomicity — if any table fails, the entire
     * patch rolls back. After successful application, the local DB version
     * is updated in [DatabasePreferences].
     *
     * @param manifest The manifest pointing to the release containing patch.json.
     * @return [DatabaseUpdateState.UpToDate] on success, [DatabaseUpdateState.Failed] on error.
     */
    suspend fun applyPatch(manifest: DatabaseManifest): DatabaseUpdateState {
        return try {
            // 1. Find patch.json asset in the release
            val release = updateApi.getLatestRelease()
            val patchAsset = release.assets.find { it.name == PATCH_ASSET_NAME }
                ?: return DatabaseUpdateState.Failed("patch.json asset not found in release")

            // 2. Download and parse patch.json (5-50KB)
            val patchJson = downloadAssetText(patchAsset.browserDownloadUrl)
            val patch = json.decodeFromString<DatabasePatch>(patchJson)

            // 3. Apply all table changes in a single Room transaction
            database.withTransaction {
                for ((tableName, changes) in patch.changes) {
                    applyTableChanges(tableName, changes)
                }
            }

            // 4. Update local version
            preferences.setDbVersion(manifest.version)
            preferences.setDbHash(manifest.dbHash)

            DatabaseUpdateState.UpToDate
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Patch application failed")
        }
    }

    /**
     * Downloads the full DB (~160MB), verifies SHA-256, and swaps it in (D-05, Pitfall 3).
     *
     * Checks for metered connection first (Pitfall 4) — returns
     * [DatabaseUpdateState.NeedsWifi] if on mobile data.
     *
     * @param manifest The manifest containing the expected dbHash and version.
     * @param onProgress Callback receiving download progress as 0f..1f.
     * @return [DatabaseUpdateState.UpToDate] on success, [DatabaseUpdateState.Failed] or [DatabaseUpdateState.NeedsWifi] on error.
     */
    suspend fun downloadFullDb(
        manifest: DatabaseManifest,
        onProgress: (Float) -> Unit = {},
    ): DatabaseUpdateState = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. Check for metered connection (Pitfall 4)
            if (isMeteredConnection()) {
                return@withContext DatabaseUpdateState.NeedsWifi
            }

            // 2. Find goveye.db asset in the release
            val release = updateApi.getLatestRelease()
            val dbAsset = release.assets.find { it.name == DB_ASSET_NAME }
                ?: return@withContext DatabaseUpdateState.Failed("goveye.db asset not found in release")

            // 3. Download to a temp file, streaming with progress
            val tempFile = File(context.cacheDir, "goveye_download.db")
            val request = Request.Builder().url(dbAsset.browserDownloadUrl).build()
            dbDownloadClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext DatabaseUpdateState.Failed("HTTP ${response.code} downloading DB")
                }
                val contentLength = response.body.contentLength()
                response.body.byteStream().use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalRead = 0L
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalRead += bytesRead
                            if (contentLength > 0) {
                                onProgress(totalRead.toFloat() / contentLength)
                            }
                        }
                    }
                }
            }

            // 4. Verify SHA-256 hash (T-10-04-01)
            val actualHash = calculateSha256(tempFile)
            if (!actualHash.equals(manifest.dbHash, ignoreCase = true)) {
                tempFile.delete()
                return@withContext DatabaseUpdateState.Failed("DB hash mismatch")
            }

            // 5. Swap the DB file (Pitfall 3)
            swapDbFile(tempFile)

            // 6. Update preferences
            preferences.setDbVersion(manifest.version)
            preferences.setDbHash(manifest.dbHash)

            DatabaseUpdateState.UpToDate
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Full DB download failed")
        }
    }

    /**
     * Swaps the downloaded DB file into Room's expected location (Pitfall 3).
     *
     * CRITICAL sequence:
     * 1. Close Room — this checkpoints the WAL.
     * 2. Delete old DB files: goveye.db, goveye.db-wal, goveye.db-shm.
     * 3. Move the temp file to the DB path.
     * 4. Room reopens lazily on next query access.
     *
     * If this is the first launch (no existing DB), skip close() and delete.
     */
    private fun swapDbFile(newDbFile: File) {
        val dbPath = context.getDatabasePath(GovEyeDatabase.DATABASE_NAME)
        val firstLaunch = !dbPath.exists()

        if (!firstLaunch) {
            // 1. Close Room — checkpoints WAL
            database.close()

            // 2. Delete old DB + WAL + SHM files
            dbPath.delete()
            File(dbPath.path + "-wal").delete()
            File(dbPath.path + "-shm").delete()
        }

        // 3. Move the temp file to the DB path
        dbPath.parentFile?.mkdirs()
        newDbFile.renameTo(dbPath)

        // 4. Room reopens lazily on next query — no manual reopen needed
    }

    /**
     * Applies upsert/delete arrays for a single table within the current transaction.
     */
    private suspend fun applyTableChanges(tableName: String, changes: TableChanges) {
        // Upsert: decode JsonObjects into the entity type and batch-upsert
        if (changes.upsert.isNotEmpty()) {
            val upsertList = changes.upsert.map { it.jsonObject }
            when (tableName) {
                "mps" -> updateDao.upsertMps(upsertList.map { json.decodeFromJsonElement<MpEntity>(it) })
                "divisions" -> updateDao.upsertDivisions(upsertList.map { json.decodeFromJsonElement<DivisionEntity>(it) })
                "division_votes" -> updateDao.upsertDivisionVotes(upsertList.map { json.decodeFromJsonElement<DivisionVoteEntity>(it) })
                "committees" -> updateDao.upsertCommittees(upsertList.map { json.decodeFromJsonElement<CommitteeEntity>(it) })
                "mp_committee_cross_ref" -> updateDao.upsertMpCommitteeCrossRef(upsertList.map { json.decodeFromJsonElement<MpCommitteeCrossRef>(it) })
                "bills" -> updateDao.upsertBills(upsertList.map { json.decodeFromJsonElement<BillEntity>(it) })
                "bill_stages" -> updateDao.upsertBillStages(upsertList.map { json.decodeFromJsonElement<BillStageEntity>(it) })
                "bill_follows" -> updateDao.upsertBillFollows(upsertList.map { json.decodeFromJsonElement<BillFollowEntity>(it) })
                "hansard_contributions" -> updateDao.upsertHansardContributions(upsertList.map { json.decodeFromJsonElement<HansardContributionEntity>(it) })
                "interests" -> updateDao.upsertInterests(upsertList.map { json.decodeFromJsonElement<InterestEntity>(it) })
                "follows" -> updateDao.upsertFollows(upsertList.map { json.decodeFromJsonElement<FollowEntity>(it) })
                "recess_dates" -> updateDao.upsertRecessDates(upsertList.map { json.decodeFromJsonElement<RecessDateEntity>(it) })
                "recess_dates_meta" -> updateDao.upsertRecessDatesMeta(upsertList.map { json.decodeFromJsonElement<RecessDatesMetaEntity>(it) })
                "mp_notification_prefs" -> updateDao.upsertMpNotificationPrefs(upsertList.map { json.decodeFromJsonElement<MpNotificationPreferenceEntity>(it) })
                "remote_keys" -> updateDao.upsertRemoteKeys(upsertList.map { json.decodeFromJsonElement<RemoteKeyEntity>(it) })
                // mps_fts is NOT handled — auto-synced by FTS4 triggers (Pitfall 2)
            }
        }

        // Delete: extract primary key fields and call the delete method
        for (deleteRow in changes.delete) {
            val obj = deleteRow.jsonObject
            when (tableName) {
                "mps" -> updateDao.deleteMp(obj["id"]!!.jsonPrimitive.intOrNull!!)
                "divisions" -> updateDao.deleteDivision(obj["id"]!!.jsonPrimitive.intOrNull!!)
                "division_votes" -> updateDao.deleteDivisionVote(
                    obj["divisionId"]!!.jsonPrimitive.intOrNull!!,
                    obj["memberId"]!!.jsonPrimitive.intOrNull!!,
                )
                "committees" -> updateDao.deleteCommittee(obj["id"]!!.jsonPrimitive.intOrNull!!)
                "mp_committee_cross_ref" -> updateDao.deleteMpCommitteeCrossRef(
                    obj["memberId"]!!.jsonPrimitive.intOrNull!!,
                    obj["committeeId"]!!.jsonPrimitive.intOrNull!!,
                )
                "bills" -> updateDao.deleteBill(obj["id"]!!.jsonPrimitive.intOrNull!!)
                "bill_stages" -> updateDao.deleteBillStage(
                    obj["billId"]!!.jsonPrimitive.intOrNull!!,
                    obj["stageId"]!!.jsonPrimitive.intOrNull!!,
                )
                "bill_follows" -> updateDao.deleteBillFollow(obj["billId"]!!.jsonPrimitive.intOrNull!!)
                "hansard_contributions" -> updateDao.deleteHansardContribution(obj["itemId"]!!.jsonPrimitive.longOrNull!!)
                "interests" -> updateDao.deleteInterest(obj["id"]!!.jsonPrimitive.intOrNull!!)
                "follows" -> updateDao.deleteFollow(obj["memberId"]!!.jsonPrimitive.intOrNull!!)
                "recess_dates" -> updateDao.deleteRecessDate(obj["id"]!!.jsonPrimitive.longOrNull!!)
                "recess_dates_meta" -> updateDao.deleteRecessDatesMeta(obj["house"]!!.jsonPrimitive.intOrNull!!)
                "mp_notification_prefs" -> updateDao.deleteMpNotificationPref(obj["memberId"]!!.jsonPrimitive.intOrNull!!)
                "remote_keys" -> updateDao.deleteRemoteKey(obj["label"]!!.jsonPrimitive.content)
            }
        }
    }

    /**
     * Checks if the current network connection is metered (mobile data).
     * Used to warn the user before a ~160MB download (Pitfall 4).
     */
    private fun isMeteredConnection(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return true
        val caps = cm.getNetworkCapabilities(network) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Downloads a small text asset (manifest.json or patch.json) via OkHttpClient.
     */
    private fun downloadAssetText(url: String): String {
        val request = Request.Builder().url(url).build()
        dbDownloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching asset: $url")
            }
            return response.body.string()
                .ifEmpty { throw IOException("Empty response body for asset: $url") }
        }
    }

    /**
     * Calculates the SHA-256 hash of a file.
     */
    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        internal const val MANIFEST_ASSET_NAME = "manifest.json"
        internal const val PATCH_ASSET_NAME = "patch.json"
        internal const val DB_ASSET_NAME = "goveye.db"
        private const val BUFFER_SIZE = 8192
    }
}
