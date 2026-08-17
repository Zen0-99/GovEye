package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DirectoryFilterPreferencesTest {
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var preferences: DirectoryFilterPreferences

    private fun setUp() {
        dataStore = PreferenceDataStoreFactory.create(
            produceFile = { RuntimeEnvironment.getApplication().preferencesDataStoreFile("test_filter_prefs") },
        )
        preferences = DirectoryFilterPreferences(dataStore)
    }

    @Test
    fun `default values on first read`() = runTest {
        setUp()
        assertEquals(emptySet<String>(), preferences.selectedParties.first())
        assertEquals(1, preferences.houseFilter.first())  // Commons
        assertTrue(preferences.currentOnly.first())
    }

    @Test
    fun `write and read back party filter`() = runTest {
        setUp()
        preferences.setSelectedParties(setOf("Labour", "Conservative"))
        assertEquals(setOf("Labour", "Conservative"), preferences.selectedParties.first())
    }

    @Test
    fun `write and read back house filter`() = runTest {
        setUp()
        preferences.setHouseFilter(2)  // Lords
        assertEquals(2, preferences.houseFilter.first())
    }

    @Test
    fun `write and read back status filter`() = runTest {
        setUp()
        preferences.setCurrentOnly(false)
        assertEquals(false, preferences.currentOnly.first())
    }

    @Test
    fun `clear all resets to defaults`() = runTest {
        setUp()
        preferences.setSelectedParties(setOf("Labour"))
        preferences.setHouseFilter(2)
        preferences.setCurrentOnly(false)
        preferences.clearAll()
        assertEquals(emptySet<String>(), preferences.selectedParties.first())
        assertEquals(1, preferences.houseFilter.first())
        assertTrue(preferences.currentOnly.first())
    }

    @Test
    fun `persistence across datastore instances`() = runTest {
        setUp()
        preferences.setSelectedParties(setOf("Green Party"))
        preferences.setHouseFilter(0)

        // Create a new instance pointing to the same file
        val newPreferences = DirectoryFilterPreferences(dataStore)
        assertEquals(setOf("Green Party"), newPreferences.selectedParties.first())
        assertEquals(0, newPreferences.houseFilter.first())
    }
}
