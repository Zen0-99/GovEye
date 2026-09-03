package com.goveye.app.data.update

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.room.withTransaction
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.CommitteeEntity
import com.goveye.app.data.local.entity.DebateSpeechEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.HansardContributionEntity
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.data.local.entity.MpCommitteeCrossRef
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.local.entity.PartyManifestoEntity
import com.goveye.app.data.local.entity.PartyStatsEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import com.goveye.app.data.local.entity.WrittenQuestionEntity
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.preference.DownloadPreferences
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
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
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
    private val preferences: DatabasePreferences,
    private val downloadPreferences: DownloadPreferences,
    private val json: Json,
    @ApplicationContext private val context: Context,
    private val database: BundledDatabase,
    private val updateDao: DatabaseUpdateDao,
    @Named("dbDownloadClient") private val dbDownloadClient: OkHttpClient,
    @Named("githubDownloadBase") private val githubDownloadBase: String = GITHUB_DOWNLOAD_BASE_DEFAULT
) {
    /**
     * Checks whether this is the first app launch (seed DB not yet downloaded)
     * OR the seed DB is outdated (seedVersion < [CURRENT_SEED_VERSION]).
     *
     * Returns true if [DatabasePreferences.seedVersion] is null, is less than
     * [CURRENT_SEED_VERSION], OR the DB file does not exist at Room's
     * expected path.
     */
    suspend fun isFirstLaunch(): Boolean {
        val current = preferences.seedVersion.first()
        if (current == null) {
            // App reinstall scenario: DB file may exist even though preferences
            // were cleared. Don't force a full redownload if the DB is there.
            return !context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()
        }
        if (current < CURRENT_SEED_VERSION) {
            Log.i(TAG, "Seed DB outdated (v$current < v$CURRENT_SEED_VERSION) — re-downloading")
            return true
        }
        return !context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()
    }

    /**
     * Synchronous check — does the DB file exist at Room's expected path?
     * Used to decide the initial UI state without awaiting a suspend call.
     */
    fun databaseFileExists(): Boolean = context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()

    /**
     * Pre-warms the Room database by running a trivial COUNT query.
     *
     * Forces Room to open the DB file, run any pending migrations, and
     * initialize the InvalidationTracker before the FeedViewModel starts
     * collecting flows. Without this, the first flow emission takes ~2s
     * because all 5 feed flows wait for the DB to open simultaneously.
     *
     * Call this during the splash screen or the update check, in parallel
     * with [checkForUpdates]. The result is discarded — the side effect
     * (DB opened + InvalidationTracker ready) is what matters.
     *
     * No-op if the DB file doesn't exist (first launch — download will
     * create it).
     */
    suspend fun prewarmDatabase() = withContext(Dispatchers.IO) {
        if (!databaseFileExists()) {
            Log.i(TAG, "prewarmDatabase — DB file doesn't exist, skipping")
            return@withContext
        }
        val start = System.currentTimeMillis()
        try {
            val count = database.divisionDao().countDivisions()
            val elapsed = System.currentTimeMillis() - start
            Log.i(TAG, "prewarmDatabase — DB opened in ${elapsed}ms, divisions=$count")
        } catch (e: Exception) {
            Log.w(TAG, "prewarmDatabase — failed (DB will open lazily on first query)", e)
        }
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
            // First launch or outdated seed — need full download.
            // BUT: if the DB file already exists AND seedVersion was previously
            // set (app reinstall scenario), recover by setting seedVersion =
            // CURRENT_SEED_VERSION and falling through to the patch check.
            // This avoids a pointless 600MB redownload when the app is
            // reinstalled but data persists.
            //
            // IMPORTANT: if seedVersion is null AND the DB exists, this is NOT
            // a reinstall — it's an old install from a prior app version that
            // was never properly tracked. The DB data is stale and marking
            // streams as "up to date" would skip all patches. Force a full
            // re-download instead.
            val seedVer = preferences.seedVersion.first()
            if (seedVer == null || seedVer < CURRENT_SEED_VERSION) {
                if (seedVer != null && context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()) {
                    Log.i(TAG, "DB exists but seedVersion=$seedVer — recovering (app reinstall)")
                    preferences.setSeedVersion(CURRENT_SEED_VERSION)
                    // Fall through to patch check — stream versions may also be
                    // missing, but checkForUpdates handles null localVersion
                    // gracefully (skips that stream).
                } else {
                    return@withContext DatabaseUpdateState.NeedsFullDownload(null)
                }
            }

            // Fetch all 7 manifests in parallel via direct GitHub download URLs
            // (bypasses the 60 req/hour unauthenticated API rate limit)
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
                Log.i(
                    TAG,
                    "Stream $streamName: local=$localVersion remote=${manifest.version} prev=${manifest.previousVersion}"
                )

                when {
                    // Local version null — stream was never tracked (new stream
                    // or version key was missing from a prior first-launch /
                    // app reinstall). The data is already in the DB from the
                    // original first-launch merge. Mark it as up to date at the
                    // current manifest version — applying all patches for all
                    // untracked streams at once would OOM the heap. The next
                    // real patch (when new data is published) will apply
                    // normally since the version is now tracked.
                    localVersion == null -> {
                        Log.w(TAG, "Stream $streamName untracked — marking as v${manifest.version}")
                        setStreamVersion(streamName, manifest.version)
                    }

                    manifest.version == localVersion -> {
                        // Up to date — no patch needed
                    }

                    manifest.previousVersion == localVersion -> {
                        // Exactly 1 behind — patch available
                        val (tag, _) = streamTags[index]
                        val patchUrl = "$githubDownloadBase/$tag/$PATCH_ASSET_NAME"
                        patches.add(PatchInfo(streamName, manifest, patchUrl))
                    }

                    else -> {
                        // Multiple versions behind. If the DB file exists, skip
                        // this stream and keep using the existing data — the user
                        // stays on their current data until a patch brings them
                        // back in range. Only force a full re-download on first
                        // launch (no DB file). This prevents the app from killing
                        // Room flows via database.close() during a background
                        // download when the user already has perfectly good data.
                        if (context.getDatabasePath(BundledDatabase.DATABASE_NAME).exists()) {
                            Log.w(
                                TAG,
                                "Stream $streamName multiple versions behind (local=$localVersion, remote=${manifest.version}) — skipping, keeping existing data"
                            )
                            // Mark as current so we don't re-check every launch
                            setStreamVersion(streamName, manifest.version)
                        } else {
                            return@withContext DatabaseUpdateState.NeedsFullDownload(null)
                        }
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
            // 1. Download and parse all patch.json files, merge changes maps.
            //    Large patches (e.g. expenses with 144K rows ≈ 114MB) are streamed
            //    to a temp file and parsed via decodeFromStream to avoid OOM from
            //    loading the entire JSON as a single string.
            val combinedChanges = mutableMapOf<String, TableChanges>()
            for (patchInfo in patches) {
                val patchFile = downloadAssetToFile(patchInfo.patchUrl, "patch_${patchInfo.streamName}.json")
                patchFile.use { scope ->
                    val patch = scope.file.inputStream().use { stream ->
                        json.decodeFromStream<DatabasePatch>(stream)
                    }

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
        } catch (e: OutOfMemoryError) {
            // OOM during JSON tree parsing (e.g. expenses patch with 144K rows).
            // The seed DB is already installed — a failed patch just means stale
            // data for that stream, not a crash. Log and return Failed so the UI
            // can show a retry option instead of killing the app.
            Log.e(TAG, "OOM applying patches (patch too large for heap)", e)
            DatabaseUpdateState.Failed("Patch too large to apply — seed DB is usable but may be stale")
        } catch (e: Exception) {
            DatabaseUpdateState.Failed(e.message ?: "Patch application failed")
        }
    }

    /**
     * Downloads the pre-built seed DB (goveye.db) from the seed-latest
     * GitHub release for first launch.
     *
     * The seed DB is built by CI (build-seed.yml) which:
     * 1. Merges all per-API DBs via merge_dbs.py
     * 2. Runs build_precompute.py to populate mp_stats and peer_averages
     * 3. Publishes goveye.db + seed-manifest.json to the seed-latest release
     *
     * This is simpler and faster than merging per-API DBs on-device:
     * - Single file download (~180MB) with accurate progress
     * - No on-device merge or precompute (those are CI-only steps)
     * - Precomputed stats tables are already in the seed DB
     *
     * After the download, per-API version keys are fetched from each
     * stream's manifest so the update check knows the starting state.
     *
     * Checks for metered connection first (Pitfall 4) — returns
     * [DatabaseUpdateState.NeedsWifi] if on mobile data.
     *
     * @param wifiOnly When non-null, controls whether to refuse downloads on
     *   metered connections. When null, reads the [DownloadPreferences.wifiOnly]
     *   flow at call time.
     * @param onProgress Callback receiving download progress as 0f..1f.
     * @return [DatabaseUpdateState.UpToDate] on success, [DatabaseUpdateState.Failed]
     *         or [DatabaseUpdateState.NeedsWifi] on error.
     */
    suspend fun downloadSeedDb(
        wifiOnly: Boolean? = null,
        onProgress: suspend (Float) -> Unit = {}
    ): DatabaseUpdateState = withContext(Dispatchers.IO) {
        return@withContext try {
            // 1. Resolve the wifiOnly setting — use the provided value or
            //    read from preferences.
            val effectiveWifiOnly = wifiOnly ?: downloadPreferences.wifiOnly.first()

            // 2. Check for metered connection (Pitfall 4) — only when
            //    wifiOnly is enabled. When wifiOnly is false, allow downloads
            //    on any network so the user gets their data regardless.
            if (effectiveWifiOnly && isMeteredConnection()) {
                Log.w(TAG, "Metered connection and wifiOnly enabled — deferring download")
                return@withContext DatabaseUpdateState.NeedsWifi
            }

            // 2. Close Room so it releases the DB file handle.
            //    Room will reopen lazily on the next query with the new DB.
            Log.i(TAG, "Closing Room to replace DB file")
            database.close()

            // 3. Download goveye.db from seed-latest release.
            //    URL: https://github.com/Zen0-99/goveye-data/releases/download/seed-latest/goveye.db
            val dbPath = context.getDatabasePath(BundledDatabase.DATABASE_NAME)
            val seedDbUrl = "$githubDownloadBase/seed-latest/goveye.db"
            Log.i(TAG, "Downloading seed DB from $seedDbUrl")

            val tempFile = File(context.cacheDir, "goveye_seed_download.db")
            val request = Request.Builder().url(seedDbUrl).build()
            try {
                dbDownloadClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext DatabaseUpdateState.Failed(
                            "HTTP ${response.code} downloading seed DB"
                        )
                    }
                    val contentLength = response.body.contentLength()
                    response.body.byteStream().use { input ->
                        tempFile.outputStream().use { output ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var bytesRead: Int
                            var totalRead = 0L
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                // Check for cancellation — WorkManager cancels
                                // the coroutine when cancelUniqueWork is called.
                                ensureActive()
                                output.write(buffer, 0, bytesRead)
                                totalRead += bytesRead
                                if (contentLength > 0) {
                                    onProgress(totalRead.toFloat() / contentLength)
                                }
                            }
                        }
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Clean up partial download
                tempFile.delete()
                Log.i(TAG, "Seed download cancelled — temp file deleted")
                throw e
            }

            Log.i(TAG, "Seed DB downloaded: ${tempFile.length()} bytes")

            // 4. Replace the DB file. Delete any existing DB + WAL + SHM files
            //    first (Room may have created them on a previous failed launch).
            if (dbPath.exists()) dbPath.delete()
            File("${dbPath.path}-wal").delete()
            File("${dbPath.path}-shm").delete()
            tempFile.copyTo(dbPath, overwrite = true)
            tempFile.delete()
            Log.i(TAG, "Seed DB installed at ${dbPath.path}")

            // 5. Fetch per-API manifests to set version keys.
            //    The seed DB is a snapshot — we need to know which version
            //    each stream was at when the seed was built so the update
            //    check can detect incremental patches correctly.
            Log.i(TAG, "Fetching per-API manifests to set version keys")
            val results = fetchAllManifests()
            for (result in results) {
                if (result == null) continue
                val (streamName, manifest) = result
                setStreamVersion(streamName, manifest.version)
                Log.i(TAG, "  $streamName: version ${manifest.version}")
            }

            // 6. Mark seed version as complete
            preferences.setSeedVersion(CURRENT_SEED_VERSION)
            Log.i(TAG, "Seed version set to $CURRENT_SEED_VERSION — first launch complete")

            DatabaseUpdateState.UpToDate
        } catch (e: Exception) {
            Log.e(TAG, "downloadSeedDb failed", e)
            DatabaseUpdateState.Failed(e.message ?: "Seed DB download failed")
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

                "debate_speeches" -> updateDao.upsertDebateSpeeches(
                    upsertList.map {
                        json.decodeFromJsonElement<DebateSpeechEntity>(it)
                    }
                )

                "bio_data" -> updateDao.upsertBioData(
                    upsertList.map {
                        json.decodeFromJsonElement<BioDataEntity>(it)
                    }
                )

                "expenses" -> updateDao.upsertExpenses(
                    upsertList.map {
                        json.decodeFromJsonElement<ExpenseEntity>(it)
                    }
                )

                "mp_links" -> updateDao.upsertMpLinks(
                    upsertList.map {
                        json.decodeFromJsonElement<MpLinkEntity>(it)
                    }
                )

                "party_manifestos" -> updateDao.upsertManifestos(
                    upsertList.map {
                        json.decodeFromJsonElement<PartyManifestoEntity>(it)
                    }
                )

                "party_stats" -> updateDao.upsertPartyStats(
                    upsertList.map {
                        json.decodeFromJsonElement<PartyStatsEntity>(it)
                    }
                )

                "written_questions" -> updateDao.upsertWrittenQuestions(
                    upsertList.map {
                        json.decodeFromJsonElement<WrittenQuestionEntity>(it)
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

                "debate_speeches" -> updateDao.deleteDebateSpeech(
                    obj["debateGid"]!!.jsonPrimitive.content,
                    obj["speechGid"]!!.jsonPrimitive.content
                )

                "bio_data" -> updateDao.deleteBioData(obj["mpId"]!!.jsonPrimitive.intOrNull!!)

                "expenses" -> updateDao.deleteExpense(obj["id"]!!.jsonPrimitive.intOrNull!!)

                "mp_links" -> updateDao.deleteMpLink(obj["mpId"]!!.jsonPrimitive.intOrNull!!)

                "party_manifestos" -> updateDao.deleteManifesto(obj["partyId"]!!.jsonPrimitive.intOrNull!!)

                "party_stats" -> updateDao.deletePartyStats(obj["partyId"]!!.jsonPrimitive.intOrNull!!)

                "written_questions" -> updateDao.deleteWrittenQuestion(obj["id"]!!.jsonPrimitive.intOrNull!!)
            }
        }
    }

    /**
     * Fetches all 7 per-API manifests in parallel (D-10).
     *
     * Returns a list of 7 nullable (streamName, manifest) pairs — null entries
     * indicate streams whose fetch failed (partial failure is OK).
     */
    private val streamTags = listOf(
        DatabaseUpdateApi.MPS_TAG to "mps",
        DatabaseUpdateApi.COMMONS_VOTES_TAG to "commons-votes",
        DatabaseUpdateApi.LORDS_VOTES_TAG to "lords-votes",
        DatabaseUpdateApi.BILLS_TAG to "bills",
        DatabaseUpdateApi.COMMITTEES_TAG to "committees",
        DatabaseUpdateApi.RECESS_TAG to "recess",
        DatabaseUpdateApi.INTERESTS_TAG to "interests",
        DatabaseUpdateApi.DEBATES_TAG to "debates",
        DatabaseUpdateApi.BIO_DATA_TAG to "bio-data",
        DatabaseUpdateApi.EXPENSES_TAG to "expenses",
        DatabaseUpdateApi.MP_LINKS_TAG to "mp-links",
        DatabaseUpdateApi.MANIFESTOS_TAG to "manifestos",
        DatabaseUpdateApi.PARTY_STATS_TAG to "party-stats",
        DatabaseUpdateApi.HISTORICAL_MEMBERS_TAG to "historical-members",
        DatabaseUpdateApi.GOV_PUBLICATIONS_TAG to "gov-publications",
        DatabaseUpdateApi.WRITTEN_STATEMENTS_TAG to "written-statements",
        DatabaseUpdateApi.WRITTEN_QUESTIONS_TAG to "written-questions",
        DatabaseUpdateApi.LEGISLATION_TAG to "legislation"
    )

    private suspend fun fetchAllManifests(): List<Pair<String, DatabaseManifest>?> = coroutineScope {
        val deferreds = streamTags.map { (tag, streamName) ->
            async {
                try {
                    val manifestUrl = "$githubDownloadBase/$tag/$MANIFEST_ASSET_NAME"
                    val manifestJson = downloadAssetText(manifestUrl)
                    val manifest = json.decodeFromString<DatabaseManifest>(manifestJson)
                    streamName to manifest
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to fetch manifest for $streamName: ${e.message}")
                    null // partial failure — this stream is skipped
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
        "debates" -> "debates.db"
        "bio-data" -> "bio_data.db"
        "expenses" -> "expenses.db"
        "mp-links" -> "mp_links.db"
        "manifestos" -> "manifestos.db"
        "party-stats" -> "party_stats.db"
        "historical-members" -> "historical_members.db"
        "gov-publications" -> "gov_publications.db"
        "written-statements" -> "written_statements.db"
        "written-questions" -> "written_questions.db"
        "legislation" -> "legislation.db"
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
        "debates" -> listOf("debate_speeches")
        "bio-data" -> listOf("bio_data")
        "expenses" -> listOf("expenses")
        "mp-links" -> listOf("mp_links")
        "manifestos" -> listOf("party_manifestos")
        "party-stats" -> listOf("party_stats")
        "historical-members" -> listOf("historical_members", "historical_members_fts4")
        "gov-publications" -> listOf("government_publications")
        "written-statements" -> listOf("written_statements")
        "written-questions" -> listOf("written_questions")
        "legislation" -> listOf("legislation")
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
        "bio-data" -> preferences.bioDataVersion.first()
        "expenses" -> preferences.expensesVersion.first()
        "mp-links" -> preferences.mpLinksVersion.first()
        "manifestos" -> preferences.manifestosVersion.first()
        "party-stats" -> preferences.partyStatsVersion.first()
        "historical-members" -> preferences.historicalMembersVersion.first()
        "debates" -> preferences.debatesVersion.first()
        "gov-publications" -> preferences.govPublicationsVersion.first()
        "written-statements" -> preferences.writtenStatementsVersion.first()
        "written-questions" -> preferences.writtenQuestionsVersion.first()
        "legislation" -> preferences.legislationVersion.first()
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
            "bio-data" -> preferences.setBioDataVersion(version)
            "expenses" -> preferences.setExpensesVersion(version)
            "mp-links" -> preferences.setMpLinksVersion(version)
            "manifestos" -> preferences.setManifestosVersion(version)
            "party-stats" -> preferences.setPartyStatsVersion(version)
            "historical-members" -> preferences.setHistoricalMembersVersion(version)
            "debates" -> preferences.setDebatesVersion(version)
            "gov-publications" -> preferences.setGovPublicationsVersion(version)
            "written-statements" -> preferences.setWrittenStatementsVersion(version)
            "written-questions" -> preferences.setWrittenQuestionsVersion(version)
            "legislation" -> preferences.setLegislationVersion(version)
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
     * Downloads a text asset (manifest.json or patch.json) to a temp file.
     *
     * Used for large patches that would cause OOM if loaded as a single string.
     * The caller is responsible for deleting the temp file via [File.use].
     *
     * @param url The URL to download.
     * @param fileName The name for the temp file in the cache directory.
     * @return A [FileUseScope] that auto-deletes the temp file on close.
     */
    private fun downloadAssetToFile(url: String, fileName: String): FileUseScope {
        val request = Request.Builder().url(url).build()
        val tempFile = File(context.cacheDir, fileName)
        dbDownloadClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code} fetching asset: $url")
            }
            response.body.byteStream().use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            }
        }
        if (tempFile.length() == 0L) {
            tempFile.delete()
            throw IOException("Empty response body for asset: $url")
        }
        return FileUseScope(tempFile)
    }

    /**
     * Wrapper that auto-deletes a temp file on [close].
     */
    private class FileUseScope(val file: File) : AutoCloseable {
        override fun close() {
            file.delete()
        }
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
        private const val TAG = "GovEye/DbUpdate"

        /**
         * Current seed DB version. Bump this when the seed DB schema or
         * contents change in a way that requires existing users to
         * re-download the full seed DB (e.g. schema fixes, new tables,
         * corrected data). When this is higher than the user's stored
         * seedVersion, the app treats it as a first launch and re-downloads.
         */
        const val CURRENT_SEED_VERSION = 7

        internal const val MANIFEST_ASSET_NAME = "manifest.json"
        internal const val PATCH_ASSET_NAME = "patch.json"
        private const val BUFFER_SIZE = 8192
        private const val GITHUB_DOWNLOAD_BASE_DEFAULT =
            "https://github.com/Zen0-99/goveye-data/releases/download"
    }
}
