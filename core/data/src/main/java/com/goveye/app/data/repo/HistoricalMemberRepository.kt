package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.HistoricalMemberDao
import com.goveye.app.data.local.entity.HistoricalMemberEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoricalMemberRepository @Inject constructor(private val historicalMemberDao: HistoricalMemberDao) {
    suspend fun getByTwfyPersonId(twfyPersonId: Int): HistoricalMemberEntity? =
        historicalMemberDao.getByTwfyPersonId(twfyPersonId)

    suspend fun getByTwfyPersonIds(twfyPersonIds: List<Int>): List<HistoricalMemberEntity> =
        historicalMemberDao.getByTwfyPersonIds(twfyPersonIds)

    suspend fun getByParliamentMemberId(parliamentMemberId: Int): HistoricalMemberEntity? =
        historicalMemberDao.getByParliamentMemberId(parliamentMemberId)

    suspend fun getCurrentMembers(): List<HistoricalMemberEntity> = historicalMemberDao.getCurrentMembers()

    suspend fun getAll(): List<HistoricalMemberEntity> = historicalMemberDao.getAll()

    suspend fun search(query: String): List<HistoricalMemberEntity> = historicalMemberDao.search(query)

    suspend fun count(): Int = historicalMemberDao.count()

    suspend fun currentCount(): Int = historicalMemberDao.currentCount()

    suspend fun searchByDisplayName(name: String): List<HistoricalMemberEntity> =
        historicalMemberDao.searchByDisplayName(name)
}
