package com.goveye.app.data.repo

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.api.MembersApi
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.mapper.MemberMapper
import com.goveye.app.domain.model.SyncStatus
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MembersRepositoryTest {
    private lateinit var database: BundledDatabase
    private lateinit var repository: MembersRepository
    private val api: MembersApi = mockk(relaxed = true)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = MembersRepository(database.mpDao(), database.searchDao(), api, MemberMapper)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun makeMp(id: Int, lastUpdated: Long): MpEntity = MpEntity(
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
        lastUpdated = lastUpdated
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
    fun `emits FRESH regardless of lastUpdated age`() = runTest {
        val eightDaysAgo = System.currentTimeMillis() - (8 * 24 * 60 * 60 * 1000L)
        database.mpDao().upsertAll(listOf(makeMp(1, eightDaysAgo)))
        repository.observeAllMps().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
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
}
