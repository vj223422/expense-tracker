package com.expensetracker.app.data.repository

import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.notification.LimitAlert
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

sealed interface AddExpenseResult {
    data class Success(
        val newAlerts: List<LimitAlert>,
        val expenseId: Long? = null,
    ) : AddExpenseResult
    data class Error(val message: String) : AddExpenseResult
}

interface ExpenseRepository {
    fun observeAllExpenses(profileId: Long): Flow<List<Expense>>
    fun observeExpensesForMonth(profileId: Long, yearMonth: YearMonth): Flow<List<Expense>>
    fun observeCategoryTotals(profileId: Long, yearMonth: YearMonth): Flow<Map<ExpenseCategory, Long>>
    fun observeMonthlyTotal(profileId: Long, yearMonth: YearMonth): Flow<Long>

    suspend fun addExpense(
        profileId: Long,
        amountMinor: Long,
        category: ExpenseCategory,
        note: String,
        date: LocalDate,
        isIncome: Boolean = false,
    ): AddExpenseResult

    suspend fun getExpenseById(id: Long, profileId: Long): Expense?

    /** [expense] already carries its own id/profileId — the fields on it are the new values. */
    suspend fun updateExpense(expense: Expense): AddExpenseResult

    /** [expense] already carries its own profileId — deletion doesn't need a separate one. */
    suspend fun deleteExpense(expense: Expense)

    /** Re-inserts a just-deleted [expense] at its original id/createdAt. */
    suspend fun restoreExpense(expense: Expense): AddExpenseResult
}
