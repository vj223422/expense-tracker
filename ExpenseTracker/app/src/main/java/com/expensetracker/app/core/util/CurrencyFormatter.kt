package com.expensetracker.app.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * All money in the app is a [Long] in minor units (e.g. cents) — see data/entity — so sums are
 * exact integer arithmetic and never accumulate floating-point rounding error.
 */
// Fixed to India rather than Locale.getDefault() — a personal expense tracker's currency
// shouldn't drift with the device's region setting; this also gives ₹ with lakh/crore grouping.
// A fresh NumberFormat per call, not a shared instance — java.text.Format is documented as not
// thread-safe, and this is called from both Compose (main thread) and the notification path
// (Dispatchers.IO), sometimes for the same alert.
private fun currencyFormat(): NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

fun Long.formatAsCurrency(): String = currencyFormat().format(this / 100.0)

/** Plain "1234.50" — no currency symbol/grouping — for prefilling an editable amount field;
 * round-trips through [parseAmountToMinorUnits] (which also expects a bare '.'-decimal string). */
fun Long.toAmountInputText(): String = String.format(Locale.US, "%.2f", this / 100.0)

/** Comfortably above any real personal expense, but far enough under Long.MAX_VALUE / 100 that
 * summing many expenses together can never overflow. */
private val MAX_AMOUNT_MAJOR_UNITS = BigDecimal(1_000_000_000)

fun String.parseAmountToMinorUnits(): Long? {
    // en-IN (this app's fixed locale, see currencyFormat above) groups digits with commas — e.g.
    // "12,34,567.89" — and always uses '.' as the decimal point, matching formatAsCurrency's own
    // output. So commas are thousands/lakh separators to strip, never a decimal point.
    val normalized = trim().replace(",", "")
    if (normalized.isEmpty()) return null
    // BigDecimal, not Double: a plain binary-float parse of e.g. "1.005" rounds to
    // 1.00499999999999989..., so multiplying by 100 and rounding gives 100 (₹1.00) instead of the
    // correct 101 (₹1.01). BigDecimal parses the decimal text exactly, so this rounds correctly.
    val value = try {
        BigDecimal(normalized)
    } catch (e: NumberFormatException) {
        return null
    }
    if (value.signum() < 0 || value > MAX_AMOUNT_MAJOR_UNITS) return null
    return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact()
}
