package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.BudgetLimit
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun observeLimits(): Flow<List<BudgetLimit>>

    /** null = the overall monthly limit rather than a per-category one. */
    suspend fun setLimit(category: ExpenseCategory?, limitMinor: Long)
    suspend fun clearLimit(category: ExpenseCategory?)
}
