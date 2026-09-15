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
    private val amountRegex = Regex("(?i)\\bSent\\s*(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)")
    private val merchantRegex = Regex("(?im)^\\s*To\\s+(.+?)\\s*$")
    private val inlineMerchantRegex = Regex("(?i)\\bto\\s+(.+?)\\s*\\.\\s*(?:RRN|Ref(?:erence)?)\\b")
    private val dateRegex = Regex("(?im)^\\s*On\\s*[:\\-]?\\s*(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\s*$")
    private val inlineDateRegex = Regex("(?i)\\bon\\s+(\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4})\\b")
    private val referenceRegex = Regex("(?im)^\\s*Ref(?:erence)?(?:\\s+No)?\\s*[:#-]?\\s*([A-Za-z0-9-]+)\\s*$")
    private val inlineReferenceRegex = Regex("(?i)\\bRRN\\s*[:#-]?\\s*([A-Za-z0-9-]+)")
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

        if (!Regex("(?i)\\bSent\\s*(?:Rs\\.?|INR)\\s*").containsMatchIn(normalized)) return null

        val isLineBasedFormat = Regex("(?im)^\\s*To\\s+.+$").containsMatchIn(normalized) &&
            Regex("(?im)^\\s*On\\s*[:\\-]?\\s*\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\s*$").containsMatchIn(normalized)
        val isInlineFormat = inlineMerchantRegex.containsMatchIn(normalized) &&
            inlineDateRegex.containsMatchIn(normalized) &&
            inlineReferenceRegex.containsMatchIn(normalized)
        if (!isLineBasedFormat && !isInlineFormat) return null

        val amountText = amountRegex.find(normalized)?.groupValues?.getOrNull(1) ?: return null
        val amountMinor = runCatching {
            BigDecimal(amountText.replace(",", ""))
                .movePointRight(2)
                .setScale(0)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L } ?: return null

        val merchant = merchantRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: inlineMerchantRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()
                ?.takeIf { it.isNotBlank() }
            ?: return null

        val dateText = dateRegex.find(normalized)?.groupValues?.getOrNull(1)
            ?: inlineDateRegex.find(normalized)?.groupValues?.getOrNull(1)
        val date = dateText?.let(::parseDate) ?: return null

        val reference = referenceRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()
            ?: inlineReferenceRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()

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
