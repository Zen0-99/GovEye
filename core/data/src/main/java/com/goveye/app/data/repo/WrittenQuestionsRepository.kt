package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.WrittenQuestionDao
import com.goveye.app.data.local.entity.WrittenQuestionEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WrittenQuestionsRepository @Inject constructor(private val writtenQuestionDao: WrittenQuestionDao) {
    suspend fun getQuestionsByMember(memberId: Int): List<WrittenQuestionEntity> =
        writtenQuestionDao.getByMemberId(memberId)

    suspend fun getQuestionsByMemberAndDateRange(memberId: Int, startDate: String): List<WrittenQuestionEntity> =
        writtenQuestionDao.getByMemberIdAndDateRange(memberId, startDate)

    suspend fun getQuestion(id: Int): WrittenQuestionEntity? = writtenQuestionDao.getQuestion(id)
}
