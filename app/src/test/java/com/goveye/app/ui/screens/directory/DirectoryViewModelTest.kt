package com.goveye.app.ui.screens.directory

import com.goveye.app.data.local.dao.CommitteeDao
import com.goveye.app.data.local.dao.CouncilDao
import com.goveye.app.data.preference.DirectoryFilterPreferences
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.repo.GovernmentAnnouncementsRepository
import com.goveye.app.data.repo.MembersRepository
import com.goveye.app.data.repo.PostcodeRepository
import io.mockk.mockk
import org.junit.Test

class DirectoryViewModelTest {

    private val membersRepository = mockk<MembersRepository>(relaxed = true)
    private val directoryPreferences = mockk<DirectoryPreferences>(relaxed = true)
    private val directoryFilterPreferences = mockk<DirectoryFilterPreferences>(relaxed = true)
    private val postcodeRepository = mockk<PostcodeRepository>(relaxed = true)
    private val councilDao = mockk<CouncilDao>(relaxed = true)
    private val committeeDao = mockk<CommitteeDao>(relaxed = true)
    private val governmentAnnouncementsRepository = mockk<GovernmentAnnouncementsRepository>(relaxed = true)

    @Test
    fun `viewmodel can be instantiated`() {
        // Basic smoke test — full flow tests require API mocking
        DirectoryViewModel(
            membersRepository,
            directoryPreferences,
            directoryFilterPreferences,
            postcodeRepository,
            councilDao,
            committeeDao,
            governmentAnnouncementsRepository
        )
    }
}
