package com.expensetracker.app.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.data.model.CategorySpend
import com.expensetracker.app.data.model.Expense
import java.time.YearMonth

/** @Immutable — see data/model/Expense.kt; also holds a java.time.YearMonth field, same issue. */
@Immutable
data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val totalSpentMinor: Long = 0L,
    val overallLimitMinor: Long? = null,
    /** Only categories with spend this month, sorted by biggest spend first. */
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
) {
    // Double, not Float — see LimitAlertEvaluator.tierFor / CategorySpend.progress for why.
    val overallProgress: Float
        get() = if (overallLimitMinor != null && overallLimitMinor > 0) (totalSpentMinor.toDouble() / overallLimitMinor.toDouble()).toFloat() else 0f

    val remainingMinor: Long?
        get() = overallLimitMinor?.let { it - totalSpentMinor }
}

sealed interface DashboardEffect {
    data class ShowUndoDelete(val expense: Expense) : DashboardEffect
    data class ShowMessage(val message: String) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}

@Stable
interface DashboardActions {
    /** Returns whether the delete actually succeeded — SwipeToDeleteExpenseItem awaits this to
     * know whether to reset the swiped-away row back to visible on failure. */
    suspend fun onDeleteExpense(expense: Expense): Boolean
    fun onUndoDelete(expense: Expense)
}
