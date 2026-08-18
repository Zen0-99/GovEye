package com.goveye.app.data.api

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Client for the UK Parliament Egg Timer API (https://api.parliament.uk/egg-timer).
 *
 * The API returns HTML-rendered tables, not JSON. This client fetches the
 * recess dates page and parses the table rows with regex to extract
 * (description, startDate, endDate) tuples.
 *
 * Used by [com.goveye.app.domain.stats.SittingDayResolver] to determine
 * whether Parliament is sitting on a given day (D-01).
 */
@Singleton
class EggTimerApi @Inject constructor(
    private val client: OkHttpClient,
) {
    companion object {
        private const val BASE_URL = "https://api.parliament.uk"
        // houseId: 1 = Commons, 2 = Lords
        private const val RECESS_PATH = "/egg-timer/houses/%d/recess-dates"
    }

    data class RecessDate(
        val description: String,
        val startDate: LocalDate,
        val endDate: LocalDate,
    )

    /**
     * Fetch recess dates for the given house.
     *
     * Returns an empty list on failure — callers should treat empty as
     * "unknown, assume sitting" (safer to poll more).
     */
    suspend fun getRecessDates(houseId: Int): List<RecessDate> {
        return try {
            val url = BASE_URL + RECESS_PATH.format(houseId)
            val request = Request.Builder().url(url).get().build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return emptyList()
            parseRecessDates(html)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Parse the recess dates HTML table.
     *
     * The table rows look like:
     *   Christmas recess 2026 │Friday 18 December 2026 │Sunday 3 January 2027
     *
     * We extract the description and two dates, then parse them into LocalDate.
     */
    internal fun parseRecessDates(html: String): List<RecessDate> {
        val results = mutableListOf<RecessDate>()
        // Match table rows: description | start date | end date
        // The dates are in format "Friday 18 December 2026"
        val datePattern = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", java.util.Locale.ENGLISH)
        // Split by table row delimiters (│ or |)
        val lines = html.split("\n")
        for (line in lines) {
            if (!line.contains("recess") && !line.contains("Recess")) continue
            val cells = line.split("│", "|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.size < 3) continue
            val description = cells[0]
            val startDate = parseDate(cells[1], datePattern) ?: continue
            val endDate = parseDate(cells[2], datePattern) ?: continue
            results.add(RecessDate(description, startDate, endDate))
        }
        return results
    }

    private fun parseDate(text: String, formatter: DateTimeFormatter): LocalDate? {
        return try {
            LocalDate.parse(text.trim(), formatter)
        } catch (e: Exception) {
            null
        }
    }
}
