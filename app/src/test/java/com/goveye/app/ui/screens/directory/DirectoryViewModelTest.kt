package com.goveye.app.ui.screens.directory

import app.cash.turbine.test
import com.goveye.app.data.local.dao.SearchDao
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.data.repo.MembersRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DirectoryViewModelTest {

    private val membersRepository = mockk<MembersRepository>(relaxed = true)
    private val searchDao = mockk<SearchDao>(relaxed = true)
    private val directoryPreferences = mockk<DirectoryPreferences>(relaxed = true)

    @Test
    fun `default view mode is LIST`() = runTest {
        every { directoryPreferences.viewMode } returns flowOf(DirectoryViewMode.LIST)

        val viewModel = DirectoryViewModel(membersRepository, searchDao, directoryPreferences)

        viewModel.viewMode.test {
            assertEquals(DirectoryViewMode.LIST, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `setViewMode persists to preferences`() = runTest {
        every { directoryPreferences.viewMode } returns flowOf(DirectoryViewMode.LIST)
        coEvery { directoryPreferences.setViewMode(any()) } returns Unit

        val viewModel = DirectoryViewModel(membersRepository, searchDao, directoryPreferences)
        viewModel.setViewMode(DirectoryViewMode.GRID)
    }

    @Test
    fun `search query updates state`() = runTest {
        every { directoryPreferences.viewMode } returns flowOf(DirectoryViewMode.LIST)
        every { searchDao.searchMps(any()) } returns flowOf(emptyList())

        val viewModel = DirectoryViewModel(membersRepository, searchDao, directoryPreferences)

        viewModel.searchQuery.test {
            assertEquals("", awaitItem())
            viewModel.updateSearchQuery("Abbott")
            assertEquals("Abbott", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `empty search query returns empty results`() = runTest {
        every { directoryPreferences.viewMode } returns flowOf(DirectoryViewMode.LIST)
        every { searchDao.searchMps(any()) } returns flowOf(emptyList())

        val viewModel = DirectoryViewModel(membersRepository, searchDao, directoryPreferences)

        viewModel.searchResults.test {
            assertEquals(emptyList<Any>(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
