package com.expensetracker.app.feature.scanpay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.util.UpiPaymentOutcome
import com.expensetracker.app.core.util.parseUpiResponse
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
import java.util.UUID

private const val KEY_PENDING_SESSION_ID = "scanpay_pending_session_id"
private const val KEY_PENDING_NOTE = "scanpay_pending_note"
private const val KEY_PENDING_CATEGORY = "scanpay_pending_category"
private const val KEY_PENDING_PROFILE_ID = "scanpay_pending_profile_id"
private const val KEY_HANDLED_SESSION_ID = "scanpay_handled_session_id"

class ScanPayViewModel(
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), ScanPayActions {

    private val _uiState = MutableStateFlow(restorePendingPayment())
    val uiState: StateFlow<ScanPayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScanPayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var paymentProfileId: Long?
        get() = savedStateHandle[KEY_PENDING_PROFILE_ID]
        set(value) { savedStateHandle[KEY_PENDING_PROFILE_ID] = value }

    private fun restorePendingPayment(): ScanPayUiState {
        val sessionId = savedStateHandle.get<String>(KEY_PENDING_SESSION_ID)
            ?: return ScanPayUiState()

        return ScanPayUiState(
            stage = ScanPayStage.LaunchingPayment,
            note = savedStateHandle[KEY_PENDING_NOTE] ?: "",
            selectedCategory = savedStateHandle.get<String>(KEY_PENDING_CATEGORY)
                ?.let { name -> ExpenseCategory.entries.firstOrNull { it.name == name } }
                ?: ExpenseCategory.OTHER,
        ).also {
            if (savedStateHandle.get<String>(KEY_HANDLED_SESSION_ID) == sessionId) {
                clearPendingPayment()
            }
        }
    }

    private fun clearPendingPayment() {
        savedStateHandle[KEY_PENDING_SESSION_ID] = null
        savedStateHandle[KEY_PENDING_NOTE] = null
        savedStateHandle[KEY_PENDING_CATEGORY] = null
        paymentProfileId = null
    }

    override fun onNoteChange(value: String) {
        if (_uiState.value.stage is ScanPayStage.LaunchingPayment) return
        _uiState.update { it.copy(note = value) }
    }

    override fun onCategoryChange(category: ExpenseCategory) {
        if (_uiState.value.stage is ScanPayStage.LaunchingPayment) return
        _uiState.update { it.copy(selectedCategory = category) }
    }

    override fun onPayClick() {
        val state = _uiState.value
        if (state.stage is ScanPayStage.LaunchingPayment) return

        viewModelScope.launch {
            val profileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            val sessionId = UUID.randomUUID().toString()

            savedStateHandle[KEY_PENDING_SESSION_ID] = sessionId
            savedStateHandle[KEY_PENDING_NOTE] = state.note
            savedStateHandle[KEY_PENDING_CATEGORY] = state.selectedCategory.name
            paymentProfileId = profileId

            _uiState.update { it.copy(stage = ScanPayStage.LaunchingPayment) }
            _effects.send(ScanPayEffect.LaunchUpiApp)
        }
    }

    override fun onPaymentActivityResult(
        resultCode: Int,
        responseExtra: String?,
    ) {
        val state = _uiState.value
        if (state.stage !is ScanPayStage.LaunchingPayment) return

        val sessionId = savedStateHandle.get<String>(KEY_PENDING_SESSION_ID)
            ?: return
        if (savedStateHandle.get<String>(KEY_HANDLED_SESSION_ID) == sessionId) return

        savedStateHandle[KEY_HANDLED_SESSION_ID] = sessionId

        when (val outcome = parseUpiResponse(resultCode, responseExtra)) {
            is UpiPaymentOutcome.Success -> {
                if (outcome.amountMinor == null || outcome.amountMinor <= 0L) {
                    // The payment app reported success but did not return an amount. The user
                    // explicitly asked to keep the successful payment visible as an expense so
                    // it can be corrected manually rather than silently losing the transaction.
                    saveExpense(
                        state = state,
                        amountMinor = 0L,
                        missingAmount = true,
                        outcome = outcome,
                    )
                } else {
                    saveExpense(
                        state = state,
                        amountMinor = outcome.amountMinor,
                        missingAmount = false,
                        outcome = outcome,
                    )
                }
            }
            is UpiPaymentOutcome.Submitted -> {
                finishWithoutExpense(
                    "Payment submitted or pending. No expense was added until payment is confirmed.",
                )
            }
            is UpiPaymentOutcome.Cancelled -> {
                finishWithoutExpense("Payment cancelled", restoreContext = true)
            }
            is UpiPaymentOutcome.Failed -> {
                finishWithoutExpense(
                    outcome.reason?.let { "Payment failed: $it" } ?: "Payment failed",
                    restoreContext = true,
                )
            }
        }
    }

    private fun saveExpense(
        state: ScanPayUiState,
        amountMinor: Long,
        missingAmount: Boolean,
        outcome: UpiPaymentOutcome.Success,
    ) {
        viewModelScope.launch {
            val profileId = paymentProfileId
                ?: profileRepository.observeActiveProfileId().filterNotNull().first()

            val result = expenseRepository.addExpense(
                profileId = profileId,
                amountMinor = amountMinor,
                category = state.selectedCategory,
                note = state.note.trim(),
                date = LocalDate.now(),
            )

            clearPendingPayment()
            _uiState.value = ScanPayUiState()

            val transactionSummary = buildString {
                outcome.txnRef?.let { append(" UPI ref: $it.") }
                outcome.txnId?.let { append(" Txn ID: $it.") }
            }.trim()

            when (result) {
                is AddExpenseResult.Success -> {
                    val message = when {
                        missingAmount ->
                            "Expense added with amount ₹0. Please edit this expense and enter the amount paid."
                        transactionSummary.isBlank() ->
                            "Expense added"
                        else ->
                            "Expense added.$transactionSummary"
                    }
                    val withAlert = result.newAlerts.firstOrNull()?.let {
                        "$message ${it.toSnackbarMessage()}"
                    } ?: message
                    _effects.send(ScanPayEffect.ShowMessage(withAlert))
                }
                is AddExpenseResult.Error -> {
                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            if (missingAmount) {
                                "Payment succeeded but couldn't be logged: ${result.message}"
                            } else {
                                "Payment succeeded but couldn't be logged: ${result.message}"
                            },
                        ),
                    )
                }
            }
        }
    }

    private fun finishWithoutExpense(
        message: String,
        restoreContext: Boolean = false,
    ) {
        val state = _uiState.value
        clearPendingPayment()
        _uiState.value = if (restoreContext) {
            ScanPayUiState(
                stage = ScanPayStage.Ready,
                note = state.note,
                selectedCategory = state.selectedCategory,
            )
        } else {
            ScanPayUiState()
        }
        viewModelScope.launch { _effects.send(ScanPayEffect.ShowMessage(message)) }
    }
}