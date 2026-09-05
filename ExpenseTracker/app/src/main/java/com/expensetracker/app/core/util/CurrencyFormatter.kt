package com.expensetracker.app.core.util

import java.text.NumberFormat
import java.util.Locale

/**
 * All money in the app is a [Long] in minor units (e.g. cents) — see data/entity — so sums are
 * exact integer arithmetic and never accumulate floating-point rounding error.
 */
private val currencyFormat: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en","IN"))

fun Long.formatAsCurrency(): String = currencyFormat.format(this / 100.0)

fun String.parseAmountToMinorUnits(): Long? {
    val normalized = trim().replace(',', '.')
    if (normalized.isEmpty()) return null
    val value = normalized.toDoubleOrNull() ?: return null
    if (value < 0) return null
    return Math.round(value * 100.0)
}
