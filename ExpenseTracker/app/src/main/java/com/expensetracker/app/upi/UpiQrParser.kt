package com.expensetracker.app.upi

import android.net.Uri

/** Parses a scanned UPI QR payload into the parts needed by the UPI handoff. */
data class UpiQrPayload(
    val vpa: String,
    val payeeName: String?,
    val note: String?,
)

object UpiQrParser {
    fun parse(raw: String): UpiQrPayload? {
        if (!raw.startsWith("upi://pay", ignoreCase = true)) return null

        val uri = runCatching { Uri.parse(raw) }.getOrNull() ?: return null
        val vpa = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (vpa.isBlank()) return null

        return UpiQrPayload(
            vpa = vpa,
            payeeName = uri.getQueryParameter("pn")?.trim()?.ifBlank { null },
            note = uri.getQueryParameter("tn")?.trim()?.ifBlank { null },
        )
    }
}
