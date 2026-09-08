package com.expensetracker.app.feature.dashboard

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.buildMonthlySummary
import com.expensetracker.app.data.notification.toSnackbarMessage
import com.expensetracker.app.data.repository.AddExpenseResult
import com.expensetracker.app.data.repository.BudgetRepository
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val expenseRepository: ExpenseRepository,
    budgetRepository: BudgetRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), DashboardActions {

    private val _effects = Channel<DashboardEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    // Re-checked every minute rather than captured once — a screen left open across midnight on
    // month-end must not keep showing the previous month's totals. distinctUntilChanged means the
    // downstream flatMapLatest only actually restarts on a real month rollover, not every tick.
    private val currentYearMonth = flow {
        while (true) {
            emit(YearMonth.now())
            delay(60_000L)
        }
    }.distinctUntilChanged()

    val uiState: StateFlow<DashboardUiState> = profileRepository.observeActiveProfileId()
        .filterNotNull()
        .combine(currentYearMonth) { profileId, yearMonth -> profileId to yearMonth }
        .flatMapLatest { (profileId, yearMonth) ->
            combine(
                expenseRepository.observeCategoryTotals(profileId, yearMonth),
                budgetRepository.observeLimits(profileId),
                expenseRepository.observeExpensesForMonth(profileId, yearMonth),
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
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())

    // Launched in viewModelScope (survives the calling composable being disposed, e.g. by the
    // list item leaving composition) and only awaited by the caller, so a delete already in
    // flight always finishes even if the swipe item itself goes away first.
    override suspend fun onDeleteExpense(expense: Expense): Boolean = viewModelScope.async {
        try {
            expenseRepository.deleteExpense(expense)
            _effects.send(DashboardEffect.ShowUndoDelete(expense))
            true
        } catch (e: SQLiteException) {
            _effects.send(DashboardEffect.ShowError("Couldn't delete \"${expense.displayLabel}\" — local storage error."))
            false
        }
    }.await()

    /** Restores it to its original profile — not necessarily whichever one is active now. */
    override fun onUndoDelete(expense: Expense) {
        viewModelScope.launch {
            when (val result = expenseRepository.restoreExpense(expense)) {
                is AddExpenseResult.Success -> {
                    result.newAlerts.firstOrNull()?.let { alert ->
                        _effects.send(DashboardEffect.ShowMessage(alert.toSnackbarMessage()))
                    }
                }
                is AddExpenseResult.Error -> {
                    _effects.send(DashboardEffect.ShowError("Couldn't restore \"${expense.displayLabel}\": ${result.message}"))
                }
            }
        }
    }

    private val Expense.displayLabel: String get() = note.ifBlank { category.displayName }
}
