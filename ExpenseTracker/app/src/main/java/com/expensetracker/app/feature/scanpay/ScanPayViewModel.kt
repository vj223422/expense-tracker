package com.expensetracker.app.feature.scanpay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.core.util.UpiPayee
import com.expensetracker.app.core.util.UpiPaymentOutcome
import com.expensetracker.app.core.util.buildUpiPaymentUri
import com.expensetracker.app.core.util.parseAmountToMinorUnits
import com.expensetracker.app.core.util.parseUpiQr
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

private const val KEY_PENDING_VPA = "scanpay_pending_vpa"
private const val KEY_PENDING_PAYEE_NAME = "scanpay_pending_payee_name"
private const val KEY_PENDING_AMOUNT_TEXT = "scanpay_pending_amount_text"
private const val KEY_PENDING_NOTE = "scanpay_pending_note"
private const val KEY_PENDING_PROFILE_ID = "scanpay_pending_profile_id"

class ScanPayViewModel(
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), ScanPayActions {

    // Restores a payment that was mid-flight in the external UPI app when this process died —
    // entering a UPI PIN takes long enough for Android to reclaim a backgrounded low-memory
    // process, and without this the completed payment's result would arrive to a freshly-reset
    // ViewModel with no LaunchingPayment stage to match it against, silently never getting logged.
    private val _uiState = MutableStateFlow(restorePendingLaunch())
    val uiState: StateFlow<ScanPayUiState> = _uiState.asStateFlow()

    private val _effects = Channel<ScanPayEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var paymentProfileId: Long?
        get() = savedStateHandle[KEY_PENDING_PROFILE_ID]
        set(value) { savedStateHandle[KEY_PENDING_PROFILE_ID] = value }

    /** Suppresses repeat "not a valid UPI code" snackbars while the camera keeps re-detecting the
     * same unsupported QR code across consecutive frames. */
    private var lastInvalidQrValue: String? = null

    private fun restorePendingLaunch(): ScanPayUiState {
        val vpa = savedStateHandle.get<String>(KEY_PENDING_VPA) ?: return ScanPayUiState()
        val payee = UpiPayee(
            vpa = vpa,
            payeeName = savedStateHandle[KEY_PENDING_PAYEE_NAME],
            suggestedAmount = null,
        )
        return ScanPayUiState(
            stage = ScanPayStage.LaunchingPayment(payee),
            amountText = savedStateHandle[KEY_PENDING_AMOUNT_TEXT] ?: "",
            note = savedStateHandle[KEY_PENDING_NOTE] ?: "",
        )
    }

    private fun clearPendingLaunch() {
        savedStateHandle[KEY_PENDING_VPA] = null
        savedStateHandle[KEY_PENDING_PAYEE_NAME] = null
        savedStateHandle[KEY_PENDING_AMOUNT_TEXT] = null
        savedStateHandle[KEY_PENDING_NOTE] = null
        paymentProfileId = null
    }

    override fun onQrDetected(rawValue: String) {
        if (_uiState.value.stage !is ScanPayStage.Scanning) return
        val payee = parseUpiQr(rawValue)
        if (payee == null) {
            if (rawValue != lastInvalidQrValue) {
                lastInvalidQrValue = rawValue
                viewModelScope.launch { _effects.send(ScanPayEffect.ShowMessage("That QR code isn't a UPI payment code")) }
            }
            return
        }
        lastInvalidQrValue = null
        _uiState.update {
            it.copy(
                stage = ScanPayStage.Confirming(payee),
                amountText = payee.suggestedAmount.orEmpty(),
                note = payee.payeeName.orEmpty(),
                amountError = null,
                amountPrefilledFromQr = !payee.suggestedAmount.isNullOrBlank(),
            )
        }
    }

    override fun onAmountChange(value: String) {
        // Once the user touches the amount themselves, it's no longer "trust the QR" territory.
        _uiState.update { it.copy(amountText = value, amountError = null, amountPrefilledFromQr = false) }
    }

    override fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    override fun onPayClick() {
        val state = _uiState.value
        val payee = (state.stage as? ScanPayStage.Confirming)?.payee ?: return
        val amountMinor = state.amountText.parseAmountToMinorUnits()
        if (amountMinor == null || amountMinor <= 0L) {
            _uiState.update { it.copy(amountError = "Enter a valid amount") }
            return
        }

        val uri = buildUpiPaymentUri(payee, amountMinor, state.note.trim())
        savedStateHandle[KEY_PENDING_VPA] = payee.vpa
        savedStateHandle[KEY_PENDING_PAYEE_NAME] = payee.payeeName
        savedStateHandle[KEY_PENDING_AMOUNT_TEXT] = state.amountText
        savedStateHandle[KEY_PENDING_NOTE] = state.note
        _uiState.update { it.copy(stage = ScanPayStage.LaunchingPayment(payee)) }
        viewModelScope.launch {
            // Captured now, at the moment the user commits to paying — not after the external UPI
            // app returns — so switching the active profile while that app is in the foreground
            // can't cause the resulting expense to be logged against the wrong profile.
            paymentProfileId = profileRepository.observeActiveProfileId().filterNotNull().first()
            _effects.send(ScanPayEffect.LaunchUpiApp(uri))
        }
    }

    override fun onCancelConfirm() {
        if (_uiState.value.stage is ScanPayStage.Confirming) {
            _uiState.value = ScanPayUiState()
        }
    }

    override fun onPaymentActivityResult(resultCode: Int, responseExtra: String?) {
        val state = _uiState.value
        val payee = (state.stage as? ScanPayStage.LaunchingPayment)?.payee ?: return

        when (val outcome = parseUpiResponse(resultCode, responseExtra)) {
            is UpiPaymentOutcome.Success -> saveExpense(state, payee, txnRef = outcome.txnRef, pending = false)
            is UpiPaymentOutcome.Submitted -> saveExpense(state, payee, txnRef = outcome.txnRef, pending = true)
            is UpiPaymentOutcome.Cancelled -> {
                clearPendingLaunch()
                _uiState.update { it.copy(stage = ScanPayStage.Confirming(payee)) }
                viewModelScope.launch { _effects.send(ScanPayEffect.ShowMessage("Payment cancelled")) }
            }
            is UpiPaymentOutcome.Failed -> {
                clearPendingLaunch()
                _uiState.update { it.copy(stage = ScanPayStage.Confirming(payee)) }
                viewModelScope.launch {
                    _effects.send(ScanPayEffect.ShowMessage(outcome.reason?.let { "Payment failed: $it" } ?: "Payment failed"))
                }
            }
        }
    }

    /**
     * Payment already went through in the UPI app by this point, so a save failure here is only
     * ever logged/reported — it never re-triggers payment. [txnRef] is appended to the note (when
     * the UPI app supplied one) purely as an audit trail: it doesn't verify anything itself, but
     * gives the user something concrete to cross-check against their bank statement if this entry
     * ever looks wrong. [pending] marks a `SUBMITTED` outcome, whose final status UPI doesn't
     * guarantee at this point, so the record is worded as provisional rather than confirmed.
     */
    private fun saveExpense(state: ScanPayUiState, payee: UpiPayee, txnRef: String?, pending: Boolean) {
        val amountMinor = state.amountText.parseAmountToMinorUnits() ?: 0L
        val baseNote = state.note.trim().ifEmpty { payee.payeeName ?: payee.vpa }
        val note = buildString {
            append(baseNote)
            if (pending) append(" (payment submitted — verify)")
            if (txnRef != null) append(" [UPI ref: $txnRef]")
        }
        viewModelScope.launch {
            val profileId = paymentProfileId ?: profileRepository.observeActiveProfileId().filterNotNull().first()
            clearPendingLaunch()
            val result = expenseRepository.addExpense(
                profileId = profileId,
                amountMinor = amountMinor,
                category = ExpenseCategory.OTHER,
                note = note,
                date = LocalDate.now(),
            )
            _uiState.value = ScanPayUiState()
            when (result) {
                is AddExpenseResult.Success -> {
                    val baseMessage = if (pending) "Expense logged — payment submitted, verify it completed" else "Expense added"
                    val message = result.newAlerts.firstOrNull()?.let { "$baseMessage. ${it.toSnackbarMessage()}" } ?: baseMessage
                    _effects.send(ScanPayEffect.ShowMessage(message))
                }
                is AddExpenseResult.Error -> _effects.send(
                    ScanPayEffect.ShowMessage("Payment succeeded but couldn't be logged: ${result.message}"),
                )
            }
        }
    }
}
