package com.expensetracker.app.feature.dashboard

import com.expensetracker.app.data.model.CategorySpend
import com.expensetracker.app.data.model.Expense
import java.time.YearMonth

data class DashboardUiState(
    val yearMonth: YearMonth = YearMonth.now(),
    val totalSpentMinor: Long = 0L,
    val overallLimitMinor: Long? = null,
    /** Only categories with spend this month, sorted by biggest spend first. */
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentExpenses: List<Expense> = emptyList(),
    val isLoading: Boolean = true,
) {
    val overallProgress: Float
        get() = if (overallLimitMinor != null && overallLimitMinor > 0) totalSpentMinor.toFloat() / overallLimitMinor else 0f

    val remainingMinor: Long?
        get() = overallLimitMinor?.let { it - totalSpentMinor }
}
