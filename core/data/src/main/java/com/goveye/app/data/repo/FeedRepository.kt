package com.goveye.app.data.repo

import android.util.Log
import com.goveye.app.data.local.dao.DivisionDao
import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.RecessDateDao
import com.goveye.app.data.local.dao.TagDao
import com.goveye.app.data.local.entity.DivisionEntity
import com.goveye.app.data.local.entity.DivisionTagEntity
import com.goveye.app.data.local.entity.RecessDateEntity
import com.goveye.app.domain.model.Division
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
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
    val divisionTags: Map<Int, List<String>> = emptyMap(),
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
    private val recessDateDao: RecessDateDao,
    private val tagDao: TagDao
) {

    /**
     * Observe the full feed data (all divisions, plus the followed-MP highlight set).
     * The UI layer applies the "Following only" filter on top of this.
     * Default limit of 50 — loads only the most recent 50 divisions for fast
     * initial render. The UI can request more as the user scrolls.
     *
     * Optimization: the followed-MP division IDs are computed via a
     * flatMapLatest on the followed-IDs flow, so the SQL query only
     * re-runs when the followed set actually changes — not on every
     * division table emission. The followed member IDs and the division
     * IDs with followed votes are carried together as a Pair so both
     * are available without a second query.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private data class FollowedVoteData(val followedMemberIds: Set<Int>, val divisionsWithFollowedVotes: Set<Int>)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeFeedData(limit: Int = 50): Flow<FeedData> = combine(
        divisionDao.observeDivisions(limit),
        followDao.observeUnmutedMemberIds()
            .flatMapLatest { followedIds ->
                if (followedIds.isEmpty()) {
                    flowOf(FollowedVoteData(emptySet(), emptySet()))
                } else {
                    flow {
                        val divIds = divisionDao.getDivisionIdsWithMemberVotes(followedIds).toSet()
                        emit(FollowedVoteData(followedIds.toSet(), divIds))
                    }
                }
            },
        tagDao.observeAllDivisionTagRows()
    ) { divisionEntities, followedData, tagRows ->
        val dbStart = System.currentTimeMillis()
        Log.i(
            "GovEye/FeedRepo",
            "observeFeedData emit — divisions=${divisionEntities.size} followedIds=${followedData.followedMemberIds.size} votesWithFollowed=${followedData.divisionsWithFollowedVotes.size} tagRows=${tagRows.size} limit=$limit"
        )
        // Build divisionId → tags map from tag rows
        val divisionTags = tagRows
            .groupBy { it.divisionId }
            .mapValues { (_, rows) -> rows.map { it.tag } }

        FeedData(
            divisions = divisionEntities.map { it.toDomain() },
            followedMemberIds = followedData.followedMemberIds,
            divisionsWithFollowedVotes = followedData.divisionsWithFollowedVotes,
            divisionTags = divisionTags,
            isLoading = false
        ).also {
            val dbTime = System.currentTimeMillis() - dbStart
            Log.i(
                "GovEye/FeedRepo",
                "FeedData built — ${it.divisions.size} divisions, votesWithFollowed=${it.divisionsWithFollowedVotes.size} tags=${it.divisionTags.size} dbTime=${dbTime}ms"
            )
        }
    }.flowOn(Dispatchers.Default)

    /**
     * Observe the filtered feed — only divisions where at least one unmuted
     * followed MP voted. Used when the "Following only" filter is ON.
     */
    fun observeFeedDataFiltered(limit: Int = 50): Flow<FeedData> = observeFeedData(limit).map { feedData ->
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
