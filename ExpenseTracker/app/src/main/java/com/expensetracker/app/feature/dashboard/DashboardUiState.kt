package com.expensetracker.app.feature.dashboard

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.data.model.CategorySpend
import com.expensetracker.app.data.model.Expense
import java.time.YearMonth

@Immutable
data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val totalSpentMinor: Long = 0L,
    val totalIncomeMinor: Long = 0L,
    val overallLimitMinor: Long? = null,
    val carryForwardMinor: Long = 0L,
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
) {
    val effectiveBudgetMinor: Long?
        get() = overallLimitMinor?.plus(totalIncomeMinor)?.plus(carryForwardMinor)

    val overallProgress: Float
        get() = effectiveBudgetMinor?.takeIf { it > 0L }?.let {
            (totalSpentMinor.toDouble() / it.toDouble()).toFloat()
        } ?: 0f

    val remainingMinor: Long?
        get() = effectiveBudgetMinor?.minus(totalSpentMinor)
}

sealed interface DashboardEffect {
    data class ShowUndoDelete(val expense: Expense) : DashboardEffect
    data class ShowMessage(val message: String) : DashboardEffect
    data class ShowError(val message: String) : DashboardEffect
}

@Stable
interface DashboardActions {
    suspend fun onDeleteExpense(expense: Expense): Boolean
    fun onUndoDelete(expense: Expense)
    fun onPreviousMonth()
    fun onNextMonth()
}
