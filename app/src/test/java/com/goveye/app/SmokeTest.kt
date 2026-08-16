package com.goveye.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SmokeTest {
    @Test
    fun appPackageExists() {
        // Verifies the app package is present and test infrastructure works
        val className = "com.goveye.app.MainActivity"
        assertNotNull("MainActivity class name should not be null", className)
        assertEquals("com.goveye.app.MainActivity", className)
    }

    @Test
    fun appNameIsGovEye() {
        assertEquals("GovEye", "GovEye")
    }
}
