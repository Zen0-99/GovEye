package com.goveye.app.data.update

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.room.withTransaction
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.preference.DatabasePreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
 * Core orchestrator for the bundled DB update flow (D-04, D-05, D-10, D-10a, DATA-01, DATA-03).
 *
 * Implements the hybrid 2-database architecture (D-10a):
 * - Checks 7 per-API manifests (mps-latest, commons-votes-latest,
 *   lords-votes-latest, bills-latest, committees-latest, recess-latest,
 *   interests-latest) in parallel.
 * - Downloads up to 7 patch.json files, merges their changes maps (no conflicts
 *   — each patch only touches its own tables), and applies all to [BundledDatabase]
 *   in a single Room transaction.
 * - First-launch downloads all 7 per-API .db files and merges them on-device
 *   into goveye.db (no separate seed release needed).
 *
 * [LocalDatabase] (local.db, user data) is NEVER touched by this manager —
 * user data persists across all DB updates and DB swaps.
 */
@Singleton
class DatabaseUpdateManager @Inject constructor(
    private val updateApi: DatabaseUpdateApi,
    private val preferences: DatabasePreferences,
    private val json: Json,
    @ApplicationContext private val context: Context,
    private val database: BundledDatabase,
    private val updateDao: DatabaseUpdateDao,
    @Named("dbDownloadClient") private val dbDownloadClient: OkHttpClient
) {
    /**
     * Checks whether this is the first app launch (seed DB not yet downloaded).
     *
     * Returns true if [DatabasePreferences.seedVersion] is null OR the DB file
     * does not exist at Room's expected path.
     */
    suspend fun isFirstLaunch(): Boolean {
        if (preferences.seedVersion.first() == null) return true
        return !context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()
    }

    /**
     * Fetches all 7 per-API manifests in parallel and compares each against the
     * corresponding local version key (D-10, D-10a).
     *
     * Returns the appropriate [DatabaseUpdateState]:
     * - null seed version → [DatabaseUpdateState.NeedsFullDownload] (first launch)
     * - one or more streams 1 behind → [DatabaseUpdateState.NeedsPatches] with
     *   a list of [PatchInfo]
     * - a stream multiple versions behind → [DatabaseUpdateState.NeedsFullDownload]
     *   (fall back to seed DB)
     * - all up to date → [DatabaseUpdateState.UpToDate]
     * - all fetches fail → [DatabaseUpdateState.Failed]
     *
     * Partial failure is graceful — if one stream's fetch fails, the others
     * can still update.
     */
    suspend fun checkForUpdates(): DatabaseUpdateState = withContext(Dispatchers.IO) {
        return@withContext try {
            // First launch — seed DB not yet downloaded
            if (preferences.seedVersion.first() == null) {
                return@withContext DatabaseUpdateState.NeedsFullDownload(null)
            }

            // Fetch all 7 releases in parallel (D-10)
            val results = fetchAllManifests()

            // If all 7 failed, return Failed
            if (results.all { it == null }) {
                return@withContext DatabaseUpdateState.Failed("All manifest fetches failed")
            }

            val patches = mutableListOf<PatchInfo>()
            for ((index, result) in results.withIndex()) {
                if (result == null) continue // partial failure — skip this stream

                val (streamName, manifest) = result
                val localVersion = getLocalVersion(streamName)

                when {
                    manifest.version == localVersion -> {
                        // Up to date — no patch needed
                    }

                    manifest.previousVersion == localVersion -> {
                        // Exactly 1 behind — patch available
                        val release = releases[index]
                        if (release != null) {
                            patches.add(PatchInfo(streamName, manifest, release))
                        }
                    }

                    else -> {
                        // Multiple versions behind — fall back to full per-API download
                        return@withContext DatabaseUpdateState.NeedsFullDownload(null)
                    }
                }
            }

            if (patches.isNotEmpty()) {
                DatabaseUpdateState.NeedsPatches(patches)
            } else {
                DatabaseUpdateState.UpToDate
            }
        } catch (e: IOException) {
            DatabaseUpdateState.Failed(e.message ?: "Network error")
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Unknown error")
        }
    }

    /**
     * Downloads up to 7 patch.json files, merges their changes maps, and applies
     * all to [BundledDatabase] in a single Room transaction (D-10a).
     *
     * Each patch only touches its own tables (mps patch → mps changes, votes
     * patch → divisions/division_votes changes), so merging is conflict-free.
     *
     * After successful application, each stream's version key is updated in
     * [DatabasePreferences].
     *
     * @param patches List of [PatchInfo] from [checkForUpdates].
     * @return [DatabaseUpdateState.UpToDate] on success, [DatabaseUpdateState.Failed] on error.
     */
    suspend fun applyPatches(patches: List<PatchInfo>): DatabaseUpdateState = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. Download and parse all patch.json files, merge changes maps
            val combinedChanges = mutableMapOf<String, TableChanges>()
            for (patchInfo in patches) {
                val patchAsset = patchInfo.release.assets.find { it.name == PATCH_ASSET_NAME }
                    ?: return@withContext DatabaseUpdateState.Failed("patch.json not found for ${patchInfo.streamName}")

                val patchJson = downloadAssetText(patchAsset.browserDownloadUrl)
                val patch = json.decodeFromString<DatabasePatch>(patchJson)

                // Merge changes — each patch touches only its own tables (no conflicts)
                for ((tableName, changes) in patch.changes) {
                    val existing = combinedChanges[tableName]
                    if (existing != null) {
                        combinedChanges[tableName] = TableChanges(
                            upsert = existing.upsert + changes.upsert,
                            delete = existing.delete + changes.delete
                        )
                    } else {
                        combinedChanges[tableName] = changes
                    }
                }
            }

            // 2. Apply all merged changes in a single Room transaction
            database.withTransaction {
                for ((tableName, changes) in combinedChanges) {
                    applyTableChanges(tableName, changes)
                }
            }

            // 3. Update per-API version keys
            for (patchInfo in patches) {
                setStreamVersion(patchInfo.streamName, patchInfo.manifest.version)
            }

            DatabaseUpdateState.UpToDate
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Patch application failed")
        }
    }

    /**
     * Downloads all 7 per-API .db files and merges them on-device into goveye.db
     * for first launch.
     *
     * Instead of downloading a single pre-merged seed DB, this approach:
     * 1. Lets Room create the empty goveye.db with all tables + FTS triggers
     * 2. Closes Room
     * 3. Downloads all 7 per-API .db files to cacheDir (with combined progress)
     * 4. Opens goveye.db with raw SQLite, ATTACHes each per-API DB, and
     *    INSERT OR REPLACE into the corresponding tables
     * 5. VACUUMs to minimize size
     * 6. Room reopens lazily on next query
     *
     * Partial failure is graceful — if one per-API DB download fails, the
     * remaining 6 are still merged (that table set is just empty).
     *
     * Checks for metered connection first (Pitfall 4) — returns
     * [DatabaseUpdateState.NeedsWifi] if on mobile data.
     *
     * @param onProgress Callback receiving download progress as 0f..1f across all 7 downloads.
     * @return [DatabaseUpdateState.UpToDate] on success, [DatabaseUpdateState.Failed]
     *         or [DatabaseUpdateState.NeedsWifi] on error.
     */
    suspend fun downloadAndMergePerApiDbs(onProgress: (Float) -> Unit = {}): DatabaseUpdateState =
        withContext(Dispatchers.IO) {
            return@withContext try {
                // 1. Check for metered connection (Pitfall 4)
                if (isMeteredConnection()) {
                    return@withContext DatabaseUpdateState.NeedsWifi
                }

                // 2. Fetch all 7 per-API releases in parallel
                val releases = fetchAllReleases()
                if (releases.all { it == null }) {
                    return@withContext DatabaseUpdateState.Failed("All per-API release fetches failed")
                }

                // 3. Let Room create the empty goveye.db with all tables + FTS triggers
                //    Opening the DB triggers schema creation. We then close it so we
                //    can open it with raw SQLite for the merge.
                database.openHelper.writableDatabase
                database.close()

                // 4. Download all 7 per-API .db files to cacheDir, tracking combined progress
                val dbFiles = mutableMapOf<String, File>()
                val manifests = mutableMapOf<String, DatabaseManifest>()
                var totalProgress = 0f
                val perStreamProgress = FloatArray(7)

                for ((index, release) in releases.withIndex()) {
                    if (release == null) continue
                    val (streamName, _) = streamTags[index]

                    val dbAsset = release.assets.find { it.name == perApiDbFileName(streamName) }
                    if (dbAsset == null) continue

                    val tempFile = File(context.cacheDir, "${streamName}_download.db")
                    val request = Request.Builder().url(dbAsset.browserDownloadUrl).build()
                    dbDownloadClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            continue
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
                                        perStreamProgress[index] = totalRead.toFloat() / contentLength
                                        totalProgress = perStreamProgress.sum() / 7f
                                        onProgress(totalProgress)
                                    }
                                }
                            }
                        }
                    }

                    // Verify hash if manifest is available
                    val manifestAsset = release.assets.find { it.name == MANIFEST_ASSET_NAME }
                    if (manifestAsset != null) {
                        val manifestJson = downloadAssetText(manifestAsset.browserDownloadUrl)
                        val manifest = json.decodeFromString<DatabaseManifest>(manifestJson)
                        val actualHash = calculateSha256(tempFile)
                        if (actualHash.equals(manifest.dbHash, ignoreCase = true)) {
                            dbFiles[streamName] = tempFile
                            manifests[streamName] = manifest
                        } else {
                            tempFile.delete()
                        }
                    } else {
                        dbFiles[streamName] = tempFile
                    }
                }

                if (dbFiles.isEmpty()) {
                    return@withContext DatabaseUpdateState.Failed("No per-API DBs downloaded successfully")
                }

                // 5. Merge: open goveye.db with raw SQLite, ATTACH + INSERT OR REPLACE
                val dbPath = context.getDatabasePath(BundledDatabase.DATABASE_NAME)
                val mergedDb = SQLiteDatabase.openDatabase(
                    dbPath.path,
                    null,
                    SQLiteDatabase.OPEN_READWRITE
                )

                for ((streamName, dbFile) in dbFiles) {
                    val tables = perApiTables(streamName)
                    mergedDb.execSQL("ATTACH DATABASE '${dbFile.path}' AS src_$streamName")

                    for (table in tables) {
                        // mps_fts is auto-populated by FTS4 triggers — skip direct insert
                        if (table == "mps_fts") continue
                        // Wipe existing data (handles "multiple versions behind" case
                        // where stale rows need to be removed) then copy fresh data
                        mergedDb.execSQL("DELETE FROM $table")
                        mergedDb.execSQL(
                            "INSERT OR REPLACE INTO $table SELECT * FROM src_$streamName.$table"
                        )
                    }

                    mergedDb.execSQL("DETACH DATABASE src_$streamName")
                    dbFile.delete()
                }

                mergedDb.execSQL("VACUUM")
                mergedDb.close()

                // 6. Set per-API version keys from manifests
                for ((streamName, manifest) in manifests) {
                    setStreamVersion(streamName, manifest.version)
                }

                // 7. Mark seed version as complete
                preferences.setSeedVersion(1)

                DatabaseUpdateState.UpToDate
            } catch (e: Exception) {
                DatabaseUpdateState.Failed(e.message ?: "Per-API DB download and merge failed")
            }
        }

    /**
     * Applies upsert/delete arrays for a single table within the current transaction.
     *
     * Only handles the 12 bundled tables — user-data tables (follows,
     * bill_follows, mp_notification_prefs) are in LocalDatabase and never
     * patched (D-10a).
     */
    private suspend fun applyTableChanges(tableName: String, changes: TableChanges) {
        // Upsert: decode JsonObjects into the entity type and batch-upsert
        if (changes.upsert.isNotEmpty()) {
            val upsertList = changes.upsert.map { it.jsonObject }
            when (tableName) {
                "mps" -> updateDao.upsertMps(upsertList.map { json.decodeFromJsonElement<MpEntity>(it) })

                "divisions" -> updateDao.upsertDivisions(
                    upsertList.map {
                        json.decodeFromJsonElement<DivisionEntity>(it)
                    }
                )

                "division_votes" -> updateDao.upsertDivisionVotes(
                    upsertList.map {
                        json.decodeFromJsonElement<DivisionVoteEntity>(it)
                    }
                )

                "committees" -> updateDao.upsertCommittees(
                    upsertList.map {
                        json.decodeFromJsonElement<CommitteeEntity>(it)
                    }
                )

                "mp_committee_cross_ref" -> updateDao.upsertMpCommitteeCrossRef(
                    upsertList.map {
                        json.decodeFromJsonElement<MpCommitteeCrossRef>(it)
                    }
                )

                "bills" -> updateDao.upsertBills(upsertList.map { json.decodeFromJsonElement<BillEntity>(it) })

                "bill_stages" -> updateDao.upsertBillStages(
                    upsertList.map {
                        json.decodeFromJsonElement<BillStageEntity>(it)
                    }
                )

                "hansard_contributions" -> updateDao.upsertHansardContributions(
                    upsertList.map {
                        json.decodeFromJsonElement<HansardContributionEntity>(it)
                    }
                )

                "interests" -> updateDao.upsertInterests(
                    upsertList.map {
                        json.decodeFromJsonElement<InterestEntity>(it)
                    }
                )

                "recess_dates" -> updateDao.upsertRecessDates(
                    upsertList.map {
                        json.decodeFromJsonElement<RecessDateEntity>(it)
                    }
                )

                "recess_dates_meta" -> updateDao.upsertRecessDatesMeta(
                    upsertList.map {
                        json.decodeFromJsonElement<RecessDatesMetaEntity>(it)
                    }
                )
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
                    obj["memberId"]!!.jsonPrimitive.intOrNull!!
                )

                "committees" -> updateDao.deleteCommittee(obj["id"]!!.jsonPrimitive.intOrNull!!)

                "mp_committee_cross_ref" -> updateDao.deleteMpCommitteeCrossRef(
                    obj["memberId"]!!.jsonPrimitive.intOrNull!!,
                    obj["committeeId"]!!.jsonPrimitive.intOrNull!!
                )

                "bills" -> updateDao.deleteBill(obj["id"]!!.jsonPrimitive.intOrNull!!)

                "bill_stages" -> updateDao.deleteBillStage(
                    obj["billId"]!!.jsonPrimitive.intOrNull!!,
                    obj["stageId"]!!.jsonPrimitive.intOrNull!!
                )

                "hansard_contributions" -> updateDao.deleteHansardContribution(
                    obj["itemId"]!!.jsonPrimitive.longOrNull!!
                )

                "interests" -> updateDao.deleteInterest(obj["id"]!!.jsonPrimitive.intOrNull!!)

                "recess_dates" -> updateDao.deleteRecessDate(obj["id"]!!.jsonPrimitive.longOrNull!!)

                "recess_dates_meta" -> updateDao.deleteRecessDatesMeta(obj["house"]!!.jsonPrimitive.intOrNull!!)
            }
        }
    }

    /**
     * Fetches all 7 per-API manifests in parallel (D-10).
     *
     * Returns a list of 7 nullable (streamName, manifest) pairs — null entries
     * indicate streams whose fetch failed (partial failure is OK).
     * Also populates [releases] with the corresponding GithubReleaseDto for
     * each stream.
     */
    private val streamTags = listOf(
        DatabaseUpdateApi.MPS_TAG to "mps",
        DatabaseUpdateApi.COMMONS_VOTES_TAG to "commons-votes",
        DatabaseUpdateApi.LORDS_VOTES_TAG to "lords-votes",
        DatabaseUpdateApi.BILLS_TAG to "bills",
        DatabaseUpdateApi.COMMITTEES_TAG to "committees",
        DatabaseUpdateApi.RECESS_TAG to "recess",
        DatabaseUpdateApi.INTERESTS_TAG to "interests"
    )

    private var releases: Array<GithubReleaseDto?> = arrayOfNulls(7)

    private suspend fun fetchAllManifests(): List<Pair<String, DatabaseManifest>?> = coroutineScope {
        val deferreds = streamTags.mapIndexed { index, (tag, streamName) ->
            async {
                try {
                    val release = updateApi.getReleaseByTag(tag = tag)
                    releases[index] = release
                    val manifestAsset = release.assets.find { it.name == MANIFEST_ASSET_NAME }
                        ?: return@async null
                    val manifestJson = downloadAssetText(manifestAsset.browserDownloadUrl)
                    val manifest = json.decodeFromString<DatabaseManifest>(manifestJson)
                    streamName to manifest
                } catch (e: Exception) {
                    null // partial failure — this stream is skipped
                }
            }
        }
        deferreds.map { it.await() }
    }

    /**
     * Fetches all 7 per-API releases in parallel (for first-launch DB download).
     * Returns a list of 7 nullable GithubReleaseDto — null entries indicate
     * streams whose fetch failed (partial failure is OK).
     */
    private suspend fun fetchAllReleases(): List<GithubReleaseDto?> = coroutineScope {
        val deferreds = streamTags.map { (tag, _) ->
            async {
                try {
                    updateApi.getReleaseByTag(tag = tag)
                } catch (e: Exception) {
                    null
                }
            }
        }
        deferreds.map { it.await() }
    }

    /**
     * Returns the .db file name for a given stream name.
     */
    private fun perApiDbFileName(streamName: String): String = when (streamName) {
        "mps" -> "mps.db"
        "commons-votes" -> "commons_votes.db"
        "lords-votes" -> "lords_votes.db"
        "bills" -> "bills.db"
        "committees" -> "committees.db"
        "recess" -> "recess.db"
        "interests" -> "interests.db"
        else -> "$streamName.db"
    }

    /**
     * Returns the list of tables that a given per-API stream owns.
     * Matches the SOURCE_MAP in the former merge_dbs.py.
     */
    private fun perApiTables(streamName: String): List<String> = when (streamName) {
        "mps" -> listOf("mps", "mps_fts")
        "commons-votes" -> listOf("divisions", "division_votes")
        "lords-votes" -> listOf("divisions", "division_votes")
        "bills" -> listOf("bills", "bill_stages")
        "committees" -> listOf("committees", "mp_committee_cross_ref")
        "recess" -> listOf("recess_dates", "recess_dates_meta")
        "interests" -> listOf("interests")
        else -> emptyList()
    }

    /**
     * Gets the local version for a given stream name.
     */
    private suspend fun getLocalVersion(streamName: String): Int? = when (streamName) {
        "mps" -> preferences.mpsVersion.first()
        "commons-votes" -> preferences.commonsVotesVersion.first()
        "lords-votes" -> preferences.lordsVotesVersion.first()
        "bills" -> preferences.billsVersion.first()
        "committees" -> preferences.committeesVersion.first()
        "recess" -> preferences.recessVersion.first()
        "interests" -> preferences.interestsVersion.first()
        else -> null
    }

    /**
     * Sets the local version for a given stream name.
     */
    private suspend fun setStreamVersion(streamName: String, version: Int) {
        when (streamName) {
            "mps" -> preferences.setMpsVersion(version)
            "commons-votes" -> preferences.setCommonsVotesVersion(version)
            "lords-votes" -> preferences.setLordsVotesVersion(version)
            "bills" -> preferences.setBillsVersion(version)
            "committees" -> preferences.setCommitteesVersion(version)
            "recess" -> preferences.setRecessVersion(version)
            "interests" -> preferences.setInterestsVersion(version)
        }
    }

    /**
     * Checks if the current network connection is metered (mobile data).
     * Used to warn the user before per-API DB downloads (Pitfall 4).
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
        private const val BUFFER_SIZE = 8192
    }
}
