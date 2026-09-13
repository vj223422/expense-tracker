package com.expensetracker.app.upi

import android.net.Uri

/** Parses a scanned UPI QR payload while retaining the original URI for payment handoff. */
data class UpiQrPayload(
    val vpa: String,
    val payeeName: String?,
    val note: String?,
    val amountMinor: Long?,
    val originalUri: Uri,
)

object UpiQrParser {
    fun parse(raw: String): UpiQrPayload? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("upi://pay", ignoreCase = true)) return null
        val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
        val vpa = uri.getQueryParameter("pa")?.trim().orEmpty()
        if (vpa.isBlank()) return null

        val amountMinor = uri.getQueryParameter("am")?.trim()?.toDoubleOrNull()
            ?.takeIf { it > 0.0 }
            ?.let { kotlin.math.round(it * 100.0).toLong() }

        return UpiQrPayload(
            vpa = vpa,
            payeeName = uri.getQueryParameter("pn")?.trim()?.ifBlank { null },
            note = uri.getQueryParameter("tn")?.trim()?.ifBlank { null },
            amountMinor = amountMinor,
            originalUri = uri,
        )
    }
}
