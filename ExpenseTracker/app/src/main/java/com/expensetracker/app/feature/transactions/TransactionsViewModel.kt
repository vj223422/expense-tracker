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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModel(
    private val expenseRepository: ExpenseRepository,
    profileRepository: ProfileRepository,
) : ViewModel(), TransactionsActions {

    private val categoryFilter = MutableStateFlow<ExpenseCategory?>(null)

    private val _effects = Channel<TransactionsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    val uiState: StateFlow<TransactionsUiState> = profileRepository.observeActiveProfileId()
        .filterNotNull()
        .flatMapLatest { profileId ->
            combine(expenseRepository.observeAllExpenses(profileId), categoryFilter) { expenses, filter ->
                val filtered = if (filter == null) expenses else expenses.filter { it.category == filter }
                TransactionsUiState(
                    expensesByDate = filtered.groupBy { it.date }
                        .entries
                        .sortedByDescending { it.key }
                        .map { (date, group) -> DateGroup(date, group, group.sumOf { it.amountMinor }) },
                    selectedCategoryFilter = filter,
                    isLoading = false,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsUiState())

    override fun onFilterChange(category: ExpenseCategory?) {
        categoryFilter.value = category
    }

    // Launched in viewModelScope (survives the calling composable being disposed, e.g. by the
    // list item leaving composition) and only awaited by the caller, so a delete already in
    // flight always finishes even if the swipe item itself goes away first.
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

    /** Restores it to its original profile — not necessarily whichever one is active now. */
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
