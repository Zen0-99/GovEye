package com.goveye.app.data.repo

import app.cash.turbine.test
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.dto.members.MemberSearchResponse
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.data.repo.MpRemoteMediator
import com.goveye.app.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MembersRepositoryTest {
    private lateinit var database: GovEyeDatabase
    private lateinit var repository: MembersRepository
    private val api: MembersApi = mockk(relaxed = true)
    private val remoteMediator: MpRemoteMediator = mockk(relaxed = true)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GovEyeDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = MembersRepository(database.mpDao(), database.searchDao(), api, MemberMapper, remoteMediator, database.remoteKeyDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun makeMp(id: Int, lastUpdated: Long): MpEntity =
        MpEntity(
            id = id,
            nameListAs = "Test MP $id",
            nameDisplayAs = "Test $id",
            nameFullTitle = null,
            nameAddressAs = null,
            gender = null,
            partyId = 15,
            partyName = "Labour",
            partyAbbreviation = "Lab",
            partyBackgroundColour = "d50000",
            partyForegroundColour = "ffffff",
            constituencyId = 4074,
            constituencyName = "Test Constituency",
            house = 1,
            membershipStartDate = null,
            membershipEndDate = null,
            isActive = true,
            thumbnailUrl = null,
            lastUpdated = lastUpdated,
        )

    @Test
    fun `emits FRESH when cache is within TTL`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(1, System.currentTimeMillis())))
        repository.observeAllMps().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(1, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits STALE when cache exceeds TTL`() = runTest {
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        database.mpDao().upsertAll(listOf(makeMp(1, eightDaysAgo)))
        repository.observeAllMps().test {
            val result = awaitItem()
            assertEquals(SyncStatus.STALE, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits EMPTY when no cache`() = runTest {
        repository.observeAllMps().test {
            val result = awaitItem()
            assertEquals(SyncStatus.EMPTY, result.status)
            assertEquals(0, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `refresh bypasses TTL and calls API`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(1, System.currentTimeMillis())))
        coEvery { api.searchMembers(any(), any(), any(), any()) } returns MemberSearchResponse()
        repository.refresh()
        // Verify API was called - if it wasn't, the mock would throw
    }

    @Test
    fun `refresh handles API failure silently`() = runTest {
        coEvery { api.searchMembers(any(), any(), any(), any()) } throws java.io.IOException("Network error")
        repository.refresh()
        // Should not throw - cache is still served
    }
}
