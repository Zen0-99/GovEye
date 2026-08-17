package com.goveye.app.ui.screens.directory

import com.goveye.app.data.preference.DirectoryFilterPreferences
import com.goveye.app.data.preference.DirectoryPreferences
import com.goveye.app.data.repo.MembersRepository
import io.mockk.mockk
import org.junit.Test

class DirectoryViewModelTest {

    private val membersRepository = mockk<MembersRepository>(relaxed = true)
    private val directoryPreferences = mockk<DirectoryPreferences>(relaxed = true)
    private val directoryFilterPreferences = mockk<DirectoryFilterPreferences>(relaxed = true)

    @Test
    fun `viewmodel can be instantiated`() {
        // Basic smoke test — full flow tests require API mocking
        DirectoryViewModel(membersRepository, directoryPreferences, directoryFilterPreferences)
    }
}
