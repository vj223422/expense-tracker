package com.expensetracker.app.data.model

import androidx.compose.runtime.Immutable

/** @Immutable — see data/model/Expense.kt; this one is passed straight into CategoryProgressRow. */
@Immutable
data class CategorySpend(
    val category: ExpenseCategory,
    val spentMinor: Long,
    val limitMinor: Long?,
) {
    /** 0f..1f+ ; not coerced so callers can distinguish "at limit" from "over limit".
     * Divides as Double, not Float — see LimitAlertEvaluator's tierFor for why: Float only
     * represents integers exactly up to ~16.7M, so a Float division here could round onto the
     * wrong side of a color threshold for spend/limits beyond that while the (Double-based)
     * notification logic reads the same numbers correctly. */
    val progress: Float
        get() = if (limitMinor != null && limitMinor > 0) (spentMinor.toDouble() / limitMinor.toDouble()).toFloat() else 0f
}

@Immutable
data class BudgetLimit(
    /** null = the overall monthly limit rather than a per-category one. */
    val category: ExpenseCategory?,
    val limitMinor: Long,
)

@Immutable
data class MonthlySummary(
    val totalSpentMinor: Long,
    val overallLimitMinor: Long?,
    val categorySpends: List<CategorySpend>,
) {
    val overallProgress: Float
        get() = if (overallLimitMinor != null && overallLimitMinor > 0) (totalSpentMinor.toDouble() / overallLimitMinor.toDouble()).toFloat() else 0f
}
