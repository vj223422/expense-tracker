package com.expensetracker.app.core.util

import android.app.Activity
import android.net.Uri
import java.util.Locale

/** A UPI payee parsed out of a scanned `upi://pay?...` QR code. */
data class UpiPayee(
    val vpa: String,
    val payeeName: String?,
    val suggestedAmount: String?,

    /** True when the QR contains transaction-bound fields. */
    val isDynamic: Boolean = false,

    /**
     * Exact URI scanned from the QR.
     *
     * Dynamic/signed QRs must be handed to the UPI app unchanged so that
     * transaction references and signatures remain valid.
     */
    val originalUri: String? = null,

    /** Safe, non-signature parameters retained from static merchant QRs. */
    val extraParams: Map<String, String> = emptyMap(),
)

private val HANDLED_UPI_PARAMS = setOf(
    "pa",
    "pn",
    "am",
    "cu",
    "tn",
)

/*
 * These are the only non-transaction parameters we can safely rebuild.
 *
 * A QR can contain provider-specific parameters that are not documented by
 * the base UPI link format.  Dropping one while rebuilding the URI can be
 * just as harmful as changing a known signature or transaction reference.
 */
private val SAFE_STATIC_UPI_PARAMS =
    HANDLED_UPI_PARAMS + setOf(
        "mc",
        "mode",
        "purpose",
    )

/**
 * Known UPI Linking Specification parameters.
 */
private val KNOWN_UPI_EXTRA_PARAMS = setOf(
    "mc",
    "tid",
    "url",
    "mode",
    "purpose",
    "orgid",
    "sign",
    "refurl",
    "minamount",
    "mam",
    "tr",
    "aid",
)

/**
 * Parameters that indicate the QR is transaction-bound/dynamic.
 *
 * We must NOT reconstruct such a URI because fields such as `tr` and `sign`
 * can be tied to the original merchant-generated request.
 */
private val DYNAMIC_UPI_PARAMS = setOf(
    "tr",
    "tid",
    "url",
    "sign",
    "refurl",
    "orgid",
    "minamount",
    "mam",
)

/** Returns null if [rawValue] isn't a UPI payment link or has no payee address. */
fun parseUpiQr(rawValue: String): UpiPayee? {
    val uri = runCatching {
        Uri.parse(rawValue)
    }.getOrNull() ?: return null

    if (uri.scheme?.lowercase() != "upi") return null
    if (uri.host?.lowercase() != "pay") return null

    val vpa = uri
        .getQueryParameter("pa")
        ?.takeIf { it.isNotBlank() }
        ?: return null

    val parameterNames = uri.queryParameterNames

    val suggestedAmount = uri
        .getQueryParameter("am")
        ?.takeIf { it.isNotBlank() }

    /*
     * Only lock a transaction-bound QR when it supplies an amount.  Merchant
     * QRs with no `am` are free-form payment requests: the payer must be able
     * to provide both the amount and note before launching their UPI app.
     */
    val isDynamic =
        !suggestedAmount.isNullOrBlank() &&
            parameterNames.any {
                it.lowercase() in DYNAMIC_UPI_PARAMS
            }

    /*
     * Keep only harmless parameters for static merchant QR codes.
     *
     * Transaction-bound/signature parameters are intentionally excluded
     * because we never want to reconstruct them with a changed amount.
     */
    val extraParams = parameterNames
        .mapNotNull { key ->
            val normalized = key.lowercase()

            if (normalized in HANDLED_UPI_PARAMS) {
                return@mapNotNull null
            }

            if (normalized !in KNOWN_UPI_EXTRA_PARAMS) {
                return@mapNotNull null
            }

            if (normalized in DYNAMIC_UPI_PARAMS) {
                return@mapNotNull null
            }

            normalized to uri.getQueryParameter(key).orEmpty()
        }
        .toMap()

    return UpiPayee(
        vpa = vpa,
        payeeName = uri
            .getQueryParameter("pn")
            ?.takeIf { it.isNotBlank() },

        suggestedAmount = suggestedAmount,

        isDynamic = isDynamic,

        /*
         * Store the exact original QR string.
         */
        originalUri = rawValue,

        extraParams = extraParams,
    )
}

/**
 * Builds the UPI payment URI.
 *
 * Static/P2P QR:
 * - Allows the user to select the amount.
 * - Allows a note.
 * - Preserves harmless merchant classification parameters.
 *
 * Dynamic/signed merchant QR:
 * - Returns the EXACT URI scanned from the QR.
 * - Does not modify the amount.
 * - Does not modify/remove transaction references.
 * - Does not invalidate merchant signatures.
 */

/**
 * Builds the UPI payment URI.
 *
 * Static/P2P QR:
 * - Allows the user to select the amount.
 * - Allows a note.
 * - Preserves harmless merchant classification parameters.
 *
 * Dynamic/signed merchant QR:
 * - Returns the EXACT URI scanned from the QR.
 * - Does not modify the amount.
 * - Does not modify/remove transaction references.
 * - Does not invalidate merchant signatures.
 */
fun buildUpiPaymentUri(
    payee: UpiPayee,
    amountMinor: Long,
    note: String,
): Uri {

    /*
     * Dynamic/signed QR:
     *
     * Never rebuild this URI.
     *
     * The merchant may have generated a transaction reference,
     * signature, transaction ID, or fixed amount.
     */
    if (payee.isDynamic && !payee.originalUri.isNullOrBlank()) {
        return Uri.parse(payee.originalUri)
    }

    val amount = String.format(
        Locale.US,
        "%.2f",
        amountMinor / 100.0,
    )

    /*
     * If the QR has no payee name, use the VPA prefix.
     *
     * Example:
     * abc@upi -> abc
     */
    val safePayeeName = payee.payeeName
        ?.takeIf { it.isNotBlank() }
        ?: payee.vpa.substringBefore("@")
            .ifBlank { payee.vpa }

    val builder = Uri.Builder()
        .scheme("upi")
        .authority("pay")
        .appendQueryParameter("pa", payee.vpa)
        .appendQueryParameter("pn", safePayeeName)

    /*
     * Preserve harmless merchant information such as MC.
     *
     * Dynamic/signature fields were removed during parsing.
     */
    payee.extraParams.forEach { (key, value) ->
        if (value.isNotBlank()) {
            builder.appendQueryParameter(key, value)
        }
    }

    builder.appendQueryParameter("am", amount)
    builder.appendQueryParameter("cu", "INR")

    /*
     * Don't append an empty tn parameter.
     */
    if (note.isNotBlank()) {
        builder.appendQueryParameter("tn", note)
    }

    // THE FIX: Intercept the built URI and swap the URL-encoded '%40' back to a raw '@'
    val uri = builder.build()
    val finalUriString = uri.toString().replace("%40", "@")
    return Uri.parse(finalUriString)
}

sealed interface UpiPaymentOutcome {

    data class Success(
        val txnRef: String?,
    ) : UpiPaymentOutcome

    data class Submitted(
        val txnRef: String?,
    ) : UpiPaymentOutcome

    data class Failed(
        val reason: String?,
    ) : UpiPaymentOutcome

    data object Cancelled : UpiPaymentOutcome
}

/**
 * UPI apps report their outcome via a `response` extra such as:
 *
 * Status=SUCCESS&txnId=...&txnRef=...
 */
fun parseUpiResponse(
    resultCode: Int,
    responseExtra: String?,
): UpiPaymentOutcome {

    if (
        resultCode == Activity.RESULT_CANCELED &&
        responseExtra.isNullOrBlank()
    ) {
        return UpiPaymentOutcome.Cancelled
    }

    if (responseExtra.isNullOrBlank()) {
        return UpiPaymentOutcome.Failed(
            reason = null,
        )
    }

    val fields = responseExtra
        .split('&')
        .mapNotNull { field ->

            val parts = field.split(
                '=',
                limit = 2,
            )

            if (parts.size == 2) {
                parts[0]
                    .trim()
                    .lowercase() to parts[1].trim()
            } else {
                null
            }
        }
        .toMap()

    val status = fields["status"]?.uppercase()

    val txnRef =
        fields["txnref"]
            ?: fields["txnid"]
            ?: fields["approvalrefno"]

    return when (status) {

        "SUCCESS" -> UpiPaymentOutcome.Success(
            txnRef = txnRef,
        )

        "SUBMITTED" -> UpiPaymentOutcome.Submitted(
            txnRef = txnRef,
        )

        else -> UpiPaymentOutcome.Failed(
            reason = fields["error"] ?: status,
        )
    }
}
