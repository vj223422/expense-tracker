package com.expensetracker.app.data.sms

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class BankDebitSms(
    val amountMinor: Long,
    val date: LocalDate,
    val merchant: String,
    val reference: String?,
)

object BankDebitSmsParser {
    private val amountRegex = Regex("(?i)\\bSent\\s+Rs\\.?\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val merchantRegex = Regex("(?im)^\\s*To\\s+(.+?)\\s*$")
    private val dateRegex = Regex("(?im)^\\s*On\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s*$")
    private val referenceRegex = Regex("(?im)^\\s*Ref\\s+([A-Za-z0-9-]+)\\s*$")
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("d/M/yy", Locale.US),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US),
    )

    fun parse(message: String): BankDebitSms? {
        val normalized = message.replace("\\r", "").trim()
        if (!Regex("(?i)\\bSent\\s+Rs\\.?").containsMatchIn(normalized)) return null
        if (!Regex("(?im)^\\s*From\\s+.+\\bBank\\s+A/C\\b").containsMatchIn(normalized)) return null

        val amountText = amountRegex.find(normalized)?.groupValues?.getOrNull(1) ?: return null
        val amountMinor = runCatching {
            BigDecimal(amountText.replace(",", ""))
                .movePointRight(2)
                .setScale(0)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L } ?: return null

        val merchant = merchantRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() } ?: return null
        val dateText = dateRegex.find(normalized)?.groupValues?.getOrNull(1)
        val date = dateText?.let(::parseDate) ?: LocalDate.now()
        val reference = referenceRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()

        return BankDebitSms(
            amountMinor = amountMinor,
            date = date,
            merchant = merchant,
            reference = reference,
        )
    }

    private fun parseDate(value: String): LocalDate =
        dateFormats.firstNotNullOfOrNull { formatter ->
            try {
                LocalDate.parse(value, formatter)
            } catch (_: DateTimeParseException) {
                null
            }
        } ?: LocalDate.now()
}
