package com.expensetracker.app.data.repository

import com.expensetracker.app.data.entity.BudgetLimitEntity
import com.expensetracker.app.data.local.dao.BudgetLimitDao
import com.expensetracker.app.data.model.BudgetLimit
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BudgetRepositoryImpl(
    private val budgetLimitDao: BudgetLimitDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BudgetRepository {

    override fun observeLimits(): Flow<List<BudgetLimit>> =
        budgetLimitDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun setLimit(category: ExpenseCategory?, limitMinor: Long) = withContext(ioDispatcher) {
        budgetLimitDao.upsert(
            BudgetLimitEntity(
                categoryKey = BudgetLimit(category, limitMinor).categoryKey(),
                limitMinor = limitMinor,
                updatedAtEpochMillis = System.currentTimeMillis(),
            ),
        )
    }

    override suspend fun clearLimit(category: ExpenseCategory?) = withContext(ioDispatcher) {
        budgetLimitDao.delete(BudgetLimit(category, 0L).categoryKey())
    }
}
