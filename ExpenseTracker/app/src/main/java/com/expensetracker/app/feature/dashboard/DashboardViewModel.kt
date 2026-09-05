package com.expensetracker.app.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.buildMonthlySummary
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth

/** No effects channel — every value here is durable state (survives rotation), nothing fire-once. */
class DashboardViewModel(
    expenseRepository: ExpenseRepository,
    budgetRepository: BudgetRepository,
) : ViewModel() {

    private val yearMonth: YearMonth = YearMonth.now()

    val uiState: StateFlow<DashboardUiState> = combine(
        expenseRepository.observeCategoryTotals(yearMonth),
        budgetRepository.observeLimits(),
        expenseRepository.observeExpensesForMonth(yearMonth),
    ) { totals, limits, monthExpenses ->
        val summary = buildMonthlySummary(totals, limits)
        DashboardUiState(
            yearMonth = yearMonth,
            totalSpentMinor = summary.totalSpentMinor,
            overallLimitMinor = summary.overallLimitMinor,
            categorySpends = summary.categorySpends
                .filter { it.spentMinor > 0 }
                .sortedByDescending { it.spentMinor },
            recentExpenses = monthExpenses.take(5),
            isLoading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
