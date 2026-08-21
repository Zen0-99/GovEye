package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.dao.MpDao
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.FollowedMpWithDetail
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class FollowRepository @Inject constructor(private val followDao: FollowDao, private val mpDao: MpDao) {
    fun observeFollowedMps(): Flow<List<Int>> = followDao.observeFollowedMemberIds()

    /**
     * Combines follow data from [FollowDao] (LocalDatabase) with MP details from
     * [MpDao] (BundledDatabase) in Kotlin (D-10a).
     *
     * The cross-database JOIN is no longer possible since follows and mps are in
     * separate Room databases. Instead, we observe follows, fetch MP details by
     * ID, and combine them here.
     */
    fun observeFollowedMpsWithDetails(): Flow<List<FollowedMpWithDetail>> =
        followDao.observeAllFollows().map { follows ->
            val mpIds = follows.map { it.memberId }
            val mpsById = if (mpIds.isNotEmpty()) {
                mpDao.getMpsByIds(mpIds).associateBy { it.id }
            } else {
                emptyMap()
            }
            follows.mapNotNull { follow ->
                val mp = mpsById[follow.memberId] ?: return@mapNotNull null
                FollowedMpWithDetail(
                    memberId = follow.memberId,
                    followedAt = follow.followedAt,
                    isMuted = follow.isMuted,
                    nameDisplayAs = mp.nameDisplayAs,
                    nameListAs = mp.nameListAs,
                    thumbnailUrl = mp.thumbnailUrl,
                    partyName = mp.partyName,
                    partyAbbreviation = mp.partyAbbreviation,
                    partyBackgroundColour = mp.partyBackgroundColour,
                    partyForegroundColour = mp.partyForegroundColour,
                    constituencyName = mp.constituencyName,
                    house = mp.house
                )
            }
        }

    fun observeUnmutedMemberIds(): Flow<List<Int>> = followDao.observeUnmutedMemberIds()

    suspend fun getUnmutedMemberIds(): List<Int> = followDao.getUnmutedMemberIds()

    suspend fun getFollowCount(): Int = followDao.getFollowCount()

    suspend fun follow(memberId: Int) {
        followDao.insert(FollowEntity(memberId, System.currentTimeMillis(), isMuted = false))
    }

    suspend fun unfollow(memberId: Int) {
        followDao.delete(memberId)
    }

    suspend fun isFollowing(memberId: Int): Boolean = followDao.isFollowing(memberId)

    fun observeIsFollowing(memberId: Int): Flow<Boolean> = followDao.observeIsFollowing(memberId)

    fun observeIsMuted(memberId: Int): Flow<Boolean?> = followDao.observeIsMuted(memberId)

    suspend fun setMuted(memberId: Int, isMuted: Boolean) {
        followDao.setMuted(memberId, isMuted)
    }
}
