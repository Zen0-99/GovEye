package com.goveye.app.ui.screens.mpprofile

import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.entity.BioDataEntity
import com.goveye.app.data.local.entity.ExpenseEntity
import com.goveye.app.data.local.entity.MpLinkEntity
import com.goveye.app.data.local.entity.MpTagEntity
import com.goveye.app.domain.model.BiographyExperience
import com.goveye.app.domain.model.Committee
import com.goveye.app.domain.model.Contact
import com.goveye.app.domain.model.DivisionVote
import com.goveye.app.domain.model.Interest
import com.goveye.app.domain.model.MemberVoteWithDivision
import com.goveye.app.domain.model.Mp
import com.goveye.app.domain.stats.ActivityScore
import com.goveye.app.domain.stats.RebellionStats
import com.goveye.app.domain.stats.TraitBar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Snapshot of all profile data for a single MP.
 * Stored in [ProfileCache] so revisiting an MP is instant.
 *
 * Includes ALL UI state fields (votes, interests, expenses, committees,
 * experiences) so that a cache hit restores the full profile — not just
 * the header. Without these, revisiting an MP within the TTL shows empty
 * Stats/Finances/Activity tabs because [refreshFromDb] didn't reload them.
 */
data class CachedProfileData(
    val mp: Mp?,
    val synopsis: String?,
    val contacts: List<Contact>,
    val bioData: BioDataEntity?,
    val mpLinks: MpLinkEntity?,
    val mpTags: List<MpTagEntity>,
    val activityScore: ActivityScore?,
    val traitBars: List<TraitBar>,
    val samePartyMps: List<Mp>,
    val committeePeerMps: List<Mp>,
    val memberVotes: List<MemberVoteWithDivision>,
    val rebellionStats: RebellionStats?,
    val allDivisionDates: List<String>,
    val allVotesByDivision: Map<Int, List<DivisionVote>>,
    val memberPartyName: String?,
    val interests: List<Interest>,
    val expenseBucketTotals: List<ExpenseBucketTotal>,
    val expenses: List<ExpenseEntity>,
    val committees: List<Committee>,
    val experiences: List<BiographyExperience>
)

/**
 * In-memory LRU cache of MP profile data.
 *
 * When the user navigates away from an MP profile and comes back,
 * the ViewModel is recreated and normally re-fetches everything from
 * the DB. This cache short-circuits that: if the profile was recently
 * loaded, the cached snapshot is returned immediately (isLoading=false
 * from the first frame), and a background refresh updates any stale
 * fields silently.
 *
 * Entries expire after [CACHE_TTL_MS] to avoid showing very stale data.
 */
@Singleton
class ProfileCache @Inject constructor() {
    private val cache = LinkedHashMap<Int, Entry>()

    companion object {
        private const val MAX_ENTRIES = 20
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutes
    }

    private data class Entry(val data: CachedProfileData, val timestamp: Long)

    /**
     * Get cached profile data if it exists and hasn't expired.
     * Returns null on miss — caller should do a full load.
     */
    fun get(memberId: Int): CachedProfileData? {
        val entry = cache[memberId] ?: return null
        val now = System.currentTimeMillis()
        if (now - entry.timestamp > CACHE_TTL_MS) {
            cache.remove(memberId)
            return null
        }
        return entry.data
    }

    /**
     * Store profile data in the cache. Evicts oldest entries if over capacity.
     */
    fun put(memberId: Int, data: CachedProfileData) {
        if (cache.size >= MAX_ENTRIES) {
            // Evict oldest entry (first in insertion order)
            val oldestKey = cache.keys.first()
            cache.remove(oldestKey)
        }
        cache[memberId] = Entry(data, System.currentTimeMillis())
    }

    fun clear() = cache.clear()
}
