package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.PartyStatsDao
import com.goveye.app.data.local.entity.PartyStatsEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyStatsRepository @Inject constructor(private val partyStatsDao: PartyStatsDao) {
    suspend fun getPartyStats(partyId: Int): PartyStatsEntity? = partyStatsDao.getByPartyId(partyId)
}
