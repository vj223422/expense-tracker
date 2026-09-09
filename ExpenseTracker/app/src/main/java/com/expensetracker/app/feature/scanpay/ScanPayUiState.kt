package com.expensetracker.app.feature.scanpay

import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import com.expensetracker.app.core.util.UpiPayee
import com.expensetracker.app.core.util.parseAmountToMinorUnits

sealed interface ScanPayStage {
    data object Scanning : ScanPayStage
    data class Confirming(val payee: UpiPayee) : ScanPayStage
    data class LaunchingPayment(val payee: UpiPayee) : ScanPayStage
}

@Immutable
data class ScanPayUiState(
    val stage: ScanPayStage = ScanPayStage.Scanning,
    val amountText: String = "",
    val note: String = "",
    val amountError: String? = null,
    /** True only while [amountText] still holds the QR's own `am` value, untouched by the user — see ConfirmPaymentSheet's "verify before paying" hint. */
    val amountPrefilledFromQr: Boolean = false,
) {
    val canPay: Boolean
        get() = stage is ScanPayStage.Confirming && (amountText.parseAmountToMinorUnits() ?: 0L) > 0L
}

sealed interface ScanPayEffect {
    data class LaunchUpiApp(val uri: Uri) : ScanPayEffect
    data class ShowMessage(val message: String) : ScanPayEffect
}

@Stable
interface ScanPayActions {
    fun onQrDetected(rawValue: String)
    fun onQrImageReadFailed()
    fun onAmountChange(value: String)
    fun onNoteChange(value: String)
    fun onPayClick()
    fun onCancelConfirm()
    fun onPaymentActivityResult(resultCode: Int, responseExtra: String?)
}
