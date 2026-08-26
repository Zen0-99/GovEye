package com.goveye.app.ui.screens.mpprofile

import app.cash.turbine.test
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.local.entity.MpEntity
import com.goveye.app.data.local.entity.MpNotificationPreferenceEntity
import com.goveye.app.data.preference.ActivityFilterPreferences
import com.goveye.app.data.repo.BioDataRepository
import com.goveye.app.data.repo.CommitteesRepository
import com.goveye.app.data.repo.ExpensesRepository
import com.goveye.app.data.repo.FollowRepository
import com.goveye.app.data.repo.InterestsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.MpLinksRepository
import com.goveye.app.data.repo.NotificationPreferenceRepository
import com.goveye.app.data.repo.StatsRepository
import com.goveye.app.data.repo.VotesRepository
import com.goveye.app.data.repo.WrittenQuestionsRepository
import com.goveye.app.domain.model.Constituency
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.model.Party
import com.goveye.app.domain.model.RepositoryResult
import com.goveye.app.domain.model.SyncStatus
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ProfileViewModelTest {

    private val membersRepository = mockk<MembersRepository>(relaxed = true)
    private val committeesRepository = mockk<CommitteesRepository>(relaxed = true)
    private val votesRepository = mockk<VotesRepository>(relaxed = true)
    private val followRepository = mockk<FollowRepository>(relaxed = true)
    private val notificationPrefRepository = mockk<NotificationPreferenceRepository>(relaxed = true)
    private val mpDao = mockk<MpDao>(relaxed = true)
    private val interestsRepository = mockk<InterestsRepository>(relaxed = true)
    private val bioDataRepository = mockk<BioDataRepository>(relaxed = true)
    private val expensesRepository = mockk<ExpensesRepository>(relaxed = true)
    private val mpLinksRepository = mockk<MpLinksRepository>(relaxed = true)
    private val statsRepository = mockk<StatsRepository>(relaxed = true)
    private val writtenQuestionsRepository = mockk<WrittenQuestionsRepository>(relaxed = true)
    private val activityFilterPreferences = mockk<ActivityFilterPreferences>(relaxed = true)
    private val tagDao = mockk<TagDao>(relaxed = true)

    private fun createViewModel() = ProfileViewModel(
        membersRepository,
        committeesRepository,
        votesRepository,
        followRepository,
        notificationPrefRepository,
        mpDao,
        interestsRepository,
        bioDataRepository,
        expensesRepository,
        mpLinksRepository,
        statsRepository,
        writtenQuestionsRepository,
        activityFilterPreferences,
        tagDao
    )

    private fun makeMpEntity(id: Int): MpEntity = MpEntity(
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
        membershipStartDate = "2019-12-12",
        membershipEndDate = null,
        isActive = true,
        thumbnailUrl = null,
        lastUpdated = System.currentTimeMillis()
    )

    private fun makeDomainMp(id: Int): Mp = Mp(
        id = id,
        nameListAs = "Test MP $id",
        nameDisplayAs = "Test $id",
        nameFullTitle = null,
        gender = null,
        party = Party(15, "Labour", "Lab", "d50000", "ffffff"),
        constituency = Constituency(4074, "Test Constituency"),
        house = 1,
        membershipStartDate = "2019-12-12",
        isActive = true,
        thumbnailUrl = null
    )

    @Test
    fun `loads MP from repository`() = runTest {
        val mp = makeDomainMp(1)
        every { membersRepository.observeMp(1) } returns flowOf(
            RepositoryResult(mp, SyncStatus.FRESH)
        )
        every { committeesRepository.observeCommitteesForMember(1) } returns flowOf(
            RepositoryResult(emptyList(), SyncStatus.EMPTY)
        )
        coEvery { membersRepository.getSynopsis(1) } returns "Test bio"
        coEvery { membersRepository.getContact(1) } returns emptyList()
        coEvery { membersRepository.getExperience(1) } returns emptyList()

        every { followRepository.observeIsFollowing(1) } returns flowOf(false)
        every { notificationPrefRepository.observe(1) } returns flowOf(MpNotificationPreferenceEntity(1))

        val viewModel = createViewModel()
        viewModel.loadProfile(1)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNotNull(state.mp)
            assertEquals("Test 1", state.mp?.nameDisplayAs)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handles MP not found`() = runTest {
        every { membersRepository.observeMp(999) } returns flowOf(
            RepositoryResult(null, SyncStatus.EMPTY)
        )
        every { committeesRepository.observeCommitteesForMember(999) } returns flowOf(
            RepositoryResult(emptyList(), SyncStatus.EMPTY)
        )
        coEvery { membersRepository.getSynopsis(999) } returns null
        coEvery { membersRepository.getContact(999) } returns emptyList()
        coEvery { membersRepository.getExperience(999) } returns emptyList()

        every { followRepository.observeIsFollowing(999) } returns flowOf(false)
        every { notificationPrefRepository.observe(999) } returns flowOf(MpNotificationPreferenceEntity(999))

        val viewModel = createViewModel()
        viewModel.loadProfile(999)

        viewModel.uiState.test {
            val state = awaitItem()
            assertNull(state.mp)
            assertEquals(SyncStatus.EMPTY, state.syncStatus)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
