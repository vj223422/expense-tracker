package com.expensetracker.app.feature.transactions

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.notification.toSnackbarMessage
import com.expensetracker.app.data.repository.AddExpenseResult
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val expenseRepository: ExpenseRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), TransactionsActions {

    private val categoryFilter = MutableStateFlow<ExpenseCategory?>(null)
    private val searchQuery = MutableStateFlow("")
    private val selectedMonth = MutableStateFlow(YearMonth.now())

    private val _effects = Channel<TransactionsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: kotlinx.coroutines.flow.StateFlow<TransactionsUiState> = profileRepository.observeActiveProfileId()
        .filterNotNull()
        .flatMapLatest { profileId ->
            combine(expenseRepository.observeAllExpenses(profileId), categoryFilter, searchQuery, selectedMonth) { expenses, filter, query, month ->
                val normalizedQuery = query.trim().lowercase()
                val filtered = expenses.filter { expense ->
                    val matchesMonth = YearMonth.from(expense.date) == month
                    val matchesCategory = filter == null || expense.category == filter
                    val matchesSearch = normalizedQuery.isEmpty() || listOf(
                        expense.note,
                        expense.category.displayName,
                        expense.amountMinor.toString(),
                        String.format(java.util.Locale.US, "%.2f", expense.amountMinor / 100.0),
                    ).any { it.lowercase().contains(normalizedQuery) }
                    matchesMonth && matchesCategory && matchesSearch
                }
                TransactionsUiState(
                    expensesByDate = filtered.groupBy { it.date }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (date, group) -> DateGroup(date, group, group.sumOf { it.amountMinor }) },
                    selectedCategoryFilter = filter,
                    searchQuery = query,
                    selectedMonth = month,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    override fun onFilterChange(category: ExpenseCategory?) {
        categoryFilter.value = category
    }

    override fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    override fun onMonthChange(month: YearMonth) {
        selectedMonth.value = month
    }

    override fun onPreviousMonth() {
        selectedMonth.update { it.minusMonths(1) }
    }

    override fun onNextMonth() {
        selectedMonth.update { it.plusMonths(1) }
    }

    override suspend fun onDeleteExpense(expense: Expense): Boolean = viewModelScope.async {
        try {
            expenseRepository.deleteExpense(expense)
            _effects.send(TransactionsEffect.ShowUndoDelete(expense))
            true
        } catch (e: SQLiteException) {
            _effects.send(TransactionsEffect.ShowError("Couldn't delete \"${expense.displayLabel}\" — local storage error."))
            false
        }
    }.await()

    override fun onUndoDelete(expense: Expense) {
        viewModelScope.launch {
            when (val result = expenseRepository.restoreExpense(expense)) {
                is AddExpenseResult.Success -> {
                    result.newAlerts.firstOrNull()?.let { alert ->
                        _effects.send(TransactionsEffect.ShowMessage(alert.toSnackbarMessage()))
                    }
                }
                is AddExpenseResult.Error -> {
                    _effects.send(TransactionsEffect.ShowError("Couldn't restore \"${expense.displayLabel}\": ${result.message}"))
                }
            }
        }
    }

    private val Expense.displayLabel: String get() = note.ifBlank { category.displayName }
}
