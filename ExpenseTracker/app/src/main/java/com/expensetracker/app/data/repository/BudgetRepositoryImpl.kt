package com.expensetracker.app.data.repository

import android.database.sqlite.SQLiteException
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

    override fun observeLimits(profileId: Long): Flow<List<BudgetLimit>> =
        budgetLimitDao.observeAll(profileId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun setLimit(profileId: Long, category: ExpenseCategory?, limitMinor: Long): BudgetSaveResult =
        withContext(ioDispatcher) {
            try {
                budgetLimitDao.upsert(
                    BudgetLimitEntity(
                        profileId = profileId,
                        categoryKey = BudgetLimit(category, limitMinor).categoryKey(),
                        limitMinor = limitMinor,
                        updatedAtEpochMillis = System.currentTimeMillis(),
                    ),
                )
                BudgetSaveResult.Success
            } catch (e: SQLiteException) {
                BudgetSaveResult.Error("Couldn't save budget limit — local storage error.")
            }
        }

    override suspend fun clearLimit(profileId: Long, category: ExpenseCategory?): BudgetSaveResult =
        withContext(ioDispatcher) {
            try {
                budgetLimitDao.delete(profileId, BudgetLimit(category, 0L).categoryKey())
                BudgetSaveResult.Success
            } catch (e: SQLiteException) {
                BudgetSaveResult.Error("Couldn't clear budget limit — local storage error.")
            }
        }
}
