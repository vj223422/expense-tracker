package com.expensetracker.app.core.util

import android.app.Activity
import android.net.Uri

/** Result returned by an external UPI application. */
sealed interface UpiPaymentOutcome {
    data class Success(
        val amountMinor: Long?,
        val txnId: String?,
        val txnRef: String?,
        val responseCode: String?,
        val approvalRefNo: String?,
        val rawFields: Map<String, String>,
    ) : UpiPaymentOutcome

    data class Submitted(
        val amountMinor: Long?,
        val txnId: String?,
        val txnRef: String?,
        val responseCode: String?,
        val rawFields: Map<String, String>,
    ) : UpiPaymentOutcome

    data class Failed(
        val reason: String?,
        val responseCode: String?,
        val rawFields: Map<String, String>,
    ) : UpiPaymentOutcome

    data object Cancelled : UpiPaymentOutcome
}

/**
 * Parses the `response` extra returned by UPI applications.
 *
 * Field names are normalized case-insensitively. UPI applications may omit fields,
 * so transaction data and amount are nullable.
 */
fun parseUpiResponse(
    resultCode: Int,
    responseExtra: String?,
): UpiPaymentOutcome {
    if (resultCode == Activity.RESULT_CANCELED && responseExtra.isNullOrBlank()) {
        return UpiPaymentOutcome.Cancelled
    }

    if (responseExtra.isNullOrBlank()) {
        return UpiPaymentOutcome.Failed(
            reason = "No UPI response was returned",
            responseCode = null,
            rawFields = emptyMap(),
        )
    }

    val fields = responseExtra
        .split('&')
        .mapNotNull { field ->
            field.split('=', limit = 2)
                .takeIf { it.size == 2 }
                ?.let { (key, value) ->
                    key.trim().lowercase() to Uri.decode(value.trim())
                }
        }
        .toMap()

    val status = fields["status"]?.trim()?.uppercase()
    val amountText = listOf("amount", "txnamount", "amt", "am")
        .firstNotNullOfOrNull { key -> fields[key]?.takeIf(String::isNotBlank) }
    val amountMinor = amountText?.parseAmountToMinorUnits()

    val txnId = fields["txnid"]?.takeIf(String::isNotBlank)
    val txnRef = fields["txnref"]?.takeIf(String::isNotBlank)
    val responseCode = fields["responsecode"]?.takeIf(String::isNotBlank)
    val approvalRefNo = fields["approvalrefno"]?.takeIf(String::isNotBlank)

    return when (status) {
        "SUCCESS" -> UpiPaymentOutcome.Success(
            amountMinor = amountMinor,
            txnId = txnId,
            txnRef = txnRef,
            responseCode = responseCode,
            approvalRefNo = approvalRefNo,
            rawFields = fields,
        )
        "SUBMITTED", "PENDING" -> UpiPaymentOutcome.Submitted(
            amountMinor = amountMinor,
            txnId = txnId,
            txnRef = txnRef,
            responseCode = responseCode,
            rawFields = fields,
        )
        else -> UpiPaymentOutcome.Failed(
            reason = fields["error"] ?: fields["errordesc"] ?: status,
            responseCode = responseCode,
            rawFields = fields,
        )
    }
}
