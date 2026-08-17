package com.goveye.app.data.repo

import app.cash.turbine.test
import com.goveye.app.data.api.LordsVotesApi
import com.goveye.app.data.api.VotesApi
import com.goveye.app.data.local.GovEyeDatabase
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.mapper.DivisionMapper
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
import androidx.room.Room

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VotesRepositoryTest {
    private lateinit var database: GovEyeDatabase
    private lateinit var repository: VotesRepository
    private val api: VotesApi = mockk(relaxed = true)
    private val lordsApi: LordsVotesApi = mockk(relaxed = true)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            RuntimeEnvironment.getApplication(),
            GovEyeDatabase::class.java,
        ).allowMainThreadQueries().build()
        repository = VotesRepository(database.divisionDao(), api, lordsApi, DivisionMapper)
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
                    id = 1, title = "Test", date = "2026-01-01", isDeferred = false,
                    ayeCount = 100, noCount = 50, house = 1, lastUpdated = System.currentTimeMillis(),
                ),
            ),
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
    fun `emits STALE when divisions exceed TTL`() = runTest {
        val twoHoursAgo = System.currentTimeMillis() - (2 * 60 * 60 * 1000L)
        database.divisionDao().upsertAll(
            listOf(
                DivisionEntity(
                    id = 1, title = "Test", date = "2026-01-01", isDeferred = false,
                    ayeCount = 100, noCount = 50, house = 1, lastUpdated = twoHoursAgo,
                ),
            ),
        )
        repository.observeDivisions().test {
            val result = awaitItem()
            assertEquals(SyncStatus.STALE, result.status)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
