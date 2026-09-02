package com.goveye.app.ui.screens.directory

import app.cash.turbine.test
import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.CouncilDao
import com.goveye.app.data.preference.DirectoryFilterPreferences
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.preference.DirectoryViewMode
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.PostcodeRepository
import com.goveye.app.domain.model.Constituency
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.Party
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DirectoryViewModelFilterTest {

    private val membersRepository = mockk<MembersRepository>(relaxed = true)
    private val directoryPreferences = mockk<DirectoryPreferences>(relaxed = true)
    private val directoryFilterPreferences = mockk<DirectoryFilterPreferences>(relaxed = true)
    private val postcodeRepository = mockk<PostcodeRepository>(relaxed = true)
    private val councilDao = mockk<CouncilDao>(relaxed = true)
    private val committeeDao = mockk<CommitteeDao>(relaxed = true)
    private val governmentAnnouncementsRepository = mockk<GovernmentAnnouncementsRepository>(relaxed = true)

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun makeMp(id: Int, partyName: String = "Labour", house: Int = 1, isActive: Boolean = true): Mp = Mp(
        id = id,
        nameListAs = "Test MP $id",
        nameDisplayAs = "Test $id",
        nameFullTitle = null,
        gender = null,
        party = Party(15, partyName, "Lab", "d50000", "ffffff"),
        constituency = Constituency(1000 + id, "Test Constituency"),
        house = house,
        membershipStartDate = null,
        isActive = isActive,
        thumbnailUrl = null
    )

    private fun setupViewModel(
        searchResults: List<Mp> = emptyList(),
        filterState: DirectoryFilterState = DirectoryFilterState(),
        distinctParties: List<String> = emptyList()
    ): DirectoryViewModel {
        every { membersRepository.searchMpsFts(any()) } returns flowOf(searchResults)
        every { membersRepository.observeDistinctParties() } returns flowOf(distinctParties)
        every { membersRepository.observeAllMps() } returns flowOf(
            RepositoryResult(searchResults, SyncStatus.FRESH)
        )

        every { directoryPreferences.viewMode } returns flowOf(DirectoryViewMode.LIST)
        every { directoryFilterPreferences.includedParties } returns MutableStateFlow(filterState.includedParties)
        every { directoryFilterPreferences.excludedParties } returns MutableStateFlow(filterState.excludedParties)
        every { directoryFilterPreferences.houseFilter } returns MutableStateFlow(filterState.houseFilter)
        every { directoryFilterPreferences.currentOnly } returns MutableStateFlow(filterState.currentOnly)

        return DirectoryViewModel(
            membersRepository,
            directoryPreferences,
            directoryFilterPreferences,
            postcodeRepository,
            councilDao,
            committeeDao,
            governmentAnnouncementsRepository
        )
    }

    @Test
    fun `text search only with default filters returns all mps`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, partyName = "Labour", house = 1, isActive = true),
            makeMp(2, partyName = "Labour", house = 2, isActive = true),
            makeMp(3, partyName = "Labour", house = 1, isActive = false)
        )
        val vm = setupViewModel(searchResults = mps)
        vm.updateSearchQuery("Labour")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(3, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `house filter lords returns only lords`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, house = 1, isActive = true),
            makeMp(2, house = 2, isActive = true)
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(houseFilter = 2)
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals(2, results.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `house filter all returns both houses`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, house = 1, isActive = true),
            makeMp(2, house = 2, isActive = true)
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(houseFilter = 0)
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(2, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `status filter include former returns inactive mps too`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, isActive = true),
            makeMp(2, isActive = false)
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(currentOnly = false)
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(2, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `party filter included only returns matching parties`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, partyName = "Labour"),
            makeMp(2, partyName = "Conservative"),
            makeMp(3, partyName = "Labour")
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(includedParties = setOf("Labour"))
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(2, results.size)
            assertTrue(results.all { it.party?.name == "Labour" })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `party filter excluded removes matching parties`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, partyName = "Labour"),
            makeMp(2, partyName = "Conservative"),
            makeMp(3, partyName = "Labour")
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(excludedParties = setOf("Labour"))
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals("Conservative", results.first().party?.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `text search plus party filter exclusive returns empty`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, partyName = "Green Party"),
            makeMp(2, partyName = "Green Party")
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(includedParties = setOf("Labour"))
        )
        vm.updateSearchQuery("Green")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(0, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all filters combined`() = runTest(testDispatcher) {
        val mps = listOf(
            makeMp(1, partyName = "Labour", house = 1, isActive = true),
            makeMp(2, partyName = "Labour", house = 2, isActive = true),
            makeMp(3, partyName = "Conservative", house = 1, isActive = true),
            makeMp(4, partyName = "Labour", house = 1, isActive = false)
        )
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(
                includedParties = setOf("Labour"),
                houseFilter = 1,
                currentOnly = true
            )
        )
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(1, results.size)
            assertEquals(1, results.first().id)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `no search no filters returns empty tab counts`() = runTest(testDispatcher) {
        val vm = setupViewModel(searchResults = emptyList())
        vm.tabCounts.test {
            val counts = awaitItem()
            assertTrue(counts.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `search active shows officials badge count`() = runTest(testDispatcher) {
        val mps = listOf(makeMp(1), makeMp(2), makeMp(3))
        val vm = setupViewModel(searchResults = mps)
        vm.updateSearchQuery("Test")

        vm.searchResults.test {
            val results = awaitItem()
            assertEquals(3, results.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `filters active without search shows officials badge count`() = runTest(testDispatcher) {
        val mps = listOf(makeMp(1), makeMp(2))
        val vm = setupViewModel(
            searchResults = mps,
            filterState = DirectoryFilterState(houseFilter = 1)
        )

        vm.filterState.test {
            val state = awaitItem()
            assertTrue(state.hasActiveFilters)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `party tri-state cycle disabled to included to excluded`() {
        val state = PartyFilterState.DISABLED
        assertEquals(PartyFilterState.INCLUDED, state.next())
        assertEquals(PartyFilterState.EXCLUDED, PartyFilterState.INCLUDED.next())
        assertEquals(PartyFilterState.DISABLED, PartyFilterState.EXCLUDED.next())
    }
}
