package com.goveye.app.data.repo

import com.goveye.app.data.local.dao.ExpenseBucketTotal
import com.goveye.app.data.local.dao.ExpenseDao
import com.goveye.app.data.local.entity.ExpenseEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpensesRepository @Inject constructor(private val expenseDao: ExpenseDao) {
    suspend fun getExpenses(mpId: Int): List<ExpenseEntity> = expenseDao.getByMpId(mpId)

    suspend fun getBucketTotals(mpId: Int): List<ExpenseBucketTotal> = expenseDao.getBucketTotals(mpId)
}
