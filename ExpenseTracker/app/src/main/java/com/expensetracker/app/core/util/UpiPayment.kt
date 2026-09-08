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
     * code, `tid` terminal id, `mode`, `purpose`, a `sign` signature) preserved as-is — see
     * KNOWN_UPI_EXTRA_PARAMS. Notably excludes `tr`: see buildUpiPaymentUri, which always mints
     * its own fresh transaction reference per attempt rather than trusting the QR's.
     * Many merchant (P2M) VPAs — especially aggregator/PSP-issued ones like `...@paytm`/`...@rzp`
     * used by small-merchant QR stickers — only resolve on the bank/NPCI side *with* this extra
     * context attached; rebuilding the payment link from just pa/pn/am strips it and can fail
     * with "receiver's UPI ID or VPA was unavailable" even though the same QR pays fine when
     * scanned directly in a UPI app that keeps the full original string.
     */
    val extraParams: Map<String, String> = emptyMap(),
)

private val HANDLED_UPI_PARAMS = setOf("pa", "pn", "am", "cu", "tn")

/** The rest of the NPCI UPI Linking Specification's known fields — everything else in a scanned
 * QR's query string is dropped rather than blindly forwarded into the Intent we launch, since a
 * QR code is untrusted external input and there's no reason to carry through a key we don't
 * recognize. `tr` is deliberately excluded here even though it's a real spec field — see
 * buildUpiPaymentUri, which always mints its own fresh one instead of reusing whatever (if
 * anything) the QR happened to contain. */
private val KNOWN_UPI_EXTRA_PARAMS = setOf("mc", "tid", "url", "mode", "purpose", "orgid", "sign", "refurl", "minamount")

/** Returns null if [rawValue] isn't a UPI payment link or has no payee address. */
fun parseUpiQr(rawValue: String): UpiPayee? {
    val uri = runCatching { Uri.parse(rawValue) }.getOrNull() ?: return null
    if (uri.scheme?.lowercase() != "upi" || uri.host?.lowercase() != "pay") return null
    val vpa = uri.getQueryParameter("pa")?.takeIf { it.isNotBlank() } ?: return null
    // Keys normalized to lowercase (the NPCI-spec form every real QR uses) so a later exact-match
    // lookup like `"tr" !in extraParams` in buildUpiPaymentUri is reliable regardless of how a
    // given QR happens to capitalize its own parameter names.
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
    // Carry through whatever merchant-specific fields the original QR had — see extraParams.
    payee.extraParams.forEach { (key, value) -> builder.appendQueryParameter(key, value) }
    // Always a fresh one per attempt, never whatever (if anything) the scanned QR itself
    // contained — tr identifies this specific payment attempt, not the payee, so it's the
    // payer's app's job to mint it, not something to trust from external QR content. A P2M
    // intent with no tr — or worse, one reused across retries of the same QR — is commonly
    // flagged by the receiving PSP app as an invalid/duplicate request, surfacing as a generic
    // "exceeded limit" error regardless of the actual amount. GPay's own scanner generates one
    // invisibly on every scan, which is why the same QR can pay fine there but not through a
    // reconstructed intent that omits it or repeats it.
    builder.appendQueryParameter("tr", UUID.randomUUID().toString().replace("-", ""))
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