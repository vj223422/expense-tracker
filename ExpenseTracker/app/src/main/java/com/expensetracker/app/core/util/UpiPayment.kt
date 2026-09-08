package com.expensetracker.app.core.util

import android.app.Activity
import android.net.Uri
import java.util.Locale

/** A UPI payee parsed out of a scanned `upi://pay?...` QR code. */
data class UpiPayee(
    val vpa: String,
    val payeeName: String?,
    val suggestedAmount: String?,
    /**
     * Every other recognized query parameter from the scanned QR (e.g. `mc` merchant category
     * code, `tid` terminal id, `mode`, `purpose`, a `sign` signature, and the original `tr`).
     */
    val extraParams: Map<String, String> = emptyMap(),
)

private val HANDLED_UPI_PARAMS = setOf("pa", "pn", "am", "cu", "tn")

/** The rest of the NPCI UPI Linking Specification's known fields. `"tr"` is included here
 * so it is not dropped, allowing us to preserve merchant-provided transaction references 
 * for dynamic QRs. */
private val KNOWN_UPI_EXTRA_PARAMS = setOf("mc", "tid", "url", "mode", "purpose", "orgid", "sign", "refurl", "minamount", "tr")

/** Returns null if [rawValue] isn't a UPI payment link or has no payee address. */
fun parseUpiQr(rawValue: String): UpiPayee? {
    val uri = runCatching { Uri.parse(rawValue) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() != "upi" || uri.host?.lowercase() != "pay") return null
    val vpa = uri.getQueryParameter("pa")?.takeIf { it.isNotBlank() } ?: return null
    
    val extraParams = uri.queryParameterNames
        .filter { it.lowercase() !in HANDLED_UPI_PARAMS && it.lowercase() in KNOWN_UPI_EXTRA_PARAMS }
        .associate { key -> key.lowercase() to uri.getQueryParameter(key).orEmpty() }
        
    return UpiPayee(
        vpa = vpa,
        payeeName = uri.getQueryParameter("pn")?.takeIf { it.isNotBlank() },
        suggestedAmount = uri.getQueryParameter("am")?.takeIf { it.isNotBlank() },
        extraParams = extraParams,
    )
}

/** Builds the `upi://pay` deep link that hands [amountMinor] (paise) and [note] off to an installed UPI app. */
fun buildUpiPaymentUri(payee: UpiPayee, amountMinor: Long, note: String): Uri {
    val amount = String.format(Locale.US, "%.2f", amountMinor / 100.0)
    val builder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", payee.vpa)
        
    // Fix 1: Ensure 'pn' never contains an '@' symbol if the name is missing, 
    // as banks like Axis will reject the format.
    val safePayeeName = payee.payeeName?.takeIf { it.isNotBlank() } ?: payee.vpa.substringBefore("@")
    builder.appendQueryParameter("pn", safePayeeName)

    // Fix 2: Carry through all extra params (including 'tr' if the dynamic QR had it).
    // CRITICAL: We NO LONGER mint a random UUID for 'tr'. If we pass an unauthorized fake 'tr', 
    // the bank switch rejects it for security. If it's missing, GPay will securely mint a valid one.
    payee.extraParams.forEach { (key, value) -> 
        if (value.isNotBlank()) {
            builder.appendQueryParameter(key, value)
        }
    }

    builder.appendQueryParameter("am", amount)
    builder.appendQueryParameter("cu", "INR")
    
    // Fix 3: Never append an empty 'tn' parameter. 
    // '&tn=' triggers a format rejection on Axis and HDFC switches.
    if (note.isNotBlank()) {
        builder.appendQueryParameter("tn", note)
    }

    return builder.build()
}

sealed interface UpiPaymentOutcome {
    data class Success(val txnRef: String?) : UpiPaymentOutcome
    data class Submitted(val txnRef: String?) : UpiPaymentOutcome
    data class Failed(val reason: String?) : UpiPaymentOutcome
    data object Cancelled : UpiPaymentOutcome
}

/**
 * UPI apps report their outcome via a `response` extra shaped like
 * `"Status=SUCCESS&txnId=..&txnRef=.."`
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