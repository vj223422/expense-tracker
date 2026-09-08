package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.BudgetLimit
import com.expensetracker.app.data.model.ExpenseCategory
import kotlinx.coroutines.flow.Flow

sealed interface BudgetSaveResult {
    data object Success : BudgetSaveResult
    data class Error(val message: String) : BudgetSaveResult
}

interface BudgetRepository {
    fun observeLimits(profileId: Long): Flow<List<BudgetLimit>>

    /** null = the overall monthly limit rather than a per-category one. */
    suspend fun setLimit(profileId: Long, category: ExpenseCategory?, limitMinor: Long): BudgetSaveResult
    suspend fun clearLimit(profileId: Long, category: ExpenseCategory?): BudgetSaveResult
}
