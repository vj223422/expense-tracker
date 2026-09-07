package com.expensetracker.app.feature.transactions

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.LocalDate

/** @Immutable — see data/model/Expense.kt. */
@Immutable
data class DateGroup(val date: LocalDate, val expenses: List<Expense>, val totalMinor: Long)

@Immutable
data class TransactionsUiState(
    val expensesByDate: List<DateGroup> = emptyList(),
    val selectedCategoryFilter: ExpenseCategory? = null,
    val isLoading: Boolean = true,
) {
    val isEmpty: Boolean get() = !isLoading && expensesByDate.isEmpty()
}

sealed interface TransactionsEffect {
    data class ShowUndoDelete(val expense: Expense) : TransactionsEffect
}

@Stable
interface TransactionsActions {
    fun onFilterChange(category: ExpenseCategory?)
    fun onDeleteExpense(expense: Expense)
    fun onUndoDelete(expense: Expense)
}
