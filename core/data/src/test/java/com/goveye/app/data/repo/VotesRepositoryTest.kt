package com.goveye.app.data.repo

import androidx.room.Room
import app.cash.turbine.test
import com.goveye.app.data.local.BundledDatabase
import com.goveye.app.data.local.entity.DivisionEntity
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
class VotesRepositoryTest {
    private lateinit var database: BundledDatabase
    private lateinit var repository: VotesRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            BundledDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = VotesRepository(database.divisionDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `emits cached divisions first`() = runTest {
        database.divisionDao().upsertAll(
            listOf(
                DivisionEntity(
                    id = 1,
                    title = "Test",
                    date = "2026-01-01",
                    isDeferred = false,
                    ayeCount = 100,
                    noCount = 50,
                    house = 1,
                    lastUpdated = System.currentTimeMillis()
                )
            )
        )
        repository.observeDivisions().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            assertEquals(1, result.data.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits EMPTY when no divisions cached`() = runTest {
        repository.observeDivisions().test {
            val result = awaitItem()
            assertEquals(SyncStatus.EMPTY, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `emits FRESH regardless of lastUpdated age`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        database.divisionDao().upsertAll(
            listOf(
                DivisionEntity(
                    id = 1,
                    title = "Test",
                    date = "2026-01-01",
                    isDeferred = false,
                    ayeCount = 100,
                    noCount = 50,
                    house = 1,
                    lastUpdated = twoHoursAgo
                )
            )
        )
        repository.observeDivisions().test {
            val result = awaitItem()
            assertEquals(SyncStatus.FRESH, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
