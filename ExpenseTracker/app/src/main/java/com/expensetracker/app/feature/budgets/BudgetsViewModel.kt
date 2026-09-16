package com.expensetracker.app.feature.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.model.buildMonthlySummary
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.BudgetSaveResult
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetsViewModel(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val appPreferences: AppPreferences,
) : ViewModel(), BudgetsActions {
    private val editingTarget = MutableStateFlow<EditTarget?>(null)
    private val activeProfileId = profileRepository.observeActiveProfileId().filterNotNull()
    private val _effects = Channel<BudgetsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<BudgetsUiState> = activeProfileId.flatMapLatest { profileId ->
        appPreferences.activeBudgetMonth(profileId).flatMapLatest { storedMonth ->
            val yearMonth = storedMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
            combine(
                expenseRepository.observeCategoryTotals(profileId, yearMonth),
                budgetRepository.observeLimits(profileId),
                editingTarget,
            ) { totals, limits, editing ->
                val summary = buildMonthlySummary(totals, limits)
                BudgetsUiState(summary.overallLimitMinor, summary.totalSpentMinor, summary.categorySpends, false, editing)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsUiState())

    override fun onEditOverall() { editingTarget.value = EditTarget.Overall }
    override fun onEditCategory(category: ExpenseCategory) { editingTarget.value = EditTarget.Category(category) }
    override fun onDismissEdit() { editingTarget.value = null }
    override fun onSaveLimit(limitMinor: Long) {
        val target = editingTarget.value ?: return
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            when (val result = budgetRepository.setLimit(profileId, (target as? EditTarget.Category)?.category, limitMinor)) {
                is BudgetSaveResult.Success -> editingTarget.value = null
                is BudgetSaveResult.Error -> _effects.send(BudgetsEffect.ShowMessage(result.message))
            }
        }
    }
    override fun onClearLimit() {
        val target = editingTarget.value ?: return
        viewModelScope.launch {
            val profileId = activeProfileId.first()
            when (val result = budgetRepository.clearLimit(profileId, (target as? EditTarget.Category)?.category)) {
                is BudgetSaveResult.Success -> editingTarget.value = null
                is BudgetSaveResult.Error -> _effects.send(BudgetsEffect.ShowMessage(result.message))
            }
        }
    }
}
