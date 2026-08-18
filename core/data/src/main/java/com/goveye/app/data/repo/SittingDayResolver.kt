package com.goveye.app.data.repo

import com.goveye.app.data.api.EggTimerApi
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.data.local.entity.RecessDatesMetaEntity
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Determines whether Parliament is sitting on a given day (D-01).
 *
 * Hybrid approach:
 * 1. Check cached recess dates from the Egg Timer API — if today falls
 *    within any recess range, return false (non-sitting).
 * 2. Fallback: if no cached data or fetch fails, return true (assume sitting
 *    — safer to poll more than to miss votes).
 *
 * The VotePollingWorker refreshes recess dates weekly and uses this resolver
 * to decide poll frequency: 30 min on sitting days, 4 hours on non-sitting.
 */
@Singleton
class SittingDayResolver @Inject constructor(
    private val eggTimerApi: EggTimerApi,
    private val recessDateDao: RecessDateDao,
) {
    companion object {
        private const val REFRESH_INTERVAL_MS = 7 * 24 * 60 * 60 * 1000L // 7 days
        private const val COMMONS_HOUSE_ID = 1
    }

    /**
     * Returns true if Parliament (Commons) is sitting on the given date.
     * Falls back to true (sitting) if data is unavailable.
     */
    suspend fun isSittingDay(date: LocalDate = LocalDate.now()): Boolean {
        val recessDates = recessDateDao.getRecessDatesForHouse(COMMONS_HOUSE_ID)
        if (recessDates.isEmpty()) return true // fallback: assume sitting

        return !recessDates.any { recess ->
            val start = LocalDate.parse(recess.startDate)
            val end = LocalDate.parse(recess.endDate)
            !date.isBefore(start) && !date.isAfter(end)
        }
    }

    /**
     * Refresh recess dates from the Egg Timer API if the cache is stale
     * (older than 7 days) or empty. Safe to call on every poll cycle.
     */
    suspend fun refreshRecessDatesIfNeeded() {
        val meta = recessDateDao.getMeta(COMMONS_HOUSE_ID)
        val now = System.currentTimeMillis()
        val needsRefresh = meta == null || (now - meta.lastRefreshedAt > REFRESH_INTERVAL_MS)
        if (!needsRefresh) return

        try {
            val freshDates = eggTimerApi.getRecessDates(COMMONS_HOUSE_ID)
            if (freshDates.isNotEmpty()) {
                recessDateDao.deleteForHouse(COMMONS_HOUSE_ID)
                recessDateDao.insertAll(
                    freshDates.map {
                        RecessDateEntity(
                            house = COMMONS_HOUSE_ID,
                            description = it.description,
                            startDate = it.startDate.toString(),
                            endDate = it.endDate.toString(),
                        )
                    },
                )
                recessDateDao.insertMeta(
                    RecessDatesMetaEntity(house = COMMONS_HOUSE_ID, lastRefreshedAt = now),
                )
            }
        } catch (e: Exception) {
            // Fetch failed — keep existing cache, try again next cycle
        }
    }
}
