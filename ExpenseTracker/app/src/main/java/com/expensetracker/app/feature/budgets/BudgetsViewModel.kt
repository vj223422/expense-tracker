package com.expensetracker.app.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.model.buildMonthlySummary
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

class BudgetsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
) : ViewModel(), BudgetsActions {

    private val yearMonth: YearMonth = YearMonth.now()
    private val editingTarget = MutableStateFlow<EditTarget?>(null)

    val uiState: StateFlow<BudgetsUiState> = combine(
        expenseRepository.observeCategoryTotals(yearMonth),
        budgetRepository.observeLimits(),
        editingTarget,
    ) { totals, limits, editing ->
        val summary = buildMonthlySummary(totals, limits)
        BudgetsUiState(
            overallLimitMinor = summary.overallLimitMinor,
            overallSpentMinor = summary.totalSpentMinor,
            categorySpends = summary.categorySpends,
            isLoading = false,
            editingTarget = editing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    override fun onEditOverall() {
        editingTarget.value = EditTarget.Overall
    }

    override fun onEditCategory(category: ExpenseCategory) {
        editingTarget.value = EditTarget.Category(category)
    }

    override fun onDismissEdit() {
        editingTarget.value = null
    }

    override fun onSaveLimit(limitMinor: Long) {
        val target = editingTarget.value ?: return
        viewModelScope.launch {
            budgetRepository.setLimit((target as? EditTarget.Category)?.category, limitMinor)
            editingTarget.value = null
        }
    }

    override fun onClearLimit() {
        val target = editingTarget.value ?: return
        viewModelScope.launch {
            budgetRepository.clearLimit((target as? EditTarget.Category)?.category)
            editingTarget.value = null
        }
    }
}
