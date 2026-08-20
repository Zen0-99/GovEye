package com.goveye.app.data.repo

import app.cash.turbine.test
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.LocalDatabase
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import androidx.room.Room
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FeedRepositoryTest {
    private lateinit var bundledDb: BundledDatabase
    private lateinit var localDb: LocalDatabase
    private lateinit var repository: FeedRepository

    @Before
    fun setUp() {
        bundledDb = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java,
        ).allowMainThreadQueries().build()
        localDb = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            LocalDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = FeedRepository(
            bundledDb.divisionDao(),
            localDb.followDao(),
            bundledDb.recessDateDao(),
        )
    }

    @After
    fun tearDown() {
        bundledDb.close()
        localDb.close()
    }

    private fun division(id: Int, date: String = "2026-08-20") = DivisionEntity(
        id = id,
        title = "Division $id",
        date = date,
        isDeferred = false,
        ayeCount = 100,
        noCount = 50,
        house = 1,
        lastUpdated = System.currentTimeMillis(),
    )

    private fun vote(divisionId: Int, memberId: Int) = DivisionVoteEntity(
        divisionId = divisionId,
        memberId = memberId,
        vote = "AYE",
        memberName = "MP$memberId",
        partyName = "Labour",
        partyColour = "#DC241F",
        constituencyName = "Test",
        isTeller = false,
        proxyName = null,
    )

    @Test
    fun `feed shows all divisions when no MPs are followed`() = runTest {
        bundledDb.divisionDao().upsertAll(listOf(division(1), division(2), division(3)))
        repository.observeFeedData().test {
            val feedData = awaitItem()
            assertEquals(3, feedData.divisions.size)
            assertTrue(feedData.divisionsWithFollowedVotes.isEmpty())
            assertTrue(feedData.followedMemberIds.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `feed shows divisions with followed-MP highlight set`() = runTest {
        bundledDb.divisionDao().upsertAll(listOf(division(1), division(2), division(3)))
        localDb.followDao().insert(FollowEntity(memberId = 100, followedAt = System.currentTimeMillis()))
        bundledDb.divisionDao().upsertVotes(listOf(vote(1, 100)))
        repository.observeFeedData().test {
            val feedData = awaitItem()
            assertTrue(feedData.divisionsWithFollowedVotes.contains(1))
            assertTrue(feedData.followedMemberIds.contains(100))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filtered feed shows only divisions with followed votes`() = runTest {
        bundledDb.divisionDao().upsertAll(listOf(division(1), division(2), division(3)))
        localDb.followDao().insert(FollowEntity(memberId = 100, followedAt = System.currentTimeMillis()))
        bundledDb.divisionDao().upsertVotes(listOf(vote(1, 100)))
        repository.observeFeedDataFiltered().test {
            val feedData = awaitItem()
            assertEquals(1, feedData.divisions.size)
            assertEquals(1, feedData.divisions[0].id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `muted follows are excluded from highlight`() = runTest {
        bundledDb.divisionDao().upsertAll(listOf(division(1)))
        localDb.followDao().insert(
            FollowEntity(memberId = 100, followedAt = System.currentTimeMillis(), isMuted = true),
        )
        bundledDb.divisionDao().upsertVotes(listOf(vote(1, 100)))
        repository.observeFeedData().test {
            val feedData = awaitItem()
            assertTrue(feedData.followedMemberIds.isEmpty())
            assertTrue(feedData.divisionsWithFollowedVotes.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getCurrentRecess returns active recess period`() = runTest {
        val today = LocalDate.now()
        bundledDb.recessDateDao().insertAll(
            listOf(
                RecessDateEntity(
                    house = 1,
                    description = "Summer recess",
                    startDate = today.minusDays(7).toString(),
                    endDate = today.plusDays(7).toString(),
                ),
            ),
        )
        val recess = repository.getCurrentRecess(1)
        assertEquals("Summer recess", recess?.description)
    }

    @Test
    fun `getCurrentRecess returns null when not in recess`() = runTest {
        bundledDb.recessDateDao().insertAll(
            listOf(
                RecessDateEntity(
                    house = 1,
                    description = "Past recess",
                    startDate = "2026-01-01",
                    endDate = "2026-01-15",
                ),
            ),
        )
        val recess = repository.getCurrentRecess(1)
        assertNull(recess)
    }

    @Test
    fun `getCurrentRecess returns null when no recess data`() = runTest {
        val recess = repository.getCurrentRecess(1)
        assertNull(recess)
    }
}
