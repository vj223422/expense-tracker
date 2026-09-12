package com.expensetracker.app.feature.scanpay

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.data.model.ExpenseCategory

sealed interface ScanPayStage {
    data object Ready : ScanPayStage
    data object LaunchingPayment : ScanPayStage
}

@Immutable
data class ScanPayUiState(
    val stage: ScanPayStage = ScanPayStage.Ready,
    val note: String = "",
    val selectedCategory: ExpenseCategory = ExpenseCategory.OTHER,
    val payeeVpa: String = "",
    val payeeName: String = "",
    val payeeMcc: String? = null,
    val payeeTransactionRef: String? = null,
) {
    val canPay: Boolean
        get() = stage is ScanPayStage.Ready && payeeVpa.isNotBlank()
}

sealed interface ScanPayEffect {
    data class LaunchUpiApp(
        val payeeVpa: String,
        val payeeName: String,
        val payeeMcc: String?,
        val transactionRef: String?,
        val transactionNote: String,
    ) : ScanPayEffect
    data class ShowMessage(val message: String) : ScanPayEffect
}

@Stable
interface ScanPayActions {
    fun onNoteChange(value: String)
    fun onCategoryChange(category: ExpenseCategory)
    fun onPayeeVpaChange(value: String)
    fun onPayeeNameChange(value: String)
    fun onPayClick()
    fun onPaymentActivityResult(resultCode: Int, responseExtra: String?)
}
