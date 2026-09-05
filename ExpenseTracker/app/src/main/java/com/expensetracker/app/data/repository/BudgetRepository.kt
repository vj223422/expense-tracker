package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.BudgetLimit
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeLimits(profileId: Long): Flow<List<BudgetLimit>>

    /** null = the overall monthly limit rather than a per-category one. */
    suspend fun setLimit(profileId: Long, category: ExpenseCategory?, limitMinor: Long)
    suspend fun clearLimit(profileId: Long, category: ExpenseCategory?)
}
