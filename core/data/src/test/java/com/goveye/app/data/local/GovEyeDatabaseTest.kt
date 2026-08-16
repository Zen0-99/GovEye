package com.goveye.app.data.local

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.data.local.entity.BillStageEntity
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionVoteEntity
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class GovEyeDatabaseTest {
    private lateinit var database: GovEyeDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GovEyeDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun makeMp(id: Int): MpEntity =
        MpEntity(
            id = id,
            nameListAs = "Abbott, Ms Diane",
            nameDisplayAs = "Diane Abbott",
            nameFullTitle = null,
            nameAddressAs = null,
            gender = "F",
            partyId = 15,
            partyName = "Labour",
            partyAbbreviation = "Lab",
            partyBackgroundColour = "d50000",
            partyForegroundColour = "ffffff",
            constituencyId = 4074,
            constituencyName = "Hackney North",
            house = 1,
            membershipStartDate = "1987-06-11",
            membershipEndDate = null,
            isActive = true,
            thumbnailUrl = null,
            lastUpdated = System.currentTimeMillis(),
        )

    @Test
    fun `insert and retrieve MP`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(172)))
        val mp = database.mpDao().getMp(172)
        assertEquals(172, mp?.id)
        assertEquals("Abbott, Ms Diane", mp?.nameListAs)
        assertEquals("Labour", mp?.partyName)
    }

    @Test
    fun `insert and retrieve division with votes`() = runTest {
        database.divisionDao().upsertAll(
            listOf(
                DivisionEntity(
                    id = 2409, title = "Test", date = "2026-01-01", isDeferred = false,
                    ayeCount = 100, noCount = 50, house = 1, lastUpdated = System.currentTimeMillis(),
                ),
            ),
        )
        database.divisionDao().upsertVotes(
            listOf(
                DivisionVoteEntity(2409, 172, "AYE", "Diane Abbott", "Labour", "d50000", "Hackney", false),
                DivisionVoteEntity(2409, 39, "NO", "John Whittingdale", "Conservative", "0063ba", "Maldon", false),
            ),
        )

        database.divisionDao().observeVotesForDivision(2409).test {
            val votes = awaitItem()
            assertEquals(2, votes.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `insert and retrieve bill with stages`() = runTest {
        database.billDao().upsertAll(
            listOf(
                BillEntity(
                    id = 3973, shortTitle = "Test Bill", currentHouse = "Commons",
                    originatingHouse = "Commons", lastUpdate = "2025-01-01",
                    isDefeated = false, isAct = false, lastUpdated = System.currentTimeMillis(),
                ),
            ),
        )
        database.billDao().upsertStages(
            listOf(
                BillStageEntity(3973, 7, "2nd reading", "2R", "Commons", 2, null, listOf("2025-07-11"), System.currentTimeMillis()),
                BillStageEntity(3973, 6, "1st reading", "1R", "Commons", 1, null, listOf("2025-06-15"), System.currentTimeMillis()),
            ),
        )

        val bill = database.billDao().getBill(3973)
        assertEquals(3973, bill?.id)

        database.billDao().observeBillStages(3973).test {
            val stages = awaitItem()
            assertEquals(2, stages.size)
            assertEquals(1, stages[0].sortOrder)
            assertEquals(2, stages[1].sortOrder)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS search finds MP by name`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(172)))

        database.searchDao().searchMps("Abbott").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            assertEquals(172, results.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `follow insert and delete`() = runTest {
        database.followDao().insert(FollowEntity(172, System.currentTimeMillis()))
        assertTrue(database.followDao().isFollowing(172))

        database.followDao().delete(172)
        assertFalse(database.followDao().isFollowing(172))
    }

    @Test
    fun `type converter for List String`() = runTest {
        database.billDao().upsertAll(
            listOf(
                BillEntity(
                    id = 1, shortTitle = "Test", currentHouse = "Commons",
                    originatingHouse = "Commons", lastUpdate = "2025-01-01",
                    isDefeated = false, isAct = false, lastUpdated = System.currentTimeMillis(),
                ),
            ),
        )
        database.billDao().upsertStages(
            listOf(
                BillStageEntity(1, 1, "Test", "T", "Commons", 1, null, listOf("2025-07-11", "2025-07-12"), System.currentTimeMillis()),
            ),
        )

        database.billDao().observeBillStages(1).test {
            val stages = awaitItem()
            assertEquals(1, stages.size)
            assertEquals(listOf("2025-07-11", "2025-07-12"), stages[0].sittingDates)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
