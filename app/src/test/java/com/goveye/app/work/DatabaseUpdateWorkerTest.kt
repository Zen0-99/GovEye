package com.goveye.app.work

import android.content.Context
import androidx.work.WorkerParameters
import com.goveye.app.data.update.DatabaseManifest
import com.goveye.app.data.update.DatabaseUpdateManager
import com.goveye.app.data.update.DatabaseUpdateState
import com.goveye.app.data.update.GithubReleaseDto
import com.goveye.app.data.update.PatchInfo
import com.goveye.app.data.update.ReleaseAssetDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseUpdateWorkerTest {

    private val databaseUpdateManager = mockk<DatabaseUpdateManager>(relaxed = true)

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        workerParams = mockk(relaxed = true)
        // Mock WorkScheduler so enqueue calls don't hit real WorkManager
        mockkObject(WorkScheduler)
        every { WorkScheduler.enqueueVotePollingOneShot(any()) } returns Unit
        every { WorkScheduler.enqueueBillPollingOneShot(any()) } returns Unit
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createWorker() = DatabaseUpdateWorker(
        context,
        workerParams,
        databaseUpdateManager,
    )

    private fun makePatch(streamName: String): PatchInfo {
        val manifest = DatabaseManifest(
            version = 2,
            previousVersion = 1,
            schemaVersion = 1,
            generatedAt = "2026-01-01T00:00:00Z",
            dbHash = "abc123",
            dbSize = 160000000,
            patchHash = "def456",
            patchSize = 50000,
        )
        val release = GithubReleaseDto(
            assets = listOf(
                ReleaseAssetDto(
                    name = "patch.json",
                    browserDownloadUrl = "https://example.com/patch.json",
                    size = 50000,
                ),
            ),
        )
        return PatchInfo(streamName, manifest, release)
    }

    @Test
    fun `NeedsPatches with votes stream enqueues VotePollingWorker`() = runTest {
        val patches = listOf(makePatch("votes"))
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsPatches(patches)
        coEvery { databaseUpdateManager.applyPatches(patches) } returns DatabaseUpdateState.UpToDate

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify { databaseUpdateManager.applyPatches(patches) }
        verify { WorkScheduler.enqueueVotePollingOneShot(any()) }
        verify(exactly = 0) { WorkScheduler.enqueueBillPollingOneShot(any()) }
    }

    @Test
    fun `NeedsPatches with bills stream enqueues BillPollingWorker`() = runTest {
        val patches = listOf(makePatch("bills"))
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsPatches(patches)
        coEvery { databaseUpdateManager.applyPatches(patches) } returns DatabaseUpdateState.UpToDate

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify { databaseUpdateManager.applyPatches(patches) }
        verify { WorkScheduler.enqueueBillPollingOneShot(any()) }
        verify(exactly = 0) { WorkScheduler.enqueueVotePollingOneShot(any()) }
    }

    @Test
    fun `NeedsPatches with both votes and bills enqueues both polling workers`() = runTest {
        val patches = listOf(makePatch("votes"), makePatch("bills"))
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsPatches(patches)
        coEvery { databaseUpdateManager.applyPatches(patches) } returns DatabaseUpdateState.UpToDate

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify { databaseUpdateManager.applyPatches(patches) }
        verify { WorkScheduler.enqueueVotePollingOneShot(any()) }
        verify { WorkScheduler.enqueueBillPollingOneShot(any()) }
    }

    @Test
    fun `NeedsPatches with neither votes nor bills does not enqueue polling workers`() = runTest {
        val patches = listOf(makePatch("mps"), makePatch("committees"), makePatch("recess"))
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsPatches(patches)
        coEvery { databaseUpdateManager.applyPatches(patches) } returns DatabaseUpdateState.UpToDate

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify { databaseUpdateManager.applyPatches(patches) }
        verify(exactly = 0) { WorkScheduler.enqueueVotePollingOneShot(any()) }
        verify(exactly = 0) { WorkScheduler.enqueueBillPollingOneShot(any()) }
    }

    @Test
    fun `UpToDate returns success without applying patches`() = runTest {
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.UpToDate

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { databaseUpdateManager.applyPatches(any()) }
        verify(exactly = 0) { WorkScheduler.enqueueVotePollingOneShot(any()) }
        verify(exactly = 0) { WorkScheduler.enqueueBillPollingOneShot(any()) }
    }

    @Test
    fun `Failed returns retry`() = runTest {
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.Failed("Network error")

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Retry)
    }

    @Test
    fun `NeedsFullDownload returns success without applying patches`() = runTest {
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsFullDownload(null)

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { databaseUpdateManager.applyPatches(any()) }
    }

    @Test
    fun `patch application failure returns retry`() = runTest {
        val patches = listOf(makePatch("votes"))
        coEvery { databaseUpdateManager.checkForUpdates() } returns DatabaseUpdateState.NeedsPatches(patches)
        coEvery { databaseUpdateManager.applyPatches(patches) } returns DatabaseUpdateState.Failed("Patch error")

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Retry)
        verify(exactly = 0) { WorkScheduler.enqueueVotePollingOneShot(any()) }
    }
}
