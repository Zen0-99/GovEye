package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.entity.FollowEntity
import com.goveye.app.data.local.entity.FollowedMpWithDetail
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val followDao: FollowDao,
) {
    fun observeFollowedMps(): Flow<List<Int>> = followDao.observeFollowedMemberIds()

    fun observeFollowedMpsWithDetails(): Flow<List<FollowedMpWithDetail>> =
        followDao.observeFollowedMpsWithDetails()

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
