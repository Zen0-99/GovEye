package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.BioDataDao
import com.goveye.app.data.local.entity.BioDataEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BioDataRepository @Inject constructor(private val bioDataDao: BioDataDao) {
    suspend fun getBioData(mpId: Int): BioDataEntity? = bioDataDao.getByMpId(mpId)
}
