package com.goveye.app.data.repo

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.entity.BillEntity
import com.goveye.app.domain.model.SyncStatus
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
class BillsRepositoryTest {
    private lateinit var database: BundledDatabase
    private lateinit var repository: BillsRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = BillsRepository(database.billDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `emits cached bills first`() = runTest {
        database.billDao().upsertAll(
            listOf(
                BillEntity(
                    id = 1,
                    shortTitle = "Test Bill",
                    currentHouse = "Commons",
                    originatingHouse = "Commons",
                    lastUpdate = "2025-01-01",
                    isDefeated = false,
                    isAct = false,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        )
        repository.observeBills().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(1, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits EMPTY when no bills cached`() = runTest {
        repository.observeBills().test {
            val result = awaitItem()
            assertEquals(SyncStatus.EMPTY, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits FRESH regardless of lastUpdated age`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        database.billDao().upsertAll(
            listOf(
                BillEntity(
                    id = 1,
                    shortTitle = "Test Bill",
                    currentHouse = "Commons",
                    originatingHouse = "Commons",
                    lastUpdate = "2025-01-01",
                    isDefeated = false,
                    isAct = false,
                    lastUpdated = twoHoursAgo
                )
            )
        )
        repository.observeBills().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
