package com.expensetracker.app.feature.budgets

import androidx.compose.runtime.Stable
import com.expensetracker.app.data.model.CategorySpend
import com.expensetracker.app.data.model.ExpenseCategory

sealed interface EditTarget {
    data object Overall : EditTarget
    data class Category(val category: ExpenseCategory) : EditTarget
}

data class BudgetsUiState(
    val overallLimitMinor: Long? = null,
    val overallSpentMinor: Long = 0L,
    /** One entry per ExpenseCategory, in enum declaration order. */
    val categorySpends: List<CategorySpend> = emptyList(),
    val isLoading: Boolean = true,
    val editingTarget: EditTarget? = null,
) {
    val overallProgress: Float
        get() = if (overallLimitMinor != null && overallLimitMinor > 0) overallSpentMinor.toFloat() / overallLimitMinor else 0f
}

@Stable
interface BudgetsActions {
    fun onEditOverall()
    fun onEditCategory(category: ExpenseCategory)
    fun onDismissEdit()
    fun onSaveLimit(limitMinor: Long)
    fun onClearLimit()
}
