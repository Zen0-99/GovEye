package com.goveye.app.work

import android.content.Context
import androidx.work.WorkerParameters
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.preference.DatabasePreferences
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.notifications.NotificationHelper
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
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
class VotePollingWorkerTest {

    private val divisionDao = mockk<DivisionDao>(relaxed = true)
    private val mpDao = mockk<MpDao>(relaxed = true)
    private val notificationHelper = mockk<NotificationHelper>(relaxed = true)
    private val notificationPrefRepository = mockk<NotificationPreferenceRepository>(relaxed = true)
    private val databasePreferences = mockk<DatabasePreferences>(relaxed = true)
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
    }

    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        workerParams = mockk(relaxed = true)
        // Default: lastNotifiedDivisionId is null (first run)
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(null)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    private fun createWorker() = VotePollingWorker(
        context,
        workerParams,
        divisionDao,
        mpDao,
        notificationHelper,
        notificationPrefRepository,
        databasePreferences,
        json
    )

    private fun makeDivision(id: Int, house: Int = 1) = DivisionEntity(
        id = id,
        title = "Division $id",
        date = "2026-01-01",
        publicationUpdated = null,
        number = null,
        isDeferred = false,
        ayeCount = 100,
        noCount = 50,
        house = house,
        lastUpdated = System.currentTimeMillis()
    )

    private fun makeVote(divisionId: Int, memberId: Int, vote: String = "AYE") = DivisionVoteEntity(
        divisionId = divisionId,
        memberId = memberId,
        vote = vote,
        memberName = "MP $memberId",
        partyName = "Test Party",
        partyColour = "#0000FF",
        constituencyName = "Test Constituency",
        isTeller = false,
        proxyName = null
    )

    private fun makeMp(id: Int) = MpEntity(
        id = id,
        nameListAs = "Test MP",
        nameDisplayAs = "MP $id",
        nameFullTitle = null,
        nameAddressAs = null,
        gender = null,
        partyId = 1,
        partyName = "Test Party",
        partyAbbreviation = "TP",
        partyBackgroundColour = "#0000FF",
        partyForegroundColour = "#FFFFFF",
        constituencyId = 1,
        constituencyName = "Test Constituency",
        house = 1,
        membershipStartDate = "2020-01-01",
        membershipEndDate = null,
        isActive = true,
        thumbnailUrl = null,
        lastUpdated = System.currentTimeMillis()
    )

    @Test
    fun `no divisions in DB returns success without notifications`() = runTest {
        coEvery { divisionDao.getMaxDivisionId() } returns null

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showVoteNotification(any()) }
        coVerify(exactly = 0) { notificationHelper.showSummaryNotification(any()) }
    }

    @Test
    fun `no new divisions returns success without notifications`() = runTest {
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(100)
        coEvery { divisionDao.getMaxDivisionId() } returns 100

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showVoteNotification(any()) }
    }

    @Test
    fun `new divisions but no notification-enabled MPs returns success without notifications`() = runTest {
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(0)
        coEvery { divisionDao.getMaxDivisionId() } returns 10
        coEvery { divisionDao.getDivisionsAfterId(0) } returns listOf(makeDivision(10))
        coEvery { notificationPrefRepository.getMemberIdsWithVotesEnabled() } returns emptyList()

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showVoteNotification(any()) }
        coVerify { databasePreferences.setLastNotifiedDivisionId(10) }
    }

    @Test
    fun `new divisions with enabled MPs who voted dispatches notifications`() = runTest {
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(0)
        coEvery { divisionDao.getMaxDivisionId() } returns 10
        coEvery { divisionDao.getDivisionsAfterId(0) } returns listOf(makeDivision(10))
        coEvery { divisionDao.getVotesForDivisions(listOf(10)) } returns listOf(makeVote(10, 42))
        coEvery { notificationPrefRepository.getMemberIdsWithVotesEnabled() } returns listOf(42)
        coEvery { mpDao.getMp(42) } returns makeMp(42)
        coEvery { divisionDao.getVotesForDivision(10) } returns listOf(makeVote(10, 42))

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 1) { notificationHelper.showVoteNotification(any()) }
        coVerify { databasePreferences.setLastNotifiedDivisionId(10) }
    }

    @Test
    fun `more than 5 new votes dispatches summary notification`() = runTest {
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(0)
        val divisions = (1..6).map { makeDivision(it) }
        val votes = (1..6).map { makeVote(it, it) }
        coEvery { divisionDao.getMaxDivisionId() } returns 6
        coEvery { divisionDao.getDivisionsAfterId(0) } returns divisions
        coEvery { divisionDao.getVotesForDivisions(any()) } returns votes
        coEvery { notificationPrefRepository.getMemberIdsWithVotesEnabled() } returns (1..6).toList()
        coEvery { mpDao.getMp(any()) } returns makeMp(1)
        coEvery { divisionDao.getVotesForDivision(any()) } returns listOf(makeVote(1, 1))

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showVoteNotification(any()) }
        coVerify(exactly = 1) { notificationHelper.showSummaryNotification(6) }
    }

    @Test
    fun `first run with no enabled MPs updates lastNotifiedDivisionId without notifications`() = runTest {
        every { databasePreferences.lastNotifiedDivisionId } returns flowOf(null)
        coEvery { divisionDao.getMaxDivisionId() } returns 50
        coEvery { divisionDao.getDivisionsAfterId(0) } returns listOf(makeDivision(50))
        coEvery { notificationPrefRepository.getMemberIdsWithVotesEnabled() } returns emptyList()

        val result = createWorker().doWork()

        assertTrue(result is androidx.work.ListenableWorker.Result.Success)
        coVerify(exactly = 0) { notificationHelper.showVoteNotification(any()) }
        coVerify { databasePreferences.setLastNotifiedDivisionId(50) }
    }
}
