package com.expensetracker.app.feature.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
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

    override fun onDeleteExpense(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.deleteExpense(expense)
            _effects.send(TransactionsEffect.ShowUndoDelete(expense))
        }
    }

    /** Restores it to its original profile — not necessarily whichever one is active now. */
    override fun onUndoDelete(expense: Expense) {
        viewModelScope.launch {
            expenseRepository.addExpense(expense.profileId, expense.amountMinor, expense.category, expense.note, expense.date)
        }
    }
}
