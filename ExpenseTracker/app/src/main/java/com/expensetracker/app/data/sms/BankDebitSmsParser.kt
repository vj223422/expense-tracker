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
    // Keep these patterns tolerant of bank-specific spacing/punctuation while still requiring
    // the distinctive HDFC-style debit fields. Extra footer text such as "Not You?" is ignored.
    private val amountRegex = Regex("(?i)\\bSent\\s*(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val merchantRegex = Regex("(?im)^\\s*To\\s+(.+?)\\s*$")
    private val dateRegex = Regex("(?im)^\\s*On\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s*$")
    private val referenceRegex = Regex("(?im)^\\s*Ref(?:erence)?(?:\\s+No)?\\s*[:#-]?\\s*([A-Za-z0-9-]+)\\s*$")
    private val fromBankRegex = Regex("(?im)^\\s*From\\s+.+?\\bBank\\s+A/C\\b")
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("d/M/yy", Locale.US),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US),
        DateTimeFormatter.ofPattern("d-M-yy", Locale.US),
        DateTimeFormatter.ofPattern("d-M-yyyy", Locale.US),
    )

    fun parse(message: String): BankDebitSms? {
        val normalized = message
            .replace('\r', '\n')
            .replace(Regex("[ \\t]+"), " ")
            .trim()

        // Do not require the optional "From ... Bank A/C" line: some legitimate bank SMS
        // variants omit or format that line differently. The Sent + To + On combination is the
        // reliable debit signature, and the optional bank line is retained only as a signal.
        if (!Regex("(?i)\\bSent\\s*(?:Rs\\.?|INR)\\s*").containsMatchIn(normalized)) return null
        if (!Regex("(?im)^\\s*To\\s+.+$").containsMatchIn(normalized)) return null
        if (!Regex("(?im)^\\s*On\\s*[:\\-]?\\s*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\s*$").containsMatchIn(normalized)) return null

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
        val date = dateText?.let(::parseDate) ?: return null
        val reference = referenceRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()

        // A debit must contain the expected bank-account wording when it is present. This check
        // is intentionally non-blocking so a bank's harmless formatting change won't lose a real
        // transaction.
        fromBankRegex.containsMatchIn(normalized)

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
