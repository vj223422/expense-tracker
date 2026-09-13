package com.expensetracker.app.upi

import android.app.Activity
import android.content.Intent
import android.net.Uri

object UpiLauncher {
    const val REQUEST_CODE = 7001
    const val EXTRA_UPI_RESPONSE = "response"

    fun buildIntent(
        payeeVpa: String? = null,
        payeeName: String? = null,
        note: String? = null,
    ): Intent {
        val builder = Uri.parse("upi://pay").buildUpon()
        if (!payeeVpa.isNullOrBlank()) builder.appendQueryParameter("pa", payeeVpa)
        if (!payeeName.isNullOrBlank()) builder.appendQueryParameter("pn", payeeName)
        if (!note.isNullOrBlank()) builder.appendQueryParameter("tn", note)
        builder.appendQueryParameter("cu", "INR")

        return Intent(Intent.ACTION_VIEW, builder.build())
    }

    fun launch(activity: Activity, intent: Intent): Boolean {
        val chooser = Intent.createChooser(intent, "Pay with")
        if (chooser.resolveActivity(activity.packageManager) == null) return false
        activity.startActivityForResult(chooser, REQUEST_CODE)
        return true
    }
}
