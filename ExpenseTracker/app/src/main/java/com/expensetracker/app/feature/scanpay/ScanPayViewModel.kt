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

private const val KEY_PENDING_VPA =
    "scanpay_pending_vpa"

private const val KEY_PENDING_PAYEE_NAME =
    "scanpay_pending_payee_name"

private const val KEY_PENDING_AMOUNT_TEXT =
    "scanpay_pending_amount_text"

private const val KEY_PENDING_NOTE =
    "scanpay_pending_note"

private const val KEY_PENDING_CATEGORY =
    "scanpay_pending_category"

private const val KEY_PENDING_PROFILE_ID =
    "scanpay_pending_profile_id"

private const val KEY_PENDING_ORIGINAL_URI =
    "scanpay_pending_original_uri"

class ScanPayViewModel(
    private val expenseRepository: ExpenseRepository,
    private val profileRepository: ProfileRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel(), ScanPayActions {

    /*
     * Restores a payment that was mid-flight in the external UPI app
     * when Android killed the process.
     */
    private val _uiState = MutableStateFlow(
        restorePendingLaunch(),
    )

    val uiState: StateFlow<ScanPayUiState> =
        _uiState.asStateFlow()

    private val _effects =
        Channel<ScanPayEffect>(Channel.BUFFERED)

    val effects =
        _effects.receiveAsFlow()

    private var paymentProfileId: Long?
        get() = savedStateHandle[KEY_PENDING_PROFILE_ID]
        set(value) {
            savedStateHandle[KEY_PENDING_PROFILE_ID] = value
        }

    /**
     * Suppresses repeated invalid QR messages while the camera
     * continues detecting the same QR.
     */
    private var lastInvalidQrValue: String? = null

    private fun restorePendingLaunch(): ScanPayUiState {

        val vpa =
            savedStateHandle.get<String>(
                KEY_PENDING_VPA,
            ) ?: return ScanPayUiState()

        /*
         * If the original QR URI is still available, parse it again.
         *
         * This is important for dynamic merchant QR codes because
         * merely restoring pa/pn would lose the transaction-bound
         * information.
         */
        val originalUri =
            savedStateHandle.get<String>(
                KEY_PENDING_ORIGINAL_URI,
            )

        val restoredPayee =
            originalUri
                ?.let { parseUpiQr(it) }

        val payee =
            restoredPayee
                ?: UpiPayee(
                    vpa = vpa,
                    payeeName = savedStateHandle[
                        KEY_PENDING_PAYEE_NAME
                    ],
                    suggestedAmount = null,
                )

        return ScanPayUiState(
            stage = ScanPayStage.LaunchingPayment(
                payee,
            ),
            amountText =
                savedStateHandle[
                    KEY_PENDING_AMOUNT_TEXT
                ] ?: "",
            note =
                savedStateHandle[
                    KEY_PENDING_NOTE
                ] ?: "",
            selectedCategory =
                savedStateHandle
                    .get<String>(KEY_PENDING_CATEGORY)
                    ?.let { categoryName ->
                        ExpenseCategory.entries.firstOrNull {
                            it.name == categoryName
                        }
                    }
                    ?: ExpenseCategory.OTHER,
        )
    }

    private fun clearPendingLaunch() {

        savedStateHandle[
            KEY_PENDING_VPA
        ] = null

        savedStateHandle[
            KEY_PENDING_PAYEE_NAME
        ] = null

        savedStateHandle[
            KEY_PENDING_AMOUNT_TEXT
        ] = null

        savedStateHandle[
            KEY_PENDING_NOTE
        ] = null

        savedStateHandle[
            KEY_PENDING_CATEGORY
        ] = null

        savedStateHandle[
            KEY_PENDING_ORIGINAL_URI
        ] = null

        paymentProfileId = null
    }

    override fun onQrDetected(
        rawValue: String,
    ) {

        if (
            _uiState.value.stage
                !is ScanPayStage.Scanning
        ) {
            return
        }

        val payee =
            parseUpiQr(rawValue)

        if (payee == null) {

            if (
                rawValue != lastInvalidQrValue
            ) {

                lastInvalidQrValue =
                    rawValue

                viewModelScope.launch {
                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            "That QR code isn't a UPI payment code",
                        ),
                    )
                }
            }

            return
        }

        lastInvalidQrValue = null

        _uiState.update { currentState ->

            currentState.copy(

                stage =
                    ScanPayStage.Confirming(
                        payee,
                    ),

                /*
                 * Dynamic QR:
                 * use the merchant-supplied amount.
                 *
                 * Static/P2P:
                 * use the supplied amount if one exists,
                 * otherwise leave it empty so the user can enter it.
                 */
                amountText =
                    payee.suggestedAmount.orEmpty(),

                /*
                 * A note is optional and must be user-provided.  Do not
                 * manufacture a `tn` parameter from the QR display name.
                 */
                note = "",

                selectedCategory = ExpenseCategory.OTHER,

                amountError = null,

                amountPrefilledFromQr =
                    !payee.suggestedAmount.isNullOrBlank(),
            )
        }
    }

    override fun onQrImageReadFailed() {

        if (
            _uiState.value.stage
                !is ScanPayStage.Scanning
        ) {
            return
        }

        viewModelScope.launch {
            _effects.send(
                ScanPayEffect.ShowMessage(
                    "Couldn't read a UPI QR code from that image",
                ),
            )
        }
    }

    override fun onAmountChange(
        value: String,
    ) {

        /*
         * A dynamic/signed merchant QR owns the amount.
         *
         * Do not allow the app to modify it.
         */
        val payee =
            (
                _uiState.value.stage
                    as? ScanPayStage.Confirming
                )?.payee

        if (payee?.isDynamic == true) {
            return
        }

        _uiState.update {
            it.copy(
                amountText = value,
                amountError = null,
                amountPrefilledFromQr = false,
            )
        }
    }

    override fun onNoteChange(
        value: String,
    ) {

        /*
         * Dynamic QR is launched exactly as scanned.
         *
         * A note added here would not be inserted into the
         * merchant-generated URI anyway, so keep the UI locked.
         */
        val payee =
            (
                _uiState.value.stage
                    as? ScanPayStage.Confirming
                )?.payee

        if (payee?.isDynamic == true) {
            return
        }

        _uiState.update {
            it.copy(
                note = value,
            )
        }
    }

    override fun onCategoryChange(
        category: ExpenseCategory,
    ) {

        _uiState.update {
            it.copy(
                selectedCategory = category,
            )
        }
    }

    override fun onPayClick() {

        val state =
            _uiState.value

        val payee =
            (
                state.stage
                    as? ScanPayStage.Confirming
            )?.payee ?: return

        val amountMinor =
            state.amountText
                .parseAmountToMinorUnits()

        if (
            amountMinor == null ||
            amountMinor <= 0L
        ) {

            _uiState.update {
                it.copy(
                    amountError =
                        "Enter a valid amount",
                )
            }

            return
        }

        /*
         * For dynamic QR this function returns the exact original
         * URI instead of rebuilding it.
         *
         * For P2P/static QR it creates a clean UPI payment URI.
         */
        val uri =
            buildUpiPaymentUri(
                payee = payee,
                amount = state.amountText.trim(),
                note = state.note.trim(),
            )

        savedStateHandle[
            KEY_PENDING_VPA
        ] = payee.vpa

        savedStateHandle[
            KEY_PENDING_PAYEE_NAME
        ] = payee.payeeName

        savedStateHandle[
            KEY_PENDING_AMOUNT_TEXT
        ] = state.amountText

        savedStateHandle[
            KEY_PENDING_NOTE
        ] = state.note

        savedStateHandle[
            KEY_PENDING_CATEGORY
        ] = state.selectedCategory.name

        /*
         * Preserve the exact QR URI so a process restart can
         * restore a dynamic merchant payment correctly.
         */
        savedStateHandle[
            KEY_PENDING_ORIGINAL_URI
        ] = payee.originalUri

        _uiState.update {
            it.copy(
                stage =
                    ScanPayStage.LaunchingPayment(
                        payee,
                    ),
            )
        }

        viewModelScope.launch {

            /*
             * Capture the active profile before launching
             * the external UPI application.
             */
            paymentProfileId =
                profileRepository
                    .observeActiveProfileId()
                    .filterNotNull()
                    .first()

            _effects.send(
                ScanPayEffect.LaunchUpiApp(
                    uri,
                ),
            )
        }
    }

    override fun onCancelConfirm() {

        if (
            _uiState.value.stage
                is ScanPayStage.Confirming
        ) {
            _uiState.value =
                ScanPayUiState()
        }
    }

    override fun onPaymentActivityResult(
        resultCode: Int,
        responseExtra: String?,
    ) {

        val state =
            _uiState.value

        val payee =
            (
                state.stage
                    as? ScanPayStage.LaunchingPayment
            )?.payee ?: return

        when (
            val outcome =
                parseUpiResponse(
                    resultCode,
                    responseExtra,
                )
        ) {

            is UpiPaymentOutcome.Success -> {

                saveExpense(
                    state = state,
                    payee = payee,
                    txnRef = outcome.txnRef,
                    pending = false,
                )
            }

            is UpiPaymentOutcome.Submitted -> {

                saveExpense(
                    state = state,
                    payee = payee,
                    txnRef = outcome.txnRef,
                    pending = true,
                )
            }

            is UpiPaymentOutcome.Cancelled -> {

                clearPendingLaunch()

                _uiState.update {
                    it.copy(
                        stage =
                            ScanPayStage.Confirming(
                                payee,
                            ),
                    )
                }

                viewModelScope.launch {
                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            "Payment cancelled",
                        ),
                    )
                }
            }

            is UpiPaymentOutcome.Failed -> {

                clearPendingLaunch()

                _uiState.update {
                    it.copy(
                        stage =
                            ScanPayStage.Confirming(
                                payee,
                            ),
                    )
                }

                viewModelScope.launch {

                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            outcome.reason?.let {
                                "Payment failed: $it"
                            } ?: "Payment failed",
                        ),
                    )
                }
            }
        }
    }

    /**
     * Saves the expense after the external UPI app returns.
     */
    private fun saveExpense(
        state: ScanPayUiState,
        payee: UpiPayee,
        txnRef: String?,
        pending: Boolean,
    ) {

        val amountMinor =
            state.amountText
                .parseAmountToMinorUnits()
                ?: 0L

        val baseNote =
            state.note
                .trim()
                .ifEmpty {
                    payee.payeeName
                        ?: payee.vpa
                }

        val note =
            buildString {

                append(baseNote)

                if (pending) {
                    append(
                        " (payment submitted — verify)",
                    )
                }

                if (txnRef != null) {
                    append(
                        " [UPI ref: $txnRef]",
                    )
                }
            }

        viewModelScope.launch {

            val profileId =
                paymentProfileId
                    ?: profileRepository
                        .observeActiveProfileId()
                        .filterNotNull()
                        .first()

            clearPendingLaunch()

            val result =
                expenseRepository.addExpense(

                    profileId = profileId,

                    amountMinor = amountMinor,

                    category =
                        state.selectedCategory,

                    note = note,

                    date = LocalDate.now(),
                )

            _uiState.value =
                ScanPayUiState()

            when (result) {

                is AddExpenseResult.Success -> {

                    val baseMessage =
                        if (pending) {
                            "Expense logged — payment submitted, verify it completed"
                        } else {
                            "Expense added"
                        }

                    val message =
                        result.newAlerts
                            .firstOrNull()
                            ?.let {
                                "$baseMessage. ${it.toSnackbarMessage()}"
                            }
                            ?: baseMessage

                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            message,
                        ),
                    )
                }

                is AddExpenseResult.Error -> {

                    _effects.send(
                        ScanPayEffect.ShowMessage(
                            "Payment succeeded but couldn't be logged: ${result.message}",
                        ),
                    )
                }
            }
        }
    }
}
