package com.expensetracker.app.core.util

import android.app.Activity
import android.net.Uri
import java.util.Locale
import java.util.UUID

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
    
    // Keys normalized to lowercase so exact-match lookups are reliable regardless of 
    // how a given QR happens to capitalize its own parameter names.
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
    // UPI requires a '.'-decimal amount regardless of device locale.
    val amount = String.format(Locale.US, "%.2f", amountMinor / 100.0)
    val builder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", payee.vpa)
        .appendQueryParameter("pn", payee.payeeName ?: payee.vpa)

    // Carry through merchant-specific fields the original QR had, filtering out 'tr' 
    // since we handle it explicitly below to ensure it is only added once.
    payee.extraParams.forEach { (key, value) -> 
        if (key != "tr") {
            builder.appendQueryParameter(key, value) 
        }
    }

    // Preserve existing 'tr' for Dynamic QRs (so merchant backend can track the order)
    // or mint a fresh fallback UUID for Static QRs (to prevent duplicate/limit errors).
    val existingTr = payee.extraParams["tr"]
    val trToUse = if (!existingTr.isNullOrBlank()) {
        existingTr
    } else {
        UUID.randomUUID().toString().replace("-", "")
    }
    builder.appendQueryParameter("tr", trToUse)
    
    return builder
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