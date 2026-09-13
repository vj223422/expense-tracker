package com.expensetracker.app.upi

import android.app.Activity
import android.content.Intent
import android.net.Uri
import java.util.Locale

object UpiLauncher {
    const val REQUEST_CODE = 7001
    const val EXTRA_UPI_RESPONSE = "response"

    fun buildIntent(
        payeeVpa: String,
        amountMinor: Long,
        payeeName: String? = null,
        note: String? = null,
        originalUri: Uri? = null,
    ): Intent {
        require(payeeVpa.isNotBlank()) { "Payee VPA is required" }
        require(amountMinor > 0L) { "Amount must be greater than zero" }

        val amount = amountMinor / 100.0
        val amountText = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            "%.2f".format(Locale.US, amount).trimEnd('0').trimEnd('.')
        }

        val builder = (originalUri ?: Uri.parse("upi://pay")).buildUpon()
            .clearQuery()
            .appendQueryParameter("pa", payeeVpa.trim())
            .appendQueryParameter("pn", payeeName?.trim().orEmpty())
            .appendQueryParameter("tn", note?.trim().orEmpty())
            .appendQueryParameter("am", amountText)
            .appendQueryParameter("cu", "INR")

        // Keep all non-standard parameters from a scanned QR (for example mc, tr,
        // url, mode, purpose and orgid) while replacing the editable payment fields.
        originalUri?.queryParameterNames?.forEach { key ->
            if (key !in setOf("pa", "pn", "tn", "am", "cu")) {
                originalUri.getQueryParameters(key).forEach { value ->
                    builder.appendQueryParameter(key, value)
                }
            }
        }

        return Intent(Intent.ACTION_VIEW, builder.build())
    }

    fun launch(activity: Activity, intent: Intent): Boolean {
        val chooser = Intent.createChooser(intent, "Pay with")
        if (chooser.resolveActivity(activity.packageManager) == null) return false
        activity.startActivityForResult(chooser, REQUEST_CODE)
        return true
    }
}
