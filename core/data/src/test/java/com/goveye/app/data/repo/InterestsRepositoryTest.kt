package com.goveye.app.data.repo

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.entity.InterestEntity
import com.goveye.app.domain.model.SyncStatus
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class InterestsRepositoryTest {
    private lateinit var database: BundledDatabase
    private lateinit var repository: InterestsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = InterestsRepository(database.interestDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `emits FRESH with interests when data exists`() = runTest {
        database.interestDao().upsertAll(
            listOf(
                testInterest(id = 1, memberId = 172),
                testInterest(id = 2, memberId = 172)
            )
        )
        repository.observeInterestsForMember(172).test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(2, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits EMPTY when no interests cached`() = runTest {
        repository.observeInterestsForMember(172).test {
            val result = awaitItem()
            assertEquals(SyncStatus.EMPTY, result.status)
            assertEquals(0, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `date range filter returns only matching interests`() = runTest {
        database.interestDao().upsertAll(
            listOf(
                testInterest(id = 1, memberId = 172, publishedDate = "2023-06-01"),
                testInterest(id = 2, memberId = 172, publishedDate = "2024-03-15"),
                testInterest(id = 3, memberId = 172, publishedDate = "2024-09-20")
            )
        )
        repository.observeInterestsForMemberInRange(172, "2024-01-01", "2024-12-31").test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(2, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `date range filter with null bounds returns all`() = runTest {
        database.interestDao().upsertAll(
            listOf(
                testInterest(id = 1, memberId = 172, publishedDate = "2023-06-01"),
                testInterest(id = 2, memberId = 172, publishedDate = "2024-03-15"),
                testInterest(id = 3, memberId = 172, publishedDate = "2024-09-20")
            )
        )
        repository.observeInterestsForMemberInRange(172, null, null).test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(3, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `date range filter with future from-date returns empty`() = runTest {
        database.interestDao().upsertAll(
            listOf(
                testInterest(id = 1, memberId = 172, publishedDate = "2024-03-15")
            )
        )
        repository.observeInterestsForMemberInRange(172, "2099-01-01", null).test {
            val result = awaitItem()
            assertEquals(SyncStatus.EMPTY, result.status)
            assertEquals(0, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `maps parsedAmountPence, currencyCode, and bucket to domain`() = runTest {
        database.interestDao().upsertAll(
            listOf(
                testInterest(
                    id = 1,
                    memberId = 172,
                    parsedAmountPence = 500000,
                    currencyCode = "GBP",
                    bucket = "Employment/Earnings"
                )
            )
        )
        repository.observeInterestsForMember(172).test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            val interest = result.data[0]
            assertEquals(500000L, interest.parsedAmountPence)
            assertEquals("GBP", interest.currencyCode)
            assertEquals("Employment/Earnings", interest.bucket)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `FRESH regardless of lastUpdated age`() = runTest {
        val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
        database.interestDao().upsertAll(
            listOf(
                testInterest(id = 1, memberId = 172, lastUpdated = thirtyDaysAgo)
            )
        )
        repository.observeInterestsForMember(172).test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

private fun testInterest(
    id: Int = 1,
    memberId: Int = 172,
    publishedDate: String? = "2024-01-15",
    parsedAmountPence: Long? = null,
    currencyCode: String? = null,
    bucket: String? = null,
    lastUpdated: Long = System.currentTimeMillis()
) = InterestEntity(
    id = id,
    memberId = memberId,
    summary = "Test interest $id",
    categoryId = 1,
    categoryNumber = "1.1",
    categoryName = "Employment and earnings",
    registrationDate = "2024-01-10",
    publishedDate = publishedDate,
    rectified = false,
    fieldsJson = "[]",
    lastUpdated = lastUpdated,
    parsedAmountPence = parsedAmountPence,
    currencyCode = currencyCode,
    bucket = bucket
)
