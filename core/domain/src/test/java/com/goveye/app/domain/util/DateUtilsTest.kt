package com.goveye.app.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `Today returns 'Today'`() {
        val today = LocalDate.now().toString()
        assertEquals("Today", DateUtils.formatRelativeDate(today))
    }

    @Test
    fun `Yesterday returns 'Yesterday'`() {
        val yesterday = LocalDate.now().minusDays(1).toString()
        assertEquals("Yesterday", DateUtils.formatRelativeDate(yesterday))
    }

    @Test
    fun `Older date returns formatted date`() {
        assertEquals("15 January 2026", DateUtils.formatRelativeDate("2026-01-15"))
    }

    @Test
    fun `ISO datetime with time component parses correctly`() {
        assertEquals("15 January 2026", DateUtils.formatRelativeDate("2026-01-15T10:30:00"))
    }

    @Test
    fun `Invalid input returns original string`() {
        assertEquals("not-a-date", DateUtils.formatRelativeDate("not-a-date"))
    }

    @Test
    fun `Empty string returns empty string`() {
        assertEquals("", DateUtils.formatRelativeDate(""))
    }
}
