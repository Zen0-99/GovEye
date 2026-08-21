package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.domain.model.Division
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Combined feed UI state: divisions, followed MP IDs, the set of division IDs
 * where at least one unmuted followed MP voted, the active recess period (if any),
 * and a loading flag.
 */
data class FeedData(
    val divisions: List<Division> = emptyList(),
    val followedMemberIds: Set<Int> = emptySet(),
    val divisionsWithFollowedVotes: Set<Int> = emptySet(),
    val currentRecess: RecessDateEntity? = null,
    val isLoading: Boolean = true
)

/**
 * Combines divisions (BundledDatabase) + followed MP IDs (LocalDatabase) +
 * recess status (BundledDatabase) into a single observable feed state.
 *
 * IMPORTANT (SPEC.md prohibition): FeedRepository MUST NOT depend on
 * HansardApi or HansardRepository. The feed list comes exclusively from the
 * bundled DB.
 *
 * Cross-database JOIN avoidance: `follows` is in LocalDatabase, `division_votes`
 * is in BundledDatabase. Instead of a SQL JOIN, the repository observes both
 * flows and combines them in Kotlin, then fetches the set of division IDs with
 * followed-MP votes via [DivisionDao.getDivisionIdsWithMemberVotes] (which
 * operates on `division_votes` only).
 */
@Singleton
class FeedRepository @Inject constructor(
    private val divisionDao: DivisionDao,
    private val followDao: FollowDao,
    private val recessDateDao: RecessDateDao
) {

    /**
     * Observe the full feed data (all divisions, plus the followed-MP highlight set).
     * The UI layer applies the "Following only" filter on top of this.
     */
    fun observeFeedData(limit: Int = 200): Flow<FeedData> = combine(
        divisionDao.observeDivisions(limit),
        followDao.observeUnmutedMemberIds()
    ) { divisionEntities, followedIds ->
        val divisionIdsWithFollowedVotes = if (followedIds.isEmpty()) {
            emptySet()
        } else {
            divisionDao.getDivisionIdsWithMemberVotes(followedIds).toSet()
        }
        FeedData(
            divisions = divisionEntities.map { it.toDomain() },
            followedMemberIds = followedIds.toSet(),
            divisionsWithFollowedVotes = divisionIdsWithFollowedVotes,
            isLoading = false
        )
    }

    /**
     * Observe the filtered feed — only divisions where at least one unmuted
     * followed MP voted. Used when the "Following only" filter is ON.
     */
    fun observeFeedDataFiltered(limit: Int = 200): Flow<FeedData> = observeFeedData(limit).map { feedData ->
        feedData.copy(
            divisions = feedData.divisions.filter { it.id in feedData.divisionsWithFollowedVotes }
        )
    }

    /**
     * Check if Parliament is currently in recess for the given house.
     * Returns the active recess period, or null if sitting / no recess data.
     * The feed ViewModel calls this on demand for the recess empty state.
     */
    suspend fun getCurrentRecess(house: Int = 1): RecessDateEntity? =
        recessDateDao.getCurrentRecess(house, LocalDate.now().toString())

    private fun DivisionEntity.toDomain(): Division = Division(
        id = id,
        title = title,
        date = date,
        number = number,
        ayeCount = ayeCount,
        noCount = noCount,
        isDeferred = isDeferred,
        house = house,
        twfyDebateUrl = twfyDebateUrl
    )
}
