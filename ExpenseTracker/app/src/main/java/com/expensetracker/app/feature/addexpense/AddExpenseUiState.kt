package com.expensetracker.app.feature.addexpense

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.data.model.ExpenseCategory
import java.time.LocalDate

/** @Immutable — holds a java.time.LocalDate field; see data/model/Expense.kt. */
@Immutable
data class AddExpenseUiState(
    // 1. Editable input
    val amountText: String = "",
    val selectedCategory: ExpenseCategory = ExpenseCategory.FOOD,
    val note: String = "",
    val date: LocalDate = LocalDate.now(),
    val fuelLitersText: String = "",
    val fuelPricePerLiterText: String = "",
    val fuelOdometerText: String = "",
    // 4. Transient UI-only
    val isEditMode: Boolean = false,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val amountError: String? = null,
) {
    // 2. Derived
    val canSave: Boolean
        get() = !isSaving && !isLoading && if (selectedCategory == ExpenseCategory.PETROL && !isEditMode) {
            (fuelLitersText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                (fuelPricePerLiterText.toDoubleOrNull() ?: 0.0) > 0.0 &&
                (fuelOdometerText.toDoubleOrNull() ?: -1.0) >= 0.0
        } else {
            (amountText.parseAmountToMinorUnits() ?: 0L) > 0L
        }
}

sealed interface AddExpenseEffect {
    data object NavigateBack : AddExpenseEffect
    data class ShowMessage(val message: String) : AddExpenseEffect
}

@Stable
interface AddExpenseActions {
    fun onAmountChange(value: String)
    fun onCategoryChange(category: ExpenseCategory)
    fun onNoteChange(value: String)
    fun onDateChange(date: LocalDate)
    fun onFuelLitersChange(value: String)
    fun onFuelPricePerLiterChange(value: String)
    fun onFuelOdometerChange(value: String)
    fun onSaveClick()
    fun onDismiss()
}
