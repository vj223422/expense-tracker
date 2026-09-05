package com.expensetracker.app.data.model

data class CategorySpend(
    val category: ExpenseCategory,
    val spentMinor: Long,
    val limitMinor: Long?,
) {
    /** 0f..1f+ ; not coerced so callers can distinguish "at limit" from "over limit". */
    val progress: Float
        get() = if (limitMinor != null && limitMinor > 0) spentMinor.toFloat() / limitMinor.toFloat() else 0f
}

data class BudgetLimit(
    /** null = the overall monthly limit rather than a per-category one. */
    val category: ExpenseCategory?,
    val limitMinor: Long,
)

data class MonthlySummary(
    val totalSpentMinor: Long,
    val overallLimitMinor: Long?,
    val categorySpends: List<CategorySpend>,
) {
    val overallProgress: Float
        get() = if (overallLimitMinor != null && overallLimitMinor > 0) totalSpentMinor.toFloat() / overallLimitMinor.toFloat() else 0f
}
