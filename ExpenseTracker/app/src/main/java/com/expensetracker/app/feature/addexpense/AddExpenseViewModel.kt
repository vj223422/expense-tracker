package com.expensetracker.app.feature.addexpense

import android.database.sqlite.SQLiteException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.core.util.toAmountInputText
import com.expensetracker.app.data.model.Expense
import com.expensetracker.app.data.model.ExpenseCategory
import com.expensetracker.app.data.notification.toSnackbarMessage
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
    private val expenseId: Long?,
) : ViewModel(), AddExpenseActions {

    private val _uiState = MutableStateFlow(AddExpenseUiState(isEditMode = expenseId != null, isLoading = expenseId != null))
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddExpenseEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadedExpense: Expense? = null

    init {
        if (expenseId != null) {
            viewModelScope.launch {
                val expense = try {
                    val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
                    expenseRepository.getExpenseById(expenseId, profileId)
                } catch (e: SQLiteException) {
                    null
                }
                if (expense == null) {
                    _effects.send(AddExpenseEffect.ShowMessage("Expense not found"))
                    _effects.send(AddExpenseEffect.NavigateBack)
                    return@launch
                }
                loadedExpense = expense
                _uiState.update {
                    it.copy(
                        amountText = expense.amountMinor.toAmountInputText(),
                        selectedCategory = expense.category,
                        note = expense.note,
                        date = expense.date,
                        isLoading = false,
                    )
                }
            }
        }
    }

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

    /** Saves an expense after the amount has been supplied by the caller, such as UPI result handling. */
    fun savePaidAmount(
        amountMinor: Long,
        transactionId: String?,
        paymentMethod: String = "UPI",
    ) {
        if (loadedExpense != null || _uiState.value.isSaving || amountMinor <= 0L) return
        _uiState.update { it.copy(amountText = amountMinor.toAmountInputText(), isSaving = true, amountError = null) }
        viewModelScope.launch {
            val state = _uiState.value
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            val note = buildString {
                state.note.trim().takeIf { it.isNotEmpty() }?.let { append(it) }
                transactionId?.takeIf { it.isNotBlank() }?.let {
                    if (isNotEmpty()) append(" ")
                    append("[$paymentMethod txn: ").append(it).append("]")
                }
            }
            when (val result = expenseRepository.addExpense(
                profileId = profileId,
                amountMinor = amountMinor,
                category = state.selectedCategory,
                note = note,
                date = state.date,
            )) {
                is AddExpenseResult.Success -> {
                    val message = result.newAlerts.firstOrNull()?.let { "Expense added. ${it.toSnackbarMessage()}" } ?: "Expense added"
                    _effects.send(AddExpenseEffect.ShowMessage(message))
                    _effects.send(AddExpenseEffect.NavigateBack)
                }
                is AddExpenseResult.Error -> {
                    _uiState.update { it.copy(isSaving = false) }
                    _effects.send(AddExpenseEffect.ShowMessage(result.message))
                }
            }
        }
    }

    override fun onSaveClick() {
        val state = _uiState.value
        if (state.isSaving) return
        val amountMinor = state.amountText.parseAmountToMinorUnits()
        if (amountMinor == null || amountMinor <= 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val existing = loadedExpense
            val result = if (existing != null) {
                expenseRepository.updateExpense(
                    existing.copy(
                        amountMinor = amountMinor,
                        category = state.selectedCategory,
                        note = state.note.trim(),
                        date = state.date,
                    ),
                )
            } else {
                val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
                expenseRepository.addExpense(
                    profileId = profileId,
                    amountMinor = amountMinor,
                    category = state.selectedCategory,
                    note = state.note.trim(),
                    date = state.date,
                )
            }
            when (result) {
                is AddExpenseResult.Success -> {
                    val baseMessage = if (existing != null) "Expense updated" else "Expense added"
                    val message = result.newAlerts.firstOrNull()?.let { "$baseMessage. ${it.toSnackbarMessage()}" } ?: baseMessage
                    _effects.send(AddExpenseEffect.ShowMessage(message))
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
