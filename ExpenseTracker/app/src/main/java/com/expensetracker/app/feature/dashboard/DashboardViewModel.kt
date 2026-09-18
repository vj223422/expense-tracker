package com.expensetracker.app.feature.dashboard

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.buildMonthlySummary
import com.expensetracker.app.data.notification.toSnackbarMessage
import com.expensetracker.app.data.prefs.AppPreferences
import com.expensetracker.app.data.repository.AddExpenseResult
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.BudgetSaveResult
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
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
class DashboardViewModel(
    private val expenseRepository: ExpenseRepository,
    private val budgetRepository: BudgetRepository,
    private val profileRepository: ProfileRepository,
    private val appPreferences: AppPreferences,
) : ViewModel(), DashboardActions {
    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private val _showBudgetEditor = MutableStateFlow(false)
    val showBudgetEditor: StateFlow<Boolean> = _showBudgetEditor
    private val selectedMonth = MutableStateFlow<YearMonth?>(null)

    val uiState: StateFlow<DashboardUiState> = profileRepository.observeActiveProfileId()
        .filterNotNull()
        .flatMapLatest { profileId ->
            combine(appPreferences.activeBudgetMonth(profileId), selectedMonth) { storedMonth, selected ->
                val defaultMonth = storedMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() } ?: YearMonth.now()
                profileId to (selected ?: defaultMonth) to storedMonth
            }
        }
        .flatMapLatest { (selection, activeBudgetMonth) ->
            val (profileId, yearMonth) = selection
            combine(
                expenseRepository.observeCategoryTotals(profileId, yearMonth),
                budgetRepository.observeLimits(profileId),
                expenseRepository.observeExpensesForBudgetPeriod(profileId, yearMonth),
                expenseRepository.observeIncomeTotal(profileId, yearMonth),
            ) { totals, limits, monthExpenses, incomeTotal ->
                val cycleMonth = yearMonth.toString()
                val isActiveCycle = activeBudgetMonth == cycleMonth
                val isConfiguredForMonth = appPreferences.isBudgetMonthConfigured(profileId, cycleMonth)
                val baseBudget = if (isConfiguredForMonth) {
                    appPreferences.getCycleBaseBudget(profileId, cycleMonth) ?: 0L
                } else if (isActiveCycle) {
                    // Legacy migration: keep the existing active-cycle budget, but never
                    // reuse it for another month.
                    appPreferences.getCycleBaseBudget(profileId, cycleMonth)
                        ?: limits.firstOrNull { it.category == null }?.limitMinor
                        ?: 0L
                } else {
                    0L
                }
                val carryForward = appPreferences.getCarryForward(profileId, cycleMonth)
                val cycleLimits = limits.filter { it.category != null } + com.expensetracker.app.data.model.BudgetLimit(null, baseBudget)
                val summary = buildMonthlySummary(totals, cycleLimits)
                DashboardUiState(
                    yearMonth = yearMonth,
                    totalSpentMinor = summary.totalSpentMinor,
                    totalIncomeMinor = incomeTotal,
                    overallLimitMinor = baseBudget,
                    carryForwardMinor = carryForward,
                    categorySpends = summary.categorySpends.filter { it.spentMinor > 0 }.sortedByDescending { it.spentMinor },
                    recentExpenses = monthExpenses.filter { !it.isIncome }.take(5),
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    override fun onPreviousMonth() { selectedMonth.value = (selectedMonth.value ?: YearMonth.now()).minusMonths(1) }
    override fun onNextMonth() { selectedMonth.value = (selectedMonth.value ?: YearMonth.now()).plusMonths(1) }

    fun onRemainingClick() { _showBudgetEditor.value = true }
    fun onDismissBudgetEditor() { _showBudgetEditor.value = false }
    fun onSaveBudget(limitMinor: Long) {
        if (limitMinor <= 0L) return
        viewModelScope.launch {
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            when (val result = budgetRepository.setLimit(profileId, null, limitMinor)) {
                is BudgetSaveResult.Success -> {
                    val month = selectedMonth.value?.toString()
                        ?: appPreferences.getActiveBudgetMonth(profileId)
                        ?: YearMonth.now().toString()
                    appPreferences.setCycleBaseBudget(profileId, month, limitMinor)
                    appPreferences.markBudgetMonthConfigured(profileId, month)
                    _showBudgetEditor.value = false
                }
                is BudgetSaveResult.Error -> _effects.send(DashboardEffect.ShowError(result.message))
            }
        }
    }
    fun onClearBudget() {
        viewModelScope.launch {
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            when (val result = budgetRepository.clearLimit(profileId, null)) {
                is BudgetSaveResult.Success -> { appPreferences.clearCycleBaseBudget(profileId, selectedMonth.value?.toString() ?: appPreferences.getActiveBudgetMonth(profileId) ?: YearMonth.now().toString()); _showBudgetEditor.value = false }
                is BudgetSaveResult.Error -> _effects.send(DashboardEffect.ShowError(result.message))
            }
        }
    }
    override suspend fun onDeleteExpense(expense: Expense): Boolean = viewModelScope.async {
        try { expenseRepository.deleteExpense(expense); _effects.send(DashboardEffect.ShowUndoDelete(expense)); true }
        catch (_: SQLiteException) { _effects.send(DashboardEffect.ShowError("Couldn't delete \"${expense.displayLabel}\" — local storage error.")); false }
    }.await()
    override fun onUndoDelete(expense: Expense) {
        viewModelScope.launch {
            when (val result = expenseRepository.restoreExpense(expense)) {
                is AddExpenseResult.Success -> result.newAlerts.firstOrNull()?.let { _effects.send(DashboardEffect.ShowMessage(it.toSnackbarMessage())) }
                is AddExpenseResult.Error -> _effects.send(DashboardEffect.ShowError("Couldn't restore \"${expense.displayLabel}\": ${result.message}"))
            }
        }
    }
    private val Expense.displayLabel: String get() = note.ifBlank { category.displayName }
}
