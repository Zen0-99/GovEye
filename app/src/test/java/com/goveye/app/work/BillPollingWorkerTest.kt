package com.goveye.app.work

import android.content.Context
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.BillDao
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.repo.BillFollowRepository
import com.goveye.app.notifications.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
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
class BillPollingWorkerTest {

    private val billDao = mockk<BillDao>(relaxed = true)
    private val billFollowRepository = mockk<BillFollowRepository>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val databasePreferences = mockk<DatabasePreferences>(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false }

    private val stagesSerializer = MapSerializer(Int.serializer(), String.serializer())

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        workerParams = mockk(relaxed = true)
        // Default: no stored stages (first run)
        every { databasePreferences.lastNotifiedBillStages } returns flowOf(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createWorker() = BillPollingWorker(
        context,
        workerParams,
        billDao,
        billFollowRepository,
        notificationHelper,
        databasePreferences,
        json,
    )

    private fun makeBill(id: Int, stage: String) = BillEntity(
        id = id,
        shortTitle = "Bill $id",
        longTitle = null,
        summary = null,
        currentHouse = "Commons",
        originatingHouse = "Commons",
        lastUpdate = "2026-01-01",
        billWithdrawn = null,
        isDefeated = false,
        isAct = false,
        billTypeId = 1,
        currentStageDescription = stage,
        currentStageAbbreviation = null,
        lastUpdated = System.currentTimeMillis(),
    )

    @Test
    fun `no followed bills returns success without notifications`() = runTest {
        coEvery { billFollowRepository.getFollowedBillIds() } returns emptyList()

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showBillStageNotification(any()) }
    }

    @Test
    fun `followed bills with no stage changes dispatches no notifications`() = runTest {
        val billIds = listOf(1, 2)
        val bills = listOf(makeBill(1, "Report Stage"), makeBill(2, "Committee Stage"))
        val lastStages = mapOf(1 to "Report Stage", 2 to "Committee Stage")
        val lastStagesJson = json.encodeToString(stagesSerializer, lastStages)

        coEvery { billFollowRepository.getFollowedBillIds() } returns billIds
        every { databasePreferences.lastNotifiedBillStages } returns flowOf(lastStagesJson)
        coEvery { billDao.getBillsByIds(billIds) } returns bills

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showBillStageNotification(any()) }
        coVerify { databasePreferences.setLastNotifiedBillStages(any()) }
    }

    @Test
    fun `followed bills with stage changes dispatches notifications`() = runTest {
        val billIds = listOf(1, 2)
        // Bill 1 changed stage, Bill 2 didn't
        val bills = listOf(makeBill(1, "Report Stage"), makeBill(2, "Committee Stage"))
        val lastStages = mapOf(1 to "Second Reading", 2 to "Committee Stage")
        val lastStagesJson = json.encodeToString(stagesSerializer, lastStages)

        coEvery { billFollowRepository.getFollowedBillIds() } returns billIds
        every { databasePreferences.lastNotifiedBillStages } returns flowOf(lastStagesJson)
        coEvery { billDao.getBillsByIds(billIds) } returns bills

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 1) { notificationHelper.showBillStageNotification(any()) }
        coVerify { databasePreferences.setLastNotifiedBillStages(any()) }
    }

    @Test
    fun `more than 5 stage changes dispatches summary notification`() = runTest {
        val billIds = (1..6).toList()
        val bills = (1..6).map { makeBill(it, "Report Stage") }
        val lastStages = (1..6).associate { it to "Second Reading" }
        val lastStagesJson = json.encodeToString(stagesSerializer, lastStages)

        coEvery { billFollowRepository.getFollowedBillIds() } returns billIds
        every { databasePreferences.lastNotifiedBillStages } returns flowOf(lastStagesJson)
        coEvery { billDao.getBillsByIds(billIds) } returns bills

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showBillStageNotification(any()) }
        coVerify(exactly = 1) { notificationHelper.showBillSummaryNotification(6) }
    }

    @Test
    fun `first run stores stages without dispatching notifications`() = runTest {
        val billIds = listOf(1, 2)
        val bills = listOf(makeBill(1, "Report Stage"), makeBill(2, "Committee Stage"))

        coEvery { billFollowRepository.getFollowedBillIds() } returns billIds
        every { databasePreferences.lastNotifiedBillStages } returns flowOf(null)
        coEvery { billDao.getBillsByIds(billIds) } returns bills

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showBillStageNotification(any()) }
        // Stages are stored for next run comparison
        coVerify { databasePreferences.setLastNotifiedBillStages(any()) }
    }
}
