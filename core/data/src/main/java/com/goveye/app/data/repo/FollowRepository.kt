package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.FollowDao
import com.goveye.app.data.local.entity.FollowEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FollowRepository @Inject constructor(
    private val followDao: FollowDao,
) {
    fun observeFollowedMps(): Flow<List<Int>> = followDao.observeFollowedMemberIds()

    suspend fun follow(memberId: Int) {
        followDao.insert(FollowEntity(memberId, System.currentTimeMillis()))
    }

    suspend fun unfollow(memberId: Int) {
        followDao.delete(memberId)
    }

    suspend fun isFollowing(memberId: Int): Boolean = followDao.isFollowing(memberId)
}
