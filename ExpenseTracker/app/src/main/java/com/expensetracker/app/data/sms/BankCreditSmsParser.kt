package com.expensetracker.app.data.sms

import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

data class BankCreditSms(
    val amountMinor: Long,
    val date: LocalDate,
    val source: String,
    val reference: String?,
)

object BankCreditSmsParser {
    private val amountRegexes = listOf(
        Regex("(?i)\\b(?:credited|received|deposited)\\b[^\\n]*?\\b(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)"),
        Regex("(?i)\\b(?:Rs\\.?|INR)\\s*([0-9,]+(?:\\.[0-9]{1,2})?)\\s*(?:has been )?(?:credited|received|deposited)\\b"),
    )
    private val sourceRegexes = listOf(
        Regex("(?im)^\\s*From\\s+(.+?)\\s*$"),
        Regex("(?i)\\b(?:received|credited)\\s+(?:from|by)\\s+([^\\n]+)"),
    )
    private val dateRegex = Regex("(?im)^\\s*On\\s+(\\d{1,2}/\\d{1,2}/\\d{2,4})\\s*$")
    private val referenceRegex = Regex("(?im)^\\s*(?:Ref|Ref No|Reference)\\s*[:#]?\\s*([A-Za-z0-9-]+)\\s*$")
    private val dateFormats = listOf(
        DateTimeFormatter.ofPattern("d/M/yy", Locale.US),
        DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US),
    )

    fun parse(message: String): BankCreditSms? {
        val normalized = message.replace("\\r", "").trim()
        if (Regex("(?i)\\b(?:debited|debit|sent|withdrawn)\\b").containsMatchIn(normalized)) return null
        if (!Regex("(?i)\\b(?:credited|received|deposited)\\b").containsMatchIn(normalized)) return null

        val amountText = amountRegexes.firstNotNullOfOrNull { regex ->
            regex.find(normalized)?.groupValues?.getOrNull(1)
        } ?: return null
        val amountMinor = runCatching {
            BigDecimal(amountText.replace(",", ""))
                .movePointRight(2)
                .setScale(0)
                .longValueExact()
        }.getOrNull()?.takeIf { it > 0L } ?: return null

        val source = sourceRegexes.firstNotNullOfOrNull { regex ->
            regex.find(normalized)?.groupValues?.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        } ?: "Bank credit"
        val date = dateRegex.find(normalized)?.groupValues?.getOrNull(1)?.let(::parseDate) ?: LocalDate.now()
        val reference = referenceRegex.find(normalized)?.groupValues?.getOrNull(1)?.trim()

        return BankCreditSms(amountMinor, date, source, reference)
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
