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
    data class ShowMessage(val message: String) : TransactionsEffect
    data class ShowError(val message: String) : TransactionsEffect
}

@Stable
interface TransactionsActions {
    fun onFilterChange(category: ExpenseCategory?)

    /** Returns whether the delete actually succeeded — SwipeToDeleteExpenseItem awaits this to
     * know whether to reset the swiped-away row back to visible on failure. */
    suspend fun onDeleteExpense(expense: Expense): Boolean
    fun onUndoDelete(expense: Expense)
}
