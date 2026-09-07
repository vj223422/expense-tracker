package com.expensetracker.app.feature.addexpense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.repository.AddExpenseResult
import com.expensetracker.app.data.repository.ExpenseRepository
import com.expensetracker.app.data.repository.ProfileRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

class AddExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
) : ViewModel(), AddExpenseActions {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddExpenseEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    override fun onAmountChange(value: String) {
        _uiState.update { it.copy(amountText = value, amountError = null) }
    }

    override fun onCategoryChange(category: ExpenseCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    override fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    override fun onDateChange(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    override fun onDismiss() {
        viewModelScope.launch { _effects.send(AddExpenseEffect.NavigateBack) }
    }

    override fun onSaveClick() {
        val state = _uiState.value
        val amountMinor = state.amountText.parseAmountToMinorUnits()
        if (amountMinor == null || amountMinor <= 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            when (
                val result = expenseRepository.addExpense(
                    profileId = profileId,
                    amountMinor = amountMinor,
                    category = state.selectedCategory,
                    note = state.note.trim(),
                    date = state.date,
                )
            ) {
                is AddExpenseResult.Success -> {
                    _effects.send(AddExpenseEffect.ShowMessage("Expense added"))
                    _effects.send(AddExpenseEffect.NavigateBack)
                }
                is AddExpenseResult.Error -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(AddExpenseEffect.ShowMessage(result.message))
                }
            }
        }
    }
}
