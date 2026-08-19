package com.goveye.app.data.local.dao

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.entity.MpEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SearchDaoFtsTest {
    private lateinit var database: BundledDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // Helper: create MP entities with varied names, parties, constituencies
    private fun makeMp(
        id: Int,
        nameListAs: String = "Test, MP $id",
        nameDisplayAs: String = "MP Test $id",
        partyName: String = "Labour",
        constituencyName: String = "Test Constituency",
        house: Int = 1,
        isActive: Boolean = true,
    ): MpEntity = MpEntity(
        id = id,
        nameListAs = nameListAs,
        nameDisplayAs = nameDisplayAs,
        nameFullTitle = null,
        nameAddressAs = null,
        gender = null,
        partyId = 15,
        partyName = partyName,
        partyAbbreviation = "Lab",
        partyBackgroundColour = "d50000",
        partyForegroundColour = "ffffff",
        constituencyId = 1000 + id,
        constituencyName = constituencyName,
        house = house,
        membershipStartDate = null,
        membershipEndDate = null,
        isActive = isActive,
        thumbnailUrl = null,
        lastUpdated = System.currentTimeMillis(),
    )

    @Test
    fun `FTS search finds MP by name token`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(172, nameListAs = "Abbott, Ms Diane", nameDisplayAs = "Diane Abbott")))
        database.searchDao().searchMpsFts("Abbott*").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            assertEquals(172, results.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS search finds MP by party name`() = runTest {
        database.mpDao().upsertAll(listOf(
            makeMp(1, partyName = "Green Party", constituencyName = "Bristol Central"),
            makeMp(2, partyName = "Labour", constituencyName = "Hackney North"),
        ))
        database.searchDao().searchMpsFts("Green*").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            assertTrue(results.all { it.partyName == "Green Party" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS search finds MP by constituency`() = runTest {
        database.mpDao().upsertAll(listOf(
            makeMp(1, constituencyName = "Hackney North"),
            makeMp(2, constituencyName = "Islington South"),
        ))
        database.searchDao().searchMpsFts("Hackney*").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            assertTrue(results.all { it.constituencyName.contains("Hackney") })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS multi-token search matches both tokens`() = runTest {
        database.mpDao().upsertAll(listOf(
            makeMp(172, nameListAs = "Abbott, Ms Diane", nameDisplayAs = "Diane Abbott", constituencyName = "Hackney North"),
        ))
        database.searchDao().searchMpsFts("Diane* Abbott*").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            assertEquals(172, results.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS search with no matches returns empty`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(1, nameListAs = "Abbott")))
        database.searchDao().searchMpsFts("xyz123*").test {
            val results = awaitItem()
            assertTrue(results.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FTS results include full entity data`() = runTest {
        database.mpDao().upsertAll(listOf(makeMp(172, nameListAs = "Abbott, Ms Diane", partyName = "Labour", constituencyName = "Hackney North")))
        database.searchDao().searchMpsFts("Abbott*").test {
            val results = awaitItem()
            assertTrue(results.isNotEmpty())
            val mp = results.first()
            assertEquals("Labour", mp.partyName)
            assertEquals("Hackney North", mp.constituencyName)
            assertEquals(1, mp.house)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinct parties query returns unique party names`() = runTest {
        database.mpDao().upsertAll(listOf(
            makeMp(1, partyName = "Labour"),
            makeMp(2, partyName = "Labour"),
            makeMp(3, partyName = "Conservative"),
            makeMp(4, partyName = "Green Party"),
        ))
        database.mpDao().observeDistinctParties().test {
            val parties = awaitItem()
            assertEquals(3, parties.size)
            assertTrue(parties.contains("Labour"))
            assertTrue(parties.contains("Conservative"))
            assertTrue(parties.contains("Green Party"))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `distinct parties query filters inactive MPs`() = runTest {
        database.mpDao().upsertAll(listOf(
            makeMp(1, partyName = "Labour", isActive = true),
            makeMp(2, partyName = "Former Party", isActive = false),
        ))
        database.mpDao().observeDistinctParties().test {
            val parties = awaitItem()
            assertTrue(parties.contains("Labour"))
            assertTrue(!parties.contains("Former Party"))
            cancelAndIgnoreRemainingEvents()
        }
    }
}
