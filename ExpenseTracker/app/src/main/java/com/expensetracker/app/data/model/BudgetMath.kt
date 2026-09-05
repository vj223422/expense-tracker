package com.expensetracker.app.data.model

/** One [CategorySpend] per category (even zero-spend ones) so the Budgets screen can list every category. */
fun buildCategorySpends(totals: Map<ExpenseCategory, Long>, limits: List<BudgetLimit>): List<CategorySpend> {
    val limitByCategory = limits.mapNotNull { limit -> limit.category?.let { it to limit.limitMinor } }.toMap()
    return ExpenseCategory.entries.map { category ->
        CategorySpend(
            category = category,
            spentMinor = totals[category] ?: 0L,
            limitMinor = limitByCategory[category],
        )
    }
}

fun buildMonthlySummary(totals: Map<ExpenseCategory, Long>, limits: List<BudgetLimit>): MonthlySummary {
    val overallLimit = limits.firstOrNull { it.category == null }?.limitMinor
    return MonthlySummary(
        totalSpentMinor = totals.values.sum(),
        overallLimitMinor = overallLimit,
        categorySpends = buildCategorySpends(totals, limits),
    )
}
