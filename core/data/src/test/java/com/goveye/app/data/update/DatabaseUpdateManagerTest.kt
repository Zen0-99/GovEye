package com.goveye.app.data.update

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.dao.DatabaseUpdateDao
import com.goveye.app.data.preference.DatabasePreferences
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseUpdateManagerTest {
    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var database: GovEyeDatabase
    private lateinit var updateDao: DatabaseUpdateDao
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: DatabasePreferences
    private val updateApi: DatabaseUpdateApi = mockk(relaxed = true)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }
    private lateinit var okHttpClient: OkHttpClient
    private lateinit var manager: DatabaseUpdateManager

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = RuntimeEnvironment.getApplication()
        database = Room.inMemoryDatabaseBuilder(context, GovEyeDatabase::class.java)
            .allowMainThreadQueries().build()
        updateDao = database.databaseUpdateDao()
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("test_db_prefs") },
        )
        preferences = DatabasePreferences(dataStore)
        okHttpClient = OkHttpClient.Builder().build()
        manager = DatabaseUpdateManager(
            updateApi = updateApi,
            preferences = preferences,
            json = json,
            context = context,
            database = database,
            updateDao = updateDao,
            dbDownloadClient = okHttpClient,
        )
    }

    @After
    fun tearDown() {
        database.close()
        server.shutdown()
    }

    /**
     * Helper: creates a GithubReleaseDto with a manifest.json asset pointing to
     * the MockWebServer, and enqueues the manifest JSON body as the response.
     */
    private fun mockReleaseWithManifest(manifestJson: String) {
        val manifestUrl = server.url("/manifest.json").toString()
        val release = GithubReleaseDto(
            assets = listOf(
                ReleaseAssetDto(
                    name = "manifest.json",
                    browserDownloadUrl = manifestUrl,
                    size = manifestJson.length.toLong(),
                ),
            ),
        )
        coEvery { updateApi.getLatestRelease() } returns release
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(manifestJson),
        )
    }

    private fun manifestJson(
        version: Int,
        previousVersion: Int? = null,
    ): String = """
        {
            "version": $version,
            "previousVersion": ${previousVersion ?: "null"},
            "schemaVersion": 8,
            "generatedAt": "2026-08-19T12:00:00Z",
            "dbHash": "abc123",
            "dbSize": 160000000,
            "patchHash": "def456",
            "patchSize": 50000
        }
    """.trimIndent()

    @Test
    fun `first launch returns NeedsFullDownload when dbVersion is null`() = runTest {
        // dbVersion is null because DataStore is empty (first launch)
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsFullDownload)
        assertTrue((state as DatabaseUpdateState.NeedsFullDownload).manifest == null)
    }

    @Test
    fun `up to date when local version matches manifest version`() = runTest {
        preferences.setDbVersion(5)
        mockReleaseWithManifest(manifestJson(version = 5, previousVersion = 4))
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.UpToDate)
    }

    @Test
    fun `needs patch when local version is exactly 1 behind`() = runTest {
        preferences.setDbVersion(4)
        mockReleaseWithManifest(manifestJson(version = 5, previousVersion = 4))
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsPatch)
        assertTrue((state as DatabaseUpdateState.NeedsPatch).manifest.version == 5)
    }

    @Test
    fun `needs full download when multiple versions behind`() = runTest {
        preferences.setDbVersion(3)
        mockReleaseWithManifest(manifestJson(version = 5, previousVersion = 4))
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.NeedsFullDownload)
        assertTrue((state as DatabaseUpdateState.NeedsFullDownload).manifest != null)
    }

    @Test
    fun `failed on network error from API`() = runTest {
        preferences.setDbVersion(5)
        coEvery { updateApi.getLatestRelease() } throws IOException("Network error")
        val state = manager.checkForUpdates()
        assertTrue(state is DatabaseUpdateState.Failed)
    }

    @Test
    fun `isFirstLaunch true when db file does not exist`() {
        // Use a fresh context — no DB file has been created
        val dbPath = context.getDatabasePath(GovEyeDatabase.DATABASE_NAME)
        dbPath.delete()
        assertTrue(manager.isFirstLaunch())
    }

    @Test
    fun `isFirstLaunch false when db file exists`() {
        val dbPath = context.getDatabasePath(GovEyeDatabase.DATABASE_NAME)
        dbPath.parentFile?.mkdirs()
        dbPath.createNewFile()
        assertTrue(!manager.isFirstLaunch())
        dbPath.delete()
    }
}
