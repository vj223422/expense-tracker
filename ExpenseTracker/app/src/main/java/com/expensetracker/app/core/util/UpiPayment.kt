package com.expensetracker.app.core.util

import android.app.Activity
import android.net.Uri
import java.util.Locale

/** A UPI payee parsed out of a scanned `upi://pay?...` QR code. */
data class UpiPayee(
    val vpa: String,
    val payeeName: String?,
    val suggestedAmount: String?,
)

/** Returns null if [rawValue] isn't a UPI payment link or has no payee address. */
fun parseUpiQr(rawValue: String): UpiPayee? {
    val uri = runCatching { Uri.parse(rawValue) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() != "upi" || uri.host?.lowercase() != "pay") return null
    val vpa = uri.getQueryParameter("pa")?.takeIf { it.isNotBlank() } ?: return null
    return UpiPayee(
        vpa = vpa,
        payeeName = uri.getQueryParameter("pn")?.takeIf { it.isNotBlank() },
        suggestedAmount = uri.getQueryParameter("am")?.takeIf { it.isNotBlank() },
    )
}

/** Builds the `upi://pay` deep link that hands [amountMinor] (paise) and [note] off to an installed UPI app. */
fun buildUpiPaymentUri(payee: UpiPayee, amountMinor: Long, note: String): Uri {
    // UPI requires a '.'-decimal amount regardless of device locale.
    val amount = String.format(Locale.US, "%.2f", amountMinor / 100.0)
    return Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", payee.vpa)
        .appendQueryParameter("pn", payee.payeeName ?: payee.vpa)
        .appendQueryParameter("am", amount)
        .appendQueryParameter("cu", "INR")
        .appendQueryParameter("tn", note)
        .build()
}

sealed interface UpiPaymentOutcome {
    data class Success(val txnRef: String?) : UpiPaymentOutcome
    data class Submitted(val txnRef: String?) : UpiPaymentOutcome
    data class Failed(val reason: String?) : UpiPaymentOutcome
    data object Cancelled : UpiPaymentOutcome
}

/**
 * UPI apps report their outcome via a `response` extra shaped like
 * `"Status=SUCCESS&txnId=..&txnRef=.."` — an ad hoc key=value&key=value string, not a URI —
 * so this parses that format directly instead of going through [Uri].
 */
fun parseUpiResponse(resultCode: Int, responseExtra: String?): UpiPaymentOutcome {
    if (resultCode == Activity.RESULT_CANCELED && responseExtra.isNullOrBlank()) {
        return UpiPaymentOutcome.Cancelled
    }
    if (responseExtra.isNullOrBlank()) return UpiPaymentOutcome.Failed(reason = null)

    val fields = responseExtra.split('&')
        .mapNotNull { field ->
            val parts = field.split('=', limit = 2)
            if (parts.size == 2) parts[0].trim().lowercase() to parts[1].trim() else null
        }
        .toMap()

    val status = fields["status"]?.uppercase()
    val txnRef = fields["txnref"] ?: fields["txnid"] ?: fields["approvalrefno"]
    return when (status) {
        "SUCCESS" -> UpiPaymentOutcome.Success(txnRef)
        "SUBMITTED" -> UpiPaymentOutcome.Submitted(txnRef)
        else -> UpiPaymentOutcome.Failed(fields["error"] ?: status)
    }
}
