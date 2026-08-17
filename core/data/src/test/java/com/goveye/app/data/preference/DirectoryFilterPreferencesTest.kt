package com.goveye.app.data.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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
        assertEquals(emptySet<String>(), preferences.includedParties.first())
        assertEquals(emptySet<String>(), preferences.excludedParties.first())
        assertEquals(0, preferences.houseFilter.first())  // all
        assertEquals(false, preferences.currentOnly.first())  // include former
    }

    @Test
    fun `write and read back included parties`() = runTest {
        setUp()
        preferences.setIncludedParties(setOf("Labour", "Conservative"))
        assertEquals(setOf("Labour", "Conservative"), preferences.includedParties.first())
    }

    @Test
    fun `write and read back excluded parties`() = runTest {
        setUp()
        preferences.setExcludedParties(setOf("Green Party"))
        assertEquals(setOf("Green Party"), preferences.excludedParties.first())
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
        preferences.setIncludedParties(setOf("Labour"))
        preferences.setExcludedParties(setOf("Conservative"))
        preferences.setHouseFilter(2)
        preferences.setCurrentOnly(true)
        preferences.clearAll()
        assertEquals(emptySet<String>(), preferences.includedParties.first())
        assertEquals(emptySet<String>(), preferences.excludedParties.first())
        assertEquals(0, preferences.houseFilter.first())
        assertEquals(false, preferences.currentOnly.first())
    }

    @Test
    fun `persistence across datastore instances`() = runTest {
        setUp()
        preferences.setIncludedParties(setOf("Green Party"))
        preferences.setExcludedParties(setOf("Labour"))
        preferences.setHouseFilter(0)

        val newPreferences = DirectoryFilterPreferences(dataStore)
        assertEquals(setOf("Green Party"), newPreferences.includedParties.first())
        assertEquals(setOf("Labour"), newPreferences.excludedParties.first())
        assertEquals(0, newPreferences.houseFilter.first())
    }
}
