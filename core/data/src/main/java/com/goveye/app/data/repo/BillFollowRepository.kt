package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.BillFollowDao
import com.goveye.app.data.local.entity.BillFollowEntity
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class BillFollowRepository @Inject constructor(private val billFollowDao: BillFollowDao) {
    fun observeFollowedBillIds(): Flow<List<Int>> = billFollowDao.observeFollowedBillIds()

    fun observeIsFollowing(billId: Int): Flow<Boolean> = billFollowDao.observeIsFollowing(billId)

    suspend fun isFollowing(billId: Int): Boolean = billFollowDao.isFollowing(billId)

    suspend fun getFollowedBillIds(): List<Int> = billFollowDao.getFollowedBillIds()

    suspend fun follow(billId: Int) {
        billFollowDao.insert(BillFollowEntity(billId, System.currentTimeMillis()))
    }

    suspend fun unfollow(billId: Int) {
        billFollowDao.delete(billId)
    }
}
