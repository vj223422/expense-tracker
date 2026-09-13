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
    ): Intent {
        require(payeeVpa.isNotBlank()) { "Payee VPA is required" }
        require(amountMinor > 0L) { "Amount must be greater than zero" }

        val amount = amountMinor / 100.0
        val amountText = if (amount % 1.0 == 0.0) {
            amount.toLong().toString()
        } else {
            "%.2f".format(Locale.US, amount).trimEnd('0').trimEnd('.')
        }

        val builder = Uri.parse("upi://pay").buildUpon()
            .appendQueryParameter("pa", payeeVpa.trim())
            .appendQueryParameter("pn", payeeName?.trim().orEmpty())
            .appendQueryParameter("tn", note?.trim().orEmpty())
            .appendQueryParameter("am", amountText)
            .appendQueryParameter("cu", "INR")

        return Intent(Intent.ACTION_VIEW, builder.build())
    }

    fun launch(activity: Activity, intent: Intent): Boolean {
        val chooser = Intent.createChooser(intent, "Pay with")
        if (chooser.resolveActivity(activity.packageManager) == null) return false
        activity.startActivityForResult(chooser, REQUEST_CODE)
        return true
    }
}
