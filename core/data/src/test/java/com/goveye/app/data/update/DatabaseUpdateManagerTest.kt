package com.goveye.app.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.preference.DownloadPreferences
import java.io.IOException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseUpdateManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var database: BundledDatabase
    private lateinit var updateDao: DatabaseUpdateDao
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: DatabasePreferences
    private lateinit var downloadPreferences: DownloadPreferences
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var manager: DatabaseUpdateManager

    /** Maps request paths to response bodies — used by the Dispatcher. */
    private val responseMap = mutableMapOf<String, String>()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, BundledDatabase::class.java)
            .allowMainThreadQueries().build()
        updateDao = database.databaseUpdateDao()
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_db_prefs") }
        )
        preferences = DatabasePreferences(dataStore)
        downloadPreferences = DownloadPreferences(dataStore)
        okHttpClient = OkHttpClient.Builder().build()
        // Point the manager at the MockWebServer so manifest downloads are
        // intercepted instead of hitting GitHub.
        manager = DatabaseUpdateManager(
            preferences = preferences,
            downloadPreferences = downloadPreferences,
            json = json,
            context = context,
            database = database,
            updateDao = updateDao,
            dbDownloadClient = okHttpClient,
            githubDownloadBase = server.url("/").toString().trimEnd('/')
        )

        // Use a Dispatcher so parallel requests get the correct response by path
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path?.removePrefix("/") ?: ""
                val body = responseMap[path]
                return if (body != null) {
                    MockResponse().setResponseCode(200).setBody(body)
                } else {
                    MockResponse().setResponseCode(404)
                }
            }
        }
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    /**
     * Registers a manifest.json body for the given tag's path on the MockWebServer.
     * The manager downloads from `$githubDownloadBase/$tag/manifest.json`.
     */
    private fun mockManifest(tag: String, manifestJson: String) {
        responseMap["$tag/manifest.json"] = manifestJson
    }

    /**
     * Simulates a manifest fetch failure by not registering a response
     * (the Dispatcher returns 404 for unregistered paths).
     */
    private fun mockManifestFailure(tag: String) {
        responseMap.remove("$tag/manifest.json")
    }

    private fun manifestJson(version: Int, previousVersion: Int? = null): String = """
        {
            "version": $version,
            "previousVersion": ${previousVersion ?: "null"},
            "schemaVersion": 1,
            "generatedAt": "2026-08-19T12:00:00Z",
            "dbHash": "abc123",
            "dbSize": 160000000,
            "patchHash": "def456",
            "patchSize": 50000
        }
    """.trimIndent()

    private val allTags = listOf(
        DatabaseUpdateApi.MPS_TAG,
        DatabaseUpdateApi.COMMONS_VOTES_TAG,
        DatabaseUpdateApi.LORDS_VOTES_TAG,
        DatabaseUpdateApi.BILLS_TAG,
        DatabaseUpdateApi.COMMITTEES_TAG,
        DatabaseUpdateApi.RECESS_TAG,
        DatabaseUpdateApi.INTERESTS_TAG
    )

    /**
     * Helper: mocks all 7 streams as up to date at the given version.
     */
    private suspend fun mockAllStreamsUpToDate(version: Int) {
        for (tag in allTags) {
            mockManifest(tag, manifestJson(version = version, previousVersion = version - 1))
        }
        preferences.setMpsVersion(version)
        preferences.setCommonsVotesVersion(version)
        preferences.setLordsVotesVersion(version)
        preferences.setBillsVersion(version)
        preferences.setCommitteesVersion(version)
        preferences.setRecessVersion(version)
        preferences.setInterestsVersion(version)
    }

    @Test
    fun `first launch returns NeedsFullDownload when seedVersion is null`() = runTest {
        // seedVersion is null because DataStore is empty (first launch)
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsFullDownload)
        assertTrue((state as DatabaseUpdateState.NeedsFullDownload).seedManifest == null)
    }

    @Test
    fun `all streams up to date`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        mockAllStreamsUpToDate(5)
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.UpToDate)
    }

    @Test
    fun `needs patches for one stream`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // mps is 1 behind, others up to date
        mockManifest(DatabaseUpdateApi.MPS_TAG, manifestJson(version = 6, previousVersion = 5))
        preferences.setMpsVersion(5)
        for (tag in listOf(
            DatabaseUpdateApi.COMMONS_VOTES_TAG,
            DatabaseUpdateApi.LORDS_VOTES_TAG,
            DatabaseUpdateApi.BILLS_TAG,
            DatabaseUpdateApi.COMMITTEES_TAG,
            DatabaseUpdateApi.RECESS_TAG,
            DatabaseUpdateApi.INTERESTS_TAG
        )) {
            mockManifest(tag, manifestJson(version = 5, previousVersion = 4))
        }
        preferences.setCommonsVotesVersion(5)
        preferences.setLordsVotesVersion(5)
        preferences.setBillsVersion(5)
        preferences.setCommitteesVersion(5)
        preferences.setRecessVersion(5)
        preferences.setInterestsVersion(5)

        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsPatches)
        val patches = (state as DatabaseUpdateState.NeedsPatches).patches
        assertEquals(1, patches.size)
        assertEquals("mps", patches[0].streamName)
    }

    @Test
    fun `needs patches for multiple streams`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // mps and commons-votes are 1 behind, others up to date
        mockManifest(DatabaseUpdateApi.MPS_TAG, manifestJson(version = 6, previousVersion = 5))
        mockManifest(DatabaseUpdateApi.COMMONS_VOTES_TAG, manifestJson(version = 6, previousVersion = 5))
        preferences.setMpsVersion(5)
        preferences.setCommonsVotesVersion(5)
        for (tag in listOf(
            DatabaseUpdateApi.LORDS_VOTES_TAG,
            DatabaseUpdateApi.BILLS_TAG,
            DatabaseUpdateApi.COMMITTEES_TAG,
            DatabaseUpdateApi.RECESS_TAG,
            DatabaseUpdateApi.INTERESTS_TAG
        )) {
            mockManifest(tag, manifestJson(version = 5, previousVersion = 4))
        }
        preferences.setLordsVotesVersion(5)
        preferences.setBillsVersion(5)
        preferences.setCommitteesVersion(5)
        preferences.setRecessVersion(5)
        preferences.setInterestsVersion(5)

        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsPatches)
        val patches = (state as DatabaseUpdateState.NeedsPatches).patches
        assertEquals(2, patches.size)
        val streamNames = patches.map { it.streamName }.sorted()
        assertEquals(listOf("commons-votes", "mps"), streamNames)
    }

    @Test
    fun `partial failure is graceful`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // mps fetch fails (404), others succeed and are up to date
        mockManifestFailure(DatabaseUpdateApi.MPS_TAG)
        for (tag in listOf(
            DatabaseUpdateApi.COMMONS_VOTES_TAG,
            DatabaseUpdateApi.LORDS_VOTES_TAG,
            DatabaseUpdateApi.BILLS_TAG,
            DatabaseUpdateApi.COMMITTEES_TAG,
            DatabaseUpdateApi.RECESS_TAG,
            DatabaseUpdateApi.INTERESTS_TAG
        )) {
            mockManifest(tag, manifestJson(version = 5, previousVersion = 4))
        }
        preferences.setMpsVersion(5)
        preferences.setCommonsVotesVersion(5)
        preferences.setLordsVotesVersion(5)
        preferences.setBillsVersion(5)
        preferences.setCommitteesVersion(5)
        preferences.setRecessVersion(5)
        preferences.setInterestsVersion(5)

        val state = manager.checkForUpdates()
        // The failed stream is skipped; others are up to date → UpToDate
        assertTrue(state is DatabaseUpdateState.UpToDate)
    }

    @Test
    fun `full download when stream multiple behind`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // mpsVersion is 3, manifest version is 5, previousVersion is 4 → multiple behind
        mockManifest(DatabaseUpdateApi.MPS_TAG, manifestJson(version = 5, previousVersion = 4))
        preferences.setMpsVersion(3)
        for (tag in listOf(
            DatabaseUpdateApi.COMMONS_VOTES_TAG,
            DatabaseUpdateApi.LORDS_VOTES_TAG,
            DatabaseUpdateApi.BILLS_TAG,
            DatabaseUpdateApi.COMMITTEES_TAG,
            DatabaseUpdateApi.RECESS_TAG,
            DatabaseUpdateApi.INTERESTS_TAG
        )) {
            mockManifest(tag, manifestJson(version = 5, previousVersion = 4))
        }
        preferences.setCommonsVotesVersion(5)
        preferences.setLordsVotesVersion(5)
        preferences.setBillsVersion(5)
        preferences.setCommitteesVersion(5)
        preferences.setRecessVersion(5)
        preferences.setInterestsVersion(5)

        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsFullDownload)
    }

    @Test
    fun `needs patches for interests stream`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // interests is 1 behind, others up to date
        mockManifest(DatabaseUpdateApi.INTERESTS_TAG, manifestJson(version = 6, previousVersion = 5))
        preferences.setInterestsVersion(5)
        for (tag in listOf(
            DatabaseUpdateApi.MPS_TAG,
            DatabaseUpdateApi.COMMONS_VOTES_TAG,
            DatabaseUpdateApi.LORDS_VOTES_TAG,
            DatabaseUpdateApi.BILLS_TAG,
            DatabaseUpdateApi.COMMITTEES_TAG,
            DatabaseUpdateApi.RECESS_TAG
        )) {
            mockManifest(tag, manifestJson(version = 5, previousVersion = 4))
        }
        preferences.setMpsVersion(5)
        preferences.setCommonsVotesVersion(5)
        preferences.setLordsVotesVersion(5)
        preferences.setBillsVersion(5)
        preferences.setCommitteesVersion(5)
        preferences.setRecessVersion(5)

        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsPatches)
        val patches = (state as DatabaseUpdateState.NeedsPatches).patches
        assertEquals(1, patches.size)
        assertEquals("interests", patches[0].streamName)
    }

    @Test
    fun `failed on all streams network error`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        // All manifests return 404 (no entries in responseMap)
        responseMap.clear()

        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.Failed)
    }

    @Test
    fun `isFirstLaunch true when seedVersion is null`() = runTest {
        // seedVersion is null because DataStore is empty
        val dbPath = context.getDatabasePath(BundledDatabase.DATABASE_NAME)
        dbPath.delete()
        assertTrue(manager.isFirstLaunch())
    }

    @Test
    fun `isFirstLaunch false when seedVersion is set`() = runTest {
        preferences.setSeedVersion(DatabaseUpdateManager.CURRENT_SEED_VERSION)
        val dbPath = context.getDatabasePath(BundledDatabase.DATABASE_NAME)
        dbPath.parentFile?.mkdirs()
        dbPath.createNewFile()
        assertTrue(!manager.isFirstLaunch())
        dbPath.delete()
    }
}
