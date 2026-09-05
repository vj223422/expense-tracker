package com.expensetracker.app.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * All money in the app is a [Long] in minor units (e.g. cents) — see data/entity — so sums are
 * exact integer arithmetic and never accumulate floating-point rounding error.
 */
// Fixed to India rather than Locale.getDefault() — a personal expense tracker's currency
// shouldn't drift with the device's region setting; this also gives ₹ with lakh/crore grouping.
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

fun Long.formatAsCurrency(): String = currencyFormat.format(this / 100.0)

fun String.parseAmountToMinorUnits(): Long? {
    val normalized = trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return Math.round(value * 100.0)
}
