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
import java.util.concurrent.ConcurrentHashMap

class AddExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    private val fuelRepository: com.expensetracker.app.data.repository.FuelRepository,
    private val expenseId: Long?,
) : ViewModel(), AddExpenseActions {

    private val _uiState = MutableStateFlow(AddExpenseUiState(isEditMode = expenseId != null, isLoading = expenseId != null))
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private val _effects = Channel<AddExpenseEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var loadedExpense: Expense? = null
    private var originalNoteForNotificationEdit: String? = null

    init {
        if (expenseId != null) {
            val clearNoteForNotificationEdit = consumeNotificationEdit(expenseId)
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
                if (clearNoteForNotificationEdit) {
                    originalNoteForNotificationEdit = expense.note
                }
                _uiState.update {
                    it.copy(
                        amountText = expense.amountMinor.toAmountInputText(),
                        selectedCategory = expense.category,
                        note = if (clearNoteForNotificationEdit) "" else expense.note,
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
        _uiState.update { state ->
            val amount = if (category == ExpenseCategory.PETROL) {
                val liters = state.fuelLitersText.toDoubleOrNull() ?: 0.0
                val price = state.fuelPricePerLiterText.toDoubleOrNull() ?: 0.0
                if (liters > 0 && price > 0) (liters * price).toString() else state.amountText
            } else state.amountText
            state.copy(selectedCategory = category, amountText = amount, amountError = null)
        }
    }

    override fun onFuelLitersChange(value: String) {
        _uiState.update { state ->
            val price = state.fuelPricePerLiterText.toDoubleOrNull() ?: 0.0
            val liters = value.toDoubleOrNull() ?: 0.0
            state.copy(
                fuelLitersText = value,
                amountText = if (state.selectedCategory == ExpenseCategory.PETROL && liters > 0 && price > 0) (liters * price).toString() else state.amountText,
            )
        }
    }

    override fun onFuelPricePerLiterChange(value: String) {
        _uiState.update { state ->
            val liters = state.fuelLitersText.toDoubleOrNull() ?: 0.0
            val price = value.toDoubleOrNull() ?: 0.0
            state.copy(
                fuelPricePerLiterText = value,
                amountText = if (state.selectedCategory == ExpenseCategory.PETROL && liters > 0 && price > 0) (liters * price).toString() else state.amountText,
            )
        }
    }

    override fun onFuelOdometerChange(value: String) {
        _uiState.update { it.copy(fuelOdometerText = value) }
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
        val amountMinor = if (state.selectedCategory == ExpenseCategory.PETROL) {
            val liters = state.fuelLitersText.toDoubleOrNull() ?: 0.0
            val price = state.fuelPricePerLiterText.toDoubleOrNull() ?: 0.0
            if (liters > 0 && price > 0) (liters * price * 100.0).toLong() else null
        } else {
            state.amountText.parseAmountToMinorUnits()
        }
        if (amountMinor == null || amountMinor <= 0L) {
            _uiState.update { it.copy(amountError = if (state.selectedCategory == ExpenseCategory.PETROL) "Enter liters and price per liter" else "Enter a valid amount") }
            return
        }

        if (state.selectedCategory == ExpenseCategory.PETROL && (state.fuelOdometerText.toDoubleOrNull() ?: -1.0) < 0.0) {
            _uiState.update { it.copy(amountError = "Enter the starting odometer") }
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val existing = loadedExpense
            val noteToSave = if (originalNoteForNotificationEdit != null && state.note.trim().isEmpty()) {
                originalNoteForNotificationEdit!!
            } else {
                state.note.trim()
            }
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            val result = if (existing != null) {
                expenseRepository.updateExpense(
                    existing.copy(
                        amountMinor = amountMinor,
                        category = state.selectedCategory,
                        note = noteToSave,
                        date = state.date,
                    ),
                )
            } else if (state.selectedCategory == ExpenseCategory.PETROL) {
                fuelRepository.addFuel(
                    profileId = profileId,
                    date = state.date,
                    liters = state.fuelLitersText.toDouble(),
                    pricePerLiter = state.fuelPricePerLiterText.toDouble(),
                    odometerKm = state.fuelOdometerText.toDouble(),
                    note = noteToSave,
                )
            } else {
                expenseRepository.addExpense(
                    profileId = profileId,
                    amountMinor = amountMinor,
                    category = state.selectedCategory,
                    note = noteToSave,
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

    companion object {
        private val notificationEditExpenseIds = ConcurrentHashMap.newKeySet<Long>()

        fun markNotificationEdit(expenseId: Long) {
            notificationEditExpenseIds.add(expenseId)
        }

        private fun consumeNotificationEdit(expenseId: Long): Boolean =
            notificationEditExpenseIds.remove(expenseId)
    }
}
