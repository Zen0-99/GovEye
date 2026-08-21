package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.MpLinkDao
import com.goveye.app.data.local.entity.MpLinkEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MpLinksRepository @Inject constructor(private val mpLinkDao: MpLinkDao) {
    suspend fun getLinks(mpId: Int): MpLinkEntity? = mpLinkDao.getByMpId(mpId)
}
